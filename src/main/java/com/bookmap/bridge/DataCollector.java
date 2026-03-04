package com.bookmap.bridge;

import velox.api.layer1.data.TradeInfo;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Collects live market data from Bookmap data streams (Core API).
 * 
 * Tracks:
 * - Trade events (volume dots)
 * - Order book depth (absorption proxy)
 * - Best bid/ask (for VWAP proximity)
 * 
 * Thread-Safety: Uses concurrent collections for scheduler access.
 */
public class DataCollector {

    private static final Logger log = Logger.getLogger(DataCollector.class.getName());
    private static final int MAX_TRADES = 200;
    private static final int MAX_ABSORPTIONS = 50;

    private final HttpForwarder forwarder;
    private final String instrumentAlias;

    // Rolling trade buffer
    private final Deque<TradeEvent> recentTrades = new ConcurrentLinkedDeque<>();

    // Absorption events (bid/ask size spike relative to traded volume)
    private final Deque<AbsorptionEvent> recentAbsorptions = new ConcurrentLinkedDeque<>();

    // Current market state
    private final AtomicReference<Double> lastPrice = new AtomicReference<>(0.0);
    private final AtomicReference<Double> bestBid = new AtomicReference<>(0.0);
    private final AtomicReference<Double> bestAsk = new AtomicReference<>(0.0);

    // Cumulative volume delta (session)
    private volatile long cumulativeBuyVolume = 0;
    private volatile long cumulativeSellVolume = 0;

    // Orderbook snapshot for absorption detection
    private final TreeMap<Double, Integer> bidLevels = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Double, Integer> askLevels = new TreeMap<>();

    // Liquidity zone tracker
    private final LiquidityZoneTracker liquidityTracker = new LiquidityZoneTracker();

    public DataCollector(HttpForwarder forwarder, String instrumentAlias) {
        this.forwarder = forwarder;
        this.instrumentAlias = instrumentAlias;
    }

    // ─── Trade Events (Volume Dots) ──────────────────────────────────────────

    /**
     * Core API signature: includes instrument alias.
     * Called by BookmapBridge in Bookmap event thread.
     */
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        lastPrice.set(price);

        // Determine trade direction: TradeInfo.isBidAggressor
        // If bid aggressor = false, buyer was aggressive (buy)
        boolean isBuy = !tradeInfo.isBidAggressor;

        if (isBuy) {
            cumulativeBuyVolume += size;
        } else {
            cumulativeSellVolume += size;
        }

        TradeEvent event = new TradeEvent(
            System.currentTimeMillis(),
            price,
            size,
            isBuy
        );

        recentTrades.addLast(event);
        if (recentTrades.size() > MAX_TRADES) recentTrades.pollFirst();

        // Check absorption: large size at a level that doesn't move price
        checkAbsorption(price, size, isBuy);
        liquidityTracker.recordTrade(price, size, isBuy);
    }

    // ─── Order Book Updates ───────────────────────────────────────────────────

    /**
     * Core API signature: includes instrument alias.
     * Called by BookmapBridge in Bookmap event thread.
     */
    public void onDepth(String alias, boolean isBid, int price, int size) {
        // Convert price level (int) to double
        double priceDouble = (double) price;

        if (isBid) {
            if (size == 0) bidLevels.remove(priceDouble);
            else bidLevels.put(priceDouble, size);

            // Update best bid
            if (!bidLevels.isEmpty()) bestBid.set(bidLevels.firstKey());
        } else {
            if (size == 0) askLevels.remove(priceDouble);
            else askLevels.put(priceDouble, size);

            // Update best ask
            if (!askLevels.isEmpty()) bestAsk.set(askLevels.firstKey());
        }
    }

    // ─── Absorption Detection ─────────────────────────────────────────────────

    private void checkAbsorption(double price, int tradedSize, boolean isBuyAggressor) {
        // Absorption = large order sitting at level, absorbing aggressor flow
        // Proxy: if bid level has huge size and sell aggression hits it without moving it
        int levelSize = 0;
        if (!isBuyAggressor && bidLevels.containsKey(price)) {
            levelSize = bidLevels.get(price);
        } else if (isBuyAggressor && askLevels.containsKey(price)) {
            levelSize = askLevels.get(price);
        }

        if (levelSize > 0 && tradedSize > 0) {
            double ratio = (double) levelSize / (double) tradedSize;
            if (ratio >= 0.65) { // Savepoint threshold
                AbsorptionEvent abs = new AbsorptionEvent(
                    System.currentTimeMillis(),
                    price,
                    tradedSize,
                    levelSize,
                    ratio,
                    !isBuyAggressor // absorbed on bid side = bullish absorption
                );
                recentAbsorptions.addLast(abs);
                if (recentAbsorptions.size() > MAX_ABSORPTIONS) recentAbsorptions.pollFirst();

                log.fine(String.format("[Absorption] price=%.2f ratio=%.2f bullish=%b",
                    price, ratio, abs.bullish));
            }
        }
    }

    // ─── Snapshot Push (called by scheduler) ─────────────────────────────────

    public void pushSnapshot() {
        try {
            MarketSnapshot snapshot = buildSnapshot();
            forwarder.pushSnapshot(snapshot);
        } catch (Exception e) {
            log.warning("[DataCollector] Snapshot push failed: " + e.getMessage());
        }
    }

    private MarketSnapshot buildSnapshot() {
        return new MarketSnapshot(
            instrumentAlias,
            System.currentTimeMillis(),
            lastPrice.get(),
            bestBid.get(),
            bestAsk.get(),
            new ArrayList<>(recentTrades),
            new ArrayList<>(recentAbsorptions),
            cumulativeBuyVolume,
            cumulativeSellVolume,
            liquidityTracker.getTopZones(5)
        );
    }

    // ─── Inner Data Classes ───────────────────────────────────────────────────

    public static class TradeEvent {
        public final long timestamp;
        public final double price;
        public final int size;
        public final boolean isBuy;

        public TradeEvent(long timestamp, double price, int size, boolean isBuy) {
            this.timestamp = timestamp;
            this.price = price;
            this.size = size;
            this.isBuy = isBuy;
        }
    }

    public static class AbsorptionEvent {
        public final long timestamp;
        public final double price;
        public final int aggressorSize;
        public final int restingSize;
        public final double ratio;
        public final boolean bullish;

        public AbsorptionEvent(long timestamp, double price, int aggressorSize,
                               int restingSize, double ratio, boolean bullish) {
            this.timestamp = timestamp;
            this.price = price;
            this.aggressorSize = aggressorSize;
            this.restingSize = restingSize;
            this.ratio = ratio;
            this.bullish = bullish;
        }
    }
}

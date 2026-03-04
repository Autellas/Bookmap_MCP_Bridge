package com.bookmap.bridge;

import com.google.gson.Gson;

import java.util.List;

/**
 * Snapshot of current market state sent to MCP Server.
 * Serialized as JSON.
 */
public class MarketSnapshot {

    public final String alias;
    public final long timestamp;
    public final double lastPrice;
    public final double bestBid;
    public final double bestAsk;
    public final List<DataCollector.TradeEvent> recentTrades;
    public final List<DataCollector.AbsorptionEvent> recentAbsorptions;
    public final long cumulativeBuyVolume;
    public final long cumulativeSellVolume;
    public final List<LiquidityZoneTracker.LiquidityZone> liquidityZones;

    // Derived fields
    public final double cvd;
    public final double spread;

    public MarketSnapshot(
            String alias,
            long timestamp,
            double lastPrice,
            double bestBid,
            double bestAsk,
            List<DataCollector.TradeEvent> recentTrades,
            List<DataCollector.AbsorptionEvent> recentAbsorptions,
            long cumulativeBuyVolume,
            long cumulativeSellVolume,
            List<LiquidityZoneTracker.LiquidityZone> liquidityZones) {

        this.alias = alias;
        this.timestamp = timestamp;
        this.lastPrice = lastPrice;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.recentTrades = recentTrades;
        this.recentAbsorptions = recentAbsorptions;
        this.cumulativeBuyVolume = cumulativeBuyVolume;
        this.cumulativeSellVolume = cumulativeSellVolume;
        this.liquidityZones = liquidityZones;

        // Derived
        this.cvd = cumulativeBuyVolume - cumulativeSellVolume;
        this.spread = bestAsk - bestBid;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

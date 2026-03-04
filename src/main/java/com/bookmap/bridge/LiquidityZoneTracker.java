package com.bookmap.bridge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks liquidity zones based on volume clusters.
 * A zone forms where repeated large trades occur at the same price level.
 */
public class LiquidityZoneTracker {

    private static final double ZONE_TOLERANCE = 2.0;   // ticks grouping threshold
    private static final int    MIN_TOUCHES    = 2;      // min hits to qualify as zone
    private static final int    MAX_ZONES      = 20;

    // price → accumulated volume at that level
    private final Map<Double, ZoneData> zones = new TreeMap<>();

    public void recordTrade(double price, int size, boolean isBuy) {
        // Round to nearest tick group
        double roundedPrice = Math.round(price / ZONE_TOLERANCE) * ZONE_TOLERANCE;

        zones.computeIfAbsent(roundedPrice, p -> new ZoneData(p))
             .addTrade(size, isBuy);
    }

    public List<LiquidityZone> getTopZones(int limit) {
        return zones.values().stream()
            .filter(z -> z.touches >= MIN_TOUCHES)
            .sorted(Comparator.comparingLong(z -> -z.totalVolume))
            .limit(limit)
            .map(z -> new LiquidityZone(
                z.price,
                z.totalVolume,
                z.buyVolume,
                z.sellVolume,
                z.touches,
                z.buyVolume > z.sellVolume ? "SUPPORT" : "RESISTANCE"
            ))
            .collect(Collectors.toList());
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    private static class ZoneData {
        double price;
        long totalVolume = 0;
        long buyVolume = 0;
        long sellVolume = 0;
        int touches = 0;

        ZoneData(double price) {
            this.price = price;
        }

        void addTrade(int size, boolean isBuy) {
            totalVolume += size;
            touches++;
            if (isBuy) buyVolume += size;
            else sellVolume += size;
        }
    }

    public static class LiquidityZone {
        public final double price;
        public final long totalVolume;
        public final long buyVolume;
        public final long sellVolume;
        public final int touches;
        public final String type; // "SUPPORT" or "RESISTANCE"

        public LiquidityZone(double price, long totalVolume, long buyVolume,
                             long sellVolume, int touches, String type) {
            this.price = price;
            this.totalVolume = totalVolume;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
            this.touches = touches;
            this.type = type;
        }
    }
}

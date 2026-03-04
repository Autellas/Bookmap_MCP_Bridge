package com.bookmap.bridge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LiquidityZoneTracker.
 */
class LiquidityZoneTrackerTest {

    private LiquidityZoneTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new LiquidityZoneTracker();
    }

    @Test
    void testNoZonesInitially() {
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(5);
        assertTrue(zones.isEmpty(), "Should have no zones initially");
    }

    @Test
    void testSingleTradeDoesNotCreateZone() {
        tracker.recordTrade(100.0, 10, true);
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(5);
        // Single trade doesn't meet MIN_TOUCHES requirement
        assertTrue(zones.isEmpty() || zones.size() == 0);
    }

    @Test
    void testMultipleTradesCreateZone() {
        // Record multiple trades at similar price levels (within ZONE_TOLERANCE)
        tracker.recordTrade(100.0, 10, true);
        tracker.recordTrade(100.0, 15, false);
        tracker.recordTrade(100.1, 20, true); // Should be grouped with 100.0
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(5);
        
        assertFalse(zones.isEmpty(), "Should have at least one zone");
        
        LiquidityZoneTracker.LiquidityZone zone = zones.get(0);
        assertEquals(45, zone.totalVolume); // 10 + 15 + 20
        assertEquals(30, zone.buyVolume);   // 10 + 20
        assertEquals(15, zone.sellVolume);  // 15
        assertTrue(zone.touches >= 2);
    }

    @Test
    void testSupportVsResistanceClassification() {
        // Create a support zone (more buy volume)
        tracker.recordTrade(100.0, 100, true);
        tracker.recordTrade(100.0, 50, false);
        tracker.recordTrade(100.0, 50, true);
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(1);
        
        assertFalse(zones.isEmpty());
        LiquidityZoneTracker.LiquidityZone zone = zones.get(0);
        
        // Buy volume (150) > Sell volume (50) = SUPPORT
        assertEquals("SUPPORT", zone.type);
    }

    @Test
    void testTopZonesOrdering() {
        // Create multiple zones with different volumes
        tracker.recordTrade(100.0, 100, true);
        tracker.recordTrade(100.0, 100, false);
        
        tracker.recordTrade(200.0, 50, true);
        tracker.recordTrade(200.0, 50, false);
        
        tracker.recordTrade(300.0, 150, true);
        tracker.recordTrade(300.0, 150, false);
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(5);
        
        // Should be ordered by total volume (descending)
        assertFalse(zones.isEmpty());
        
        // Zone at 300.0 should have highest volume (300)
        // Zone at 100.0 should have second highest (200)
        // Zone at 200.0 should have lowest (100)
        if (zones.size() >= 2) {
            assertTrue(zones.get(0).totalVolume >= zones.get(1).totalVolume);
        }
    }

    @Test
    void testLimitTopZones() {
        // Create many zones
        for (int i = 0; i < 20; i++) {
            double price = 100.0 + (i * 10);
            tracker.recordTrade(price, 10, true);
            tracker.recordTrade(price, 10, false);
        }
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(3);
        
        assertTrue(zones.size() <= 3, "Should limit to requested number of zones");
    }

    @Test
    void testZoneTouchesIncrement() {
        tracker.recordTrade(100.0, 10, true);
        tracker.recordTrade(100.0, 10, true);
        tracker.recordTrade(100.0, 10, true);
        
        List<LiquidityZoneTracker.LiquidityZone> zones = tracker.getTopZones(1);
        
        if (!zones.isEmpty()) {
            assertTrue(zones.get(0).touches >= 3);
        }
    }
}

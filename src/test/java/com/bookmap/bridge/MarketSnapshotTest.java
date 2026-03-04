package com.bookmap.bridge;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MarketSnapshot serialization and data integrity.
 */
class MarketSnapshotTest {

    @Test
    void testSnapshotCreation() {
        List<DataCollector.TradeEvent> trades = new ArrayList<>();
        trades.add(new DataCollector.TradeEvent(1000L, 100.0, 10, true));
        
        List<DataCollector.AbsorptionEvent> absorptions = new ArrayList<>();
        List<LiquidityZoneTracker.LiquidityZone> zones = new ArrayList<>();

        MarketSnapshot snapshot = new MarketSnapshot(
            "BTCUSD",
            System.currentTimeMillis(),
            100.0,
            99.5,
            100.5,
            trades,
            absorptions,
            1000L,
            800L,
            zones
        );

        assertEquals("BTCUSD", snapshot.alias);
        assertEquals(100.0, snapshot.lastPrice);
        assertEquals(99.5, snapshot.bestBid);
        assertEquals(100.5, snapshot.bestAsk);
        assertEquals(200L, snapshot.cvd); // 1000 - 800
        assertEquals(1.0, snapshot.spread, 0.001); // 100.5 - 99.5
    }

    @Test
    void testSnapshotJsonSerialization() {
        List<DataCollector.TradeEvent> trades = new ArrayList<>();
        List<DataCollector.AbsorptionEvent> absorptions = new ArrayList<>();
        List<LiquidityZoneTracker.LiquidityZone> zones = new ArrayList<>();

        MarketSnapshot snapshot = new MarketSnapshot(
            "ETHUSD",
            1234567890L,
            2000.0,
            1999.0,
            2001.0,
            trades,
            absorptions,
            5000L,
            3000L,
            zones
        );

        String json = snapshot.toJson();
        
        assertNotNull(json);
        assertTrue(json.contains("ETHUSD"));
        assertTrue(json.contains("2000.0"));
        assertTrue(json.contains("\"cvd\":2000"));
    }

    @Test
    void testNegativeCvdCalculation() {
        MarketSnapshot snapshot = new MarketSnapshot(
            "TEST",
            1000L,
            100.0,
            99.0,
            101.0,
            new ArrayList<>(),
            new ArrayList<>(),
            300L,  // buy volume
            500L,  // sell volume
            new ArrayList<>()
        );

        assertEquals(-200L, snapshot.cvd); // 300 - 500 = -200
    }

    @Test
    void testZeroSpreadHandling() {
        MarketSnapshot snapshot = new MarketSnapshot(
            "TEST",
            1000L,
            100.0,
            100.0,
            100.0,
            new ArrayList<>(),
            new ArrayList<>(),
            100L,
            100L,
            new ArrayList<>()
        );

        assertEquals(0.0, snapshot.spread, 0.001);
    }
}

package com.bookmap.bridge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpForwarder.
 * Note: Most tests are disabled by default as they require a running MCP server.
 * Enable them for integration testing.
 */
class HttpForwarderTest {

    private HttpForwarder forwarder;
    private static final String TEST_URL = "http://localhost:8765";

    @BeforeEach
    void setUp() {
        forwarder = new HttpForwarder(TEST_URL);
    }

    @Test
    void testForwarderCreation() {
        assertNotNull(forwarder);
    }

    @Test
    void testPushSnapshotWithInvalidUrl() {
        HttpForwarder badForwarder = new HttpForwarder("http://invalid-host-that-does-not-exist:9999");
        
        MarketSnapshot snapshot = new MarketSnapshot(
            "BTCUSD",
            System.currentTimeMillis(),
            100.0,
            99.5,
            100.5,
            new ArrayList<>(),
            new ArrayList<>(),
            1000L,
            800L,
            new ArrayList<>()
        );

        // Should not throw exception even if connection fails
        assertDoesNotThrow(() -> badForwarder.pushSnapshot(snapshot));
    }

    @Test
    void testPushSnapshotWithNullData() {
        MarketSnapshot snapshot = new MarketSnapshot(
            "TEST",
            System.currentTimeMillis(),
            0.0,
            0.0,
            0.0,
            new ArrayList<>(),
            new ArrayList<>(),
            0L,
            0L,
            new ArrayList<>()
        );

        // Should handle snapshot with all zero/empty values
        assertDoesNotThrow(() -> forwarder.pushSnapshot(snapshot));
    }

    @Test
    void testJsonSerializationBeforePost() {
        MarketSnapshot snapshot = new MarketSnapshot(
            "ETHUSD",
            System.currentTimeMillis(),
            2000.0,
            1999.0,
            2001.0,
            new ArrayList<>(),
            new ArrayList<>(),
            5000L,
            3000L,
            new ArrayList<>()
        );

        String json = snapshot.toJson();
        assertNotNull(json);
        assertTrue(json.length() > 0);
        
        // Verify it's valid JSON by checking basic structure
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    @Disabled("Requires running MCP server on localhost:8765")
    void testPushSnapshotToRealServer() {
        // This test requires a running MCP server
        MarketSnapshot snapshot = new MarketSnapshot(
            "BTCUSD",
            System.currentTimeMillis(),
            50000.0,
            49950.0,
            50050.0,
            new ArrayList<>(),
            new ArrayList<>(),
            10000L,
            8000L,
            new ArrayList<>()
        );

        assertDoesNotThrow(() -> forwarder.pushSnapshot(snapshot));
    }

    @Test
    void testMultipleSequentialPushes() {
        // Simulate multiple rapid snapshots
        for (int i = 0; i < 5; i++) {
            MarketSnapshot snapshot = new MarketSnapshot(
                "BTCUSD",
                System.currentTimeMillis(),
                100.0 + i,
                99.0 + i,
                101.0 + i,
                new ArrayList<>(),
                new ArrayList<>(),
                1000L + i * 100,
                800L + i * 50,
                new ArrayList<>()
            );

            assertDoesNotThrow(() -> forwarder.pushSnapshot(snapshot));
        }
    }

    @Test
    void testForwarderWithDifferentBaseUrl() {
        HttpForwarder customForwarder = new HttpForwarder("http://custom-host:9999");
        
        MarketSnapshot snapshot = new MarketSnapshot(
            "TEST",
            System.currentTimeMillis(),
            100.0,
            99.0,
            101.0,
            new ArrayList<>(),
            new ArrayList<>(),
            100L,
            100L,
            new ArrayList<>()
        );

        // Should not crash with custom URL
        assertDoesNotThrow(() -> customForwarder.pushSnapshot(snapshot));
    }

    @Test
    void testPushSnapshotWithComplexData() {
        // Create snapshot with actual trade events
        var trades = new ArrayList<DataCollector.TradeEvent>();
        trades.add(new DataCollector.TradeEvent(System.currentTimeMillis(), 100.0, 10, true));
        trades.add(new DataCollector.TradeEvent(System.currentTimeMillis(), 100.5, 15, false));

        var absorptions = new ArrayList<DataCollector.AbsorptionEvent>();
        absorptions.add(new DataCollector.AbsorptionEvent(
            System.currentTimeMillis(), 100.0, 50, 500, 10.0, true
        ));

        var zones = new ArrayList<LiquidityZoneTracker.LiquidityZone>();
        zones.add(new LiquidityZoneTracker.LiquidityZone(
            100.0, 1000L, 600L, 400L, 5, "SUPPORT"
        ));

        MarketSnapshot snapshot = new MarketSnapshot(
            "BTCUSD",
            System.currentTimeMillis(),
            100.25,
            100.0,
            100.5,
            trades,
            absorptions,
            5000L,
            3000L,
            zones
        );

        // Should handle complex snapshot with all data types
        assertDoesNotThrow(() -> forwarder.pushSnapshot(snapshot));
        
        // Verify JSON is created successfully
        String json = snapshot.toJson();
        assertTrue(json.contains("BTCUSD"));
        assertTrue(json.contains("SUPPORT"));
    }
}

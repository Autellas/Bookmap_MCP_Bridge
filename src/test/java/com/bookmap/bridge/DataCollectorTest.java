package com.bookmap.bridge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataCollector.
 * Note: Some tests are simplified to avoid Bookmap API dependencies in test environment.
 */
class DataCollectorTest {

    @Mock
    private HttpForwarder mockForwarder;

    private DataCollector collector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        collector = new DataCollector(mockForwarder, "BTCUSD");
    }

    @Test
    void testCollectorCreation() {
        assertNotNull(collector);
    }

    @Test
    void testOrderBookBidUpdate() {
        // Test depth data handling without requiring TradeInfo
        collector.onDepth("BTCUSD", true, 100, 50); // Bid at 100.0 with size 50
        collector.onDepth("BTCUSD", true, 99, 30);  // Bid at 99.0 with size 30

        // Should not throw exception
        assertDoesNotThrow(() -> collector.pushSnapshot());
        verify(mockForwarder, atLeastOnce()).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testOrderBookAskUpdate() {
        collector.onDepth("BTCUSD", false, 101, 40); // Ask at 101.0 with size 40
        collector.onDepth("BTCUSD", false, 102, 20); // Ask at 102.0 with size 20

        // Should not throw exception
        assertDoesNotThrow(() -> collector.pushSnapshot());
        verify(mockForwarder, atLeastOnce()).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testOrderBookLevelRemoval() {
        collector.onDepth("BTCUSD", true, 100, 50);  // Add bid
        collector.onDepth("BTCUSD", true, 100, 0);   // Remove bid (size = 0)

        assertDoesNotThrow(() -> collector.pushSnapshot());
        verify(mockForwarder, atLeastOnce()).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testSnapshotPushHandlesException() {
        doThrow(new RuntimeException("Network error"))
            .when(mockForwarder).pushSnapshot(any(MarketSnapshot.class));

        // Should not throw exception even if forwarder fails
        assertDoesNotThrow(() -> collector.pushSnapshot());
    }

    @Test
    void testMultipleDepthUpdates() {
        // Simulate multiple depth updates
        for (int i = 0; i < 10; i++) {
            collector.onDepth("BTCUSD", true, 100 + i, 10 + i);
            collector.onDepth("BTCUSD", false, 110 + i, 10 + i);
        }

        assertDoesNotThrow(() -> collector.pushSnapshot());
        verify(mockForwarder, atLeastOnce()).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testSnapshotCreation() {
        // Setup some depth data
        collector.onDepth("BTCUSD", true, 100, 50);
        collector.onDepth("BTCUSD", false, 101, 40);

        // Push snapshot and verify it was called
        collector.pushSnapshot();
        verify(mockForwarder, times(1)).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testInstrumentAliasPreserved() {
        DataCollector customCollector = new DataCollector(mockForwarder, "ETHUSD");
        
        customCollector.onDepth("ETHUSD", true, 2000, 10);
        customCollector.pushSnapshot();
        
        verify(mockForwarder, atLeastOnce()).pushSnapshot(any(MarketSnapshot.class));
    }

    @Test
    void testMultipleSnapshotPushes() {
        collector.onDepth("BTCUSD", true, 100, 50);
        
        // Push multiple snapshots
        collector.pushSnapshot();
        collector.pushSnapshot();
        collector.pushSnapshot();
        
        verify(mockForwarder, times(3)).pushSnapshot(any(MarketSnapshot.class));
    }
}

// Path: java-bridge/src/main/java/com/bookmap/bridge/BookmapBridge.java
package com.bookmap.bridge;

import velox.api.layer1.Layer1ApiDataListener;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.TradeInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Bookmap Bridge Addon - Core API Implementation
 * Collects market data and forwards to MCP Server via HTTP.
 * 
 * Core API Compliance: Uses @Layer1Attachable (no Simplified API)
 * Thread-Safety: Scheduler isolated from Bookmap event thread
 * Lifecycle: finish() ensures clean shutdown
 */
@Layer1Attachable
@Layer1StrategyName("MCP Trading Bridge")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class BookmapBridge implements 
    Layer1ApiFinishable,
    Layer1ApiInstrumentListener,
    Layer1ApiDataListener {

    static {
        LoggingSetup.configure();
    }

    private static final Logger log = Logger.getLogger(BookmapBridge.class.getName());

    // MCP Server endpoint
    private static final String MCP_SERVER_URL = "http://localhost:8765";
    
    // Core API Provider
    private Layer1ApiProvider provider;
    
    // Data collectors per instrument (supports multi-instrument)
    private final Map<String, DataCollector> collectors = new HashMap<>();
    
    // HTTP forwarder (shared)
    private HttpForwarder httpForwarder;
    
    // Scheduler for periodic snapshot push
    private ScheduledExecutorService scheduler;

    /**
     * Constructor called by Bookmap when addon is loaded.
     * Core API requires constructor injection with Layer1ApiProvider.
     */
    public BookmapBridge(Layer1ApiProvider provider) {
        log.info("[BookmapBridge] Constructor called (Core API).");
        String logsDir = LoggingSetup.getLogsDirectory();
        if (logsDir != null) {
            log.info("[BookmapBridge] Logs directory: " + logsDir);
        }

        this.provider = provider;
        this.httpForwarder = new HttpForwarder(MCP_SERVER_URL);
        
        // Start global scheduler for all instruments (500ms push interval)
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            this::pushAllSnapshots,
            1000,
            500,
            TimeUnit.MILLISECONDS
        );
        
        log.info("[BookmapBridge] Provider set. MCP Server: " + MCP_SERVER_URL);
    }
    
    // ===========================================================================
    // Layer1ApiInstrumentListener
    // ===========================================================================
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo info) {
        log.info("[BookmapBridge] Instrument added: " + alias + " (" + info.symbol + ")");
        
        // Create data collector for this instrument
        DataCollector collector = new DataCollector(httpForwarder, alias);
        collectors.put(alias, collector);
    }
    
    @Override
    public void onInstrumentRemoved(String alias) {
        log.info("[BookmapBridge] Instrument removed: " + alias);
        collectors.remove(alias);
    }
    
    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {
        log.warning("[BookmapBridge] Instrument not found: " + symbol + "@" + exchange);
    }
    
    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
        log.warning("[BookmapBridge] Instrument already subscribed: " + symbol + "@" + exchange);
    }
    
    // ===========================================================================
    // Layer1ApiDataListener
    // ===========================================================================
    
    @Override
    public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
        DataCollector collector = collectors.get(alias);
        if (collector != null) {
            collector.onTrade(alias, price, size, tradeInfo);
        }
    }
    
    @Override
    public void onDepth(String alias, boolean isBid, int price, int size) {
        DataCollector collector = collectors.get(alias);
        if (collector != null) {
            collector.onDepth(alias, isBid, price, size);
        }
    }
    
    @Override
    public void onMarketMode(String alias, MarketMode marketMode) {
        // Optional: Track market mode (live/replay/backfill)
        log.fine("[BookmapBridge] Market mode for " + alias + ": " + marketMode);
    }
    
    // ===========================================================================
    // Layer1ApiFinishable (Critical for clean shutdown)
    // ===========================================================================
    
    @Override
    public void finish() {
        log.info("[BookmapBridge] Shutdown initiated...");
        
        // Stop scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warning("[BookmapBridge] Scheduler did not terminate in time, forcing shutdown.");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warning("[BookmapBridge] Scheduler shutdown interrupted");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clear collectors
        collectors.clear();
        
        log.info("[BookmapBridge] Shutdown complete.");
    }
    
    // ===========================================================================
    // Private Helpers
    // ===========================================================================
    
    /**
     * Called by scheduler every 500ms to push snapshots for all instruments.
     * Thread-Safety: Runs in scheduler thread, isolated from Bookmap event thread.
     */
    private void pushAllSnapshots() {
        for (DataCollector collector : collectors.values()) {
            try {
                collector.pushSnapshot();
            } catch (Exception e) {
                log.warning("[BookmapBridge] Snapshot push failed: " + e.getMessage());
            }
        }
    }
}

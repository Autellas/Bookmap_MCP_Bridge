package com.bookmap.bridge;

import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.NoAutosubscription;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Bookmap Bridge Addon
 * Collects market data and forwards to MCP Server via HTTP.
 * 
 * Setup: Add this addon in Bookmap -> Addons -> Add
 */
@Layer1SimpleAttachable
@Layer1StrategyName("MCP Trading Bridge")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@NoAutosubscription
public class BookmapBridge implements CustomModule {

    static {
        LoggingSetup.configure();
    }

    private static final Logger log = Logger.getLogger(BookmapBridge.class.getName());

    private Api api;
    private DataCollector dataCollector;
    private HttpForwarder httpForwarder;
    private ScheduledExecutorService scheduler;

    // MCP Server endpoint - change if needed
    private static final String MCP_SERVER_URL = "http://localhost:8765";

    // No-argument constructor required by Bookmap Simplified API
    public BookmapBridge() {
        log.info("[BookmapBridge] Constructor called.");
        String logsDir = LoggingSetup.getLogsDirectory();
        if (logsDir != null) {
            log.info("[BookmapBridge] Logs directory: " + logsDir);
        }
    }
    
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
        this.api = api;
        
        log.info("[BookmapBridge] Initializing for instrument: " + alias);

        this.httpForwarder = new HttpForwarder(MCP_SERVER_URL);
        this.dataCollector = new DataCollector(httpForwarder, alias);

        // Register data listeners using Simplified API
        api.addTradeDataListeners(dataCollector);
        api.addDepthDataListeners(dataCollector);

        // Start periodic snapshot push (every 500ms)
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            () -> dataCollector.pushSnapshot(),
            1000,
            500,
            TimeUnit.MILLISECONDS
        );

        log.info("[BookmapBridge] Initialized. MCP Server: " + MCP_SERVER_URL);
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warning("[BookmapBridge] Scheduler shutdown interrupted");
            }
        }
        log.info("[BookmapBridge] Stopped.");
    }
}

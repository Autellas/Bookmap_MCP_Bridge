package com.bookmap.bridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures file-based logging for the bridge package.
 */
public final class LoggingSetup {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static volatile Path logsDirectory;

    private LoggingSetup() {
    }

    public static void configure() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        try {
            logsDirectory = Paths.get(System.getProperty("user.home"), "BookmapBridgeLogs");
            Files.createDirectories(logsDirectory);

            String pattern = logsDirectory.resolve("bookmap-bridge-%g.log").toString();

            FileHandler fileHandler = new FileHandler(pattern, 5 * 1024 * 1024, 3, true);
            fileHandler.setLevel(Level.FINE);
            fileHandler.setFormatter(new SimpleFormatter());

            Logger packageLogger = Logger.getLogger("com.bookmap.bridge");
            packageLogger.setLevel(Level.FINE);
            packageLogger.addHandler(fileHandler);

            Logger.getLogger(LoggingSetup.class.getName())
                .info("[LoggingSetup] File logging enabled: " + logsDirectory.toAbsolutePath());

        } catch (IOException e) {
            initialized.set(false);
            Logger.getLogger(LoggingSetup.class.getName())
                .warning("[LoggingSetup] Could not initialize file logging: " + e.getMessage());
        }
    }

    public static String getLogsDirectory() {
        return logsDirectory == null ? null : logsDirectory.toAbsolutePath().toString();
    }
}
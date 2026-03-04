package com.bookmap.bridge;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Forwards market snapshots to the MCP Python server via HTTP POST.
 * Endpoint: POST /snapshot
 */
public class HttpForwarder {

    private static final Logger log = Logger.getLogger(HttpForwarder.class.getName());
    private static final int TIMEOUT_MS = 1000;

    private final String baseUrl;

    public HttpForwarder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void pushSnapshot(MarketSnapshot snapshot) {
        String json = snapshot.toJson();
        sendPost("/snapshot", json);
    }

    private void sendPost(String path, String jsonBody) {
        try {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warning("[HttpForwarder] Non-200 response: " + responseCode);
            }
            conn.disconnect();

        } catch (ConnectException e) {
            // MCP server not running — suppress frequent logs
        } catch (Exception e) {
            log.warning("[HttpForwarder] Error: " + e.getMessage());
        }
    }
}

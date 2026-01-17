package com.oi.market.controller;

import com.oi.market.service.auth.TokenManager;
import com.oi.market.service.auth.MarketDataAuthorizeService;
import com.oi.market.service.websocket.WebSocketConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for WebSocket connection management.
 * Handles connection start/stop and status monitoring.
 */
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, methods = {RequestMethod.GET, RequestMethod.POST}, allowCredentials = "true")
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final WebSocketConnectionManager wsManager;
    private final TokenManager tokenManager;
    private final MarketDataAuthorizeService authorizeService;

    public WebSocketController(
            WebSocketConnectionManager wsManager,
            TokenManager tokenManager,
            MarketDataAuthorizeService authorizeService) {
        this.wsManager = wsManager;
        this.tokenManager = tokenManager;
        this.authorizeService = authorizeService;
    }

    /**
     * Start WebSocket connection for market data streaming
     * Requires valid authentication token
     *
     * @return Map with connection status
     */
    @PostMapping("/connect")
    public Mono<Map<String, Object>> connect() {
        logger.info("Attempting to establish WebSocket connection");
        
        // Check if user is authenticated
        if (!tokenManager.isTokenValid()) {
            logger.warn("WebSocket connection attempt without valid authentication");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Authentication required. Please login first.");
            return Mono.just(errorResponse);
        }

        // Get authorized WebSocket URL
        return authorizeService.getAuthorizedWebSocketUrl()
                .map(wsUrl -> {
                    // Mark connection as established
                    wsManager.markConnected(wsUrl);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "connecting");
                    response.put("wsUrl", wsUrl);
                    response.put("message", "WebSocket connection initiated");
                    response.put("timestamp", System.currentTimeMillis());
                    
                    logger.info("WebSocket connection initiated with URL: {}", wsUrl);
                    return response;
                })
                .onErrorResume(error -> {
                    logger.error("Failed to initiate WebSocket connection", error);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Failed to get WebSocket authorization: " + error.getMessage());
                    return Mono.just(errorResponse);
                });
    }

    /**
     * Stop WebSocket connection
     *
     * @return Map with disconnection status
     */
    @PostMapping("/disconnect")
    public Mono<Map<String, Object>> disconnect() {
        logger.info("Disconnecting WebSocket");
        wsManager.markDisconnected("User initiated disconnect");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "disconnected");
        response.put("message", "WebSocket disconnected successfully");
        response.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(response);
    }

    /**
     * Get current WebSocket connection status
     *
     * @return Map with connection details
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("connected", wsManager.isConnected());
        response.put("subscriptionCount", wsManager.getSubscriptionCount());
        response.put("timestamp", System.currentTimeMillis());
        
        if (wsManager.isConnected()) {
            response.put("connectionStartTime", wsManager.getConnectionStartTime());
            response.put("lastUpdateTime", wsManager.getLastUpdateTs());
            response.put("lastHeartbeatTime", wsManager.getLastHeartbeatTs());
            response.put("message", "WebSocket is connected and streaming data");
        } else {
            response.put("message", "WebSocket is not connected");
        }
        
        return Mono.just(response);
    }

    /**
     * Get detailed WebSocket metrics
     *
     * @return Map with metrics data
     */
    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getMetrics() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("connected", wsManager.isConnected());
        response.put("subscriptionCount", wsManager.getSubscriptionCount());
        response.put("connectionUptime", wsManager.getUptimeMs());
        response.put("reconnectAttempts", wsManager.getReconnectCount());
        response.put("lastError", wsManager.getLastError());
        response.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(response);
    }
}

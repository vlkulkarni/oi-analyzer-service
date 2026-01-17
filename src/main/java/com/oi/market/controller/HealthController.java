package com.oi.market.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for health check and general API information.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, methods = RequestMethod.GET)
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private static final String SERVICE_NAME = "OI Analyzer Service";
    private static final String SERVICE_VERSION = "1.0.0";

    /**
     * Health check endpoint
     *
     * @return Map with health status
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", SERVICE_NAME);
        response.put("version", SERVICE_VERSION);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.debug("Health check requested");
        return Mono.just(response);
    }

    /**
     * Get service information
     *
     * @return Map with service details
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", SERVICE_NAME);
        response.put("version", SERVICE_VERSION);
        response.put("description", "Real-time Option Chain Analysis from Upstox");
        response.put("features", new String[]{
            "OAuth2 Authentication with Upstox",
            "Real-time WebSocket Market Data Streaming",
            "Protobuf Message Decoding",
            "OHLC Data Processing",
            "Metrics Collection"
        });
        response.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(response);
    }

    /**
     * Get API endpoint information
     *
     * @return Map with available endpoints
     */
    @GetMapping("/endpoints")
    public Mono<Map<String, Object>> endpoints() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> authEndpoints = new HashMap<>();
        authEndpoints.put("GET /api/auth/login-url", "Get Upstox OAuth2 authorization URL");
        authEndpoints.put("GET /api/auth/callback", "Handle OAuth2 callback (called by Upstox)");
        authEndpoints.put("GET /api/auth/status", "Check authentication status");
        authEndpoints.put("POST /api/auth/logout", "Logout current user");
        authEndpoints.put("GET /api/auth/token-info", "Get token validity information");
        
        Map<String, Object> wsEndpoints = new HashMap<>();
        wsEndpoints.put("POST /api/websocket/connect", "Start WebSocket connection");
        wsEndpoints.put("POST /api/websocket/disconnect", "Stop WebSocket connection");
        wsEndpoints.put("GET /api/websocket/status", "Get WebSocket connection status");
        wsEndpoints.put("GET /api/websocket/metrics", "Get WebSocket metrics");
        
        Map<String, Object> healthEndpoints = new HashMap<>();
        healthEndpoints.put("GET /api/health", "Service health check");
        healthEndpoints.put("GET /api/info", "Service information");
        healthEndpoints.put("GET /api/endpoints", "List all endpoints");
        
        response.put("auth", authEndpoints);
        response.put("websocket", wsEndpoints);
        response.put("health", healthEndpoints);
        response.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(response);
    }
}

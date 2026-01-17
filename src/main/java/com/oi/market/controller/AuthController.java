package com.oi.market.controller;

import com.oi.market.dto.ApiResponse;
import com.oi.market.service.auth.UpstoxAuthService;
import com.oi.market.service.auth.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Upstox OAuth2 authentication.
 * Handles authorization flow and token management.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, methods = {RequestMethod.GET, RequestMethod.POST}, allowCredentials = "true")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UpstoxAuthService authService;
    private final TokenManager tokenManager;

    public AuthController(UpstoxAuthService authService, TokenManager tokenManager) {
        this.authService = authService;
        this.tokenManager = tokenManager;
    }

    /**
     * Generate and return Upstox OAuth2 authorization URL
     * Frontend redirects user to this URL
     *
     * @return Map containing authUrl
     */
    @GetMapping("/login-url")
    public Mono<Map<String, Object>> getLoginUrl() {
        try {
            String authUrl = authService.generateAuthorizationUrl();
            logger.info("Authorization URL generated");
            
            Map<String, Object> response = new HashMap<>();
            response.put("authUrl", authUrl);
            response.put("status", "success");
            
            return Mono.just(response);
        } catch (Exception e) {
            logger.error("Failed to generate authorization URL", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return Mono.just(errorResponse);
        }
    }

    /**
     * Handle OAuth callback from Upstox
     * Exchange authorization code for access token and redirect to frontend
     *
     * @param code Authorization code from Upstox
     * @return Redirect to frontend dashboard or error page
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> handleCallback(@RequestParam String code) {
        logger.info("Handling OAuth callback with authorization code");
        
        return authService.exchangeCodeForToken(code)
                .map(token -> {
                    logger.info("User authenticated successfully, redirecting to dashboard");
                    // Redirect to frontend dashboard
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("http://localhost:3000/"))
                            .<Void>build();
                })
                .onErrorResume(error -> {
                    logger.error("Authentication failed", error);
                    // Redirect to login page with error message
                    String errorMessage = error.getMessage();
                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("http://localhost:3000/login?error=" + 
                                encodeErrorMessage(errorMessage)))
                            .<Void>build());
                });
    }

    /**
     * URL encode error message for safe transmission in URL
     */
    private String encodeErrorMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return "Authentication failed";
        }
    }

    /**
     * Check current authentication status
     *
     * @return Map with authenticated flag and timestamp
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isAuthenticated = tokenManager.isTokenValid();
        response.put("authenticated", isAuthenticated);
        response.put("timestamp", System.currentTimeMillis());
        
        if (isAuthenticated) {
            response.put("message", "User is authenticated");
            logger.debug("User authentication status: authenticated");
        } else {
            response.put("message", "User is not authenticated");
            logger.debug("User authentication status: not authenticated");
        }
        
        return Mono.just(response);
    }

    /**
     * Logout user (invalidate token)
     *
     * @return Map with status
     */
    @PostMapping("/logout")
    public Mono<Map<String, Object>> logout() {
        tokenManager.invalidateToken();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User logged out successfully");
        logger.info("User logged out");
        
        return Mono.just(response);
    }

    /**
     * Get token validity information
     *
     * @return Map with token validity details
     */
    @GetMapping("/token-info")
    public Mono<Map<String, Object>> getTokenInfo() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("isValid", tokenManager.isTokenValid());
        response.put("timestamp", System.currentTimeMillis());
        
        if (tokenManager.isTokenValid()) {
            response.put("expiresAt", tokenManager.getExpiryTime());
            response.put("refreshAt", tokenManager.getNextRefreshTime());
        }
        
        return Mono.just(response);
    }
}

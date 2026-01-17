package com.oi.market.service.auth;

import com.oi.market.exception.UpstoxAuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service to authorize and get WebSocket URL for market data feed.
 * Calls Upstox /feed/market-data-feed/authorize endpoint.
 */
@Service
public class MarketDataAuthorizeService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataAuthorizeService.class);
    private static final String AUTHORIZE_ENDPOINT = "https://api.upstox.com/v3/feed/market-data-feed/authorize";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;
    private final TokenManager tokenManager;

    public MarketDataAuthorizeService(WebClient webClient, TokenManager tokenManager) {
        this.webClient = webClient;
        this.tokenManager = tokenManager;
    }

    /**
     * Get authorized WebSocket URL for market data feed
     */
    public Mono<String> getAuthorizedWebSocketUrl() {
        String token = tokenManager.getAccessToken();

        if (token == null || token.isEmpty()) {
            return Mono.error(new UpstoxAuthException("No valid access token available"));
        }

        return webClient.get()
                .uri(AUTHORIZE_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseWebSocketUrl)
                .doOnNext(wsUrl -> logger.info("Obtained WebSocket URL: {}", maskUrl(wsUrl)))
                .doOnError(error -> {
                    logger.error("Failed to get authorized WebSocket URL", error);
                    if (error instanceof IllegalStateException) {
                        tokenManager.invalidateToken();
                    }
                });
    }

    /**
     * Parse WebSocket URL from authorization response
     * Expects JSON response with "data" object containing WebSocket URL
     */
    private Mono<String> parseWebSocketUrl(String response) {
        try {
            logger.info("Full authorization response: {}", response);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            
            if (data == null) {
                logger.error("No 'data' field in response. Root fields: {}", root.fieldNames());
                // Try to get URL from root level
                for (String fieldName : new String[]{"authorizedRedirectUrl", "redirectUrl", "wsUrl", "url"}) {
                    JsonNode urlNode = root.get(fieldName);
                    if (urlNode != null && !urlNode.isNull()) {
                        String wsUrl = urlNode.asText();
                        logger.info("Found WebSocket URL at root level in field: {}", fieldName);
                        return Mono.just(wsUrl);
                    }
                }
                return Mono.error(new UpstoxAuthException("'data' field not found in authorization response"));
            }

            // Try multiple possible field names for the WebSocket URL
            String wsUrl = null;
            for (String fieldName : new String[]{"authorizedRedirectUri", "authorized_redirect_uri", "authorizedRedirectUrl", "redirectUrl", "wsUrl", "url"}) {
                JsonNode urlNode = data.get(fieldName);
                if (urlNode != null && !urlNode.isNull()) {
                    wsUrl = urlNode.asText();
                    logger.info("Found WebSocket URL in field: {}", fieldName);
                    break;
                }
            }

            if (wsUrl == null || wsUrl.isEmpty()) {
                logger.error("No WebSocket URL found in response. Available fields in data: {}", data.fieldNames());
                return Mono.error(new UpstoxAuthException("WebSocket URL not found in authorization response"));
            }

            if (!wsUrl.startsWith("wss://") && !wsUrl.startsWith("ws://")) {
                logger.error("Invalid WebSocket URL format: {}", wsUrl);
                return Mono.error(new UpstoxAuthException("Invalid WebSocket URL format: " + wsUrl));
            }

            logger.info("Successfully parsed WebSocket URL");
            return Mono.just(wsUrl);
        } catch (Exception e) {
            logger.error("Failed to parse authorization response: {}", response);
            return Mono.error(new UpstoxAuthException("Failed to parse authorization response: " + e.getMessage(), e));
        }
    }

    /**
     * Check if authorization is still valid
     */
    public boolean isAuthorizationValid() {
        return tokenManager.isTokenValid();
    }

    /**
     * Mask URL for logging (hide sensitive parts)
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return url;
        }
        return url.substring(0, 20) + "...";
    }
}

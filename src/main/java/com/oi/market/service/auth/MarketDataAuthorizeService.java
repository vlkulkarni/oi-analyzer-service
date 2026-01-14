package com.oi.market.service.auth;

import com.oi.market.exception.UpstoxAuthException;
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
     * Expects JSON response with "data" object containing "authorizedRedirectUrl"
     */
    private Mono<String> parseWebSocketUrl(String response) {
        try {
            // Parse "authorizedRedirectUrl" from JSON response
            // Expected format: {..., "data": {..., "authorizedRedirectUrl": "wss://..."}}

            int dataStartIdx = response.indexOf("\"data\"");
            if (dataStartIdx == -1) {
                return Mono.error(new UpstoxAuthException("'data' field not found in authorization response"));
            }

            int urlStartIdx = response.indexOf("\"authorizedRedirectUrl\"", dataStartIdx);
            if (urlStartIdx == -1) {
                return Mono.error(new UpstoxAuthException("'authorizedRedirectUrl' field not found in authorization response"));
            }

            // Find the URL value
            int quoteStart = response.indexOf("\"", urlStartIdx + 23);
            int quoteEnd = response.indexOf("\"", quoteStart + 1);

            if (quoteStart == -1 || quoteEnd == -1) {
                return Mono.error(new UpstoxAuthException("Invalid WebSocket URL format in response"));
            }

            String wsUrl = response.substring(quoteStart + 1, quoteEnd);

            if (!wsUrl.startsWith("wss://")) {
                return Mono.error(new UpstoxAuthException("Invalid WebSocket URL: " + wsUrl));
            }

            return Mono.just(wsUrl);
        } catch (Exception e) {
            return Mono.error(new UpstoxAuthException("Failed to parse authorization response", e));
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

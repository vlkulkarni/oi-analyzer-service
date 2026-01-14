package com.oi.market.service.auth;

import com.oi.market.exception.UpstoxAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Upstox OAuth2 authentication service.
 * Handles authorization flow and token generation.
 */
@Service
public class UpstoxAuthService {

    private static final Logger logger = LoggerFactory.getLogger(UpstoxAuthService.class);
    private static final String AUTH_ENDPOINT = "https://api.upstox.com/v2/login/authorization/dialog";
    private static final String TOKEN_ENDPOINT = "https://api.upstox.com/v2/login/authorization/token";

    @Value("${upstox.api.client-id}")
    private String clientId;

    @Value("${upstox.api.client-secret}")
    private String clientSecret;

    @Value("${upstox.api.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient;
    private final TokenManager tokenManager;

    // PKCE code challenge
    private String codeChallenge;
    private String codeVerifier;

    public UpstoxAuthService(WebClient webClient, TokenManager tokenManager) {
        this.webClient = webClient;
        this.tokenManager = tokenManager;
    }

    /**
     * Generate authorization URL for user login
     */
    public String generateAuthorizationUrl() throws Exception {
        // Generate PKCE code
        generatePKCE();

        StringBuilder authUrl = new StringBuilder(AUTH_ENDPOINT);
        authUrl.append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        authUrl.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        authUrl.append("&response_type=code");
        authUrl.append("&scope=").append(URLEncoder.encode("full_access", StandardCharsets.UTF_8));
        authUrl.append("&state=").append(generateRandomState());
        authUrl.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8));
        authUrl.append("&code_challenge_method=S256");

        logger.info("Generated authorization URL");
        return authUrl.toString();
    }

    /**
     * Exchange authorization code for access token
     */
    public Mono<String> exchangeCodeForToken(String authorizationCode) {
        if (codeVerifier == null) {
            return Mono.error(new UpstoxAuthException("Code verifier not found. Call generateAuthorizationUrl first."));
        }

        String body = "code=" + authorizationCode
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&redirect_uri=" + redirectUri
                + "&grant_type=authorization_code"
                + "&code_verifier=" + codeVerifier;

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseTokenResponse)
                .doOnNext(token -> {
                    tokenManager.setAccessToken(token);
                    logger.info("Access token obtained successfully");
                })
                .doOnError(error -> {
                    logger.error("Failed to exchange code for token", error);
                    throw new UpstoxAuthException("Token exchange failed: " + error.getMessage(), error);
                });
    }

    /**
     * Refresh the access token (if needed)
     */
    public Mono<String> refreshAccessToken(String refreshToken) {
        String body = "refresh_token=" + refreshToken
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&grant_type=refresh_token";

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseTokenResponse)
                .doOnNext(token -> {
                    tokenManager.setAccessToken(token);
                    logger.info("Access token refreshed successfully");
                })
                .doOnError(error -> {
                    logger.error("Failed to refresh token", error);
                    throw new UpstoxAuthException("Token refresh failed: " + error.getMessage(), error);
                });
    }

    /**
     * Parse access token from OAuth2 response
     * Expects JSON response with "access_token" field
     */
    private Mono<String> parseTokenResponse(String response) {
        try {
            // Simple JSON parsing for access_token
            // In production, use Jackson or other JSON library
            int startIdx = response.indexOf("\"access_token\":");
            if (startIdx == -1) {
                return Mono.error(new UpstoxAuthException("access_token not found in response"));
            }

            startIdx = response.indexOf("\"", startIdx + 15);
            int endIdx = response.indexOf("\"", startIdx + 1);

            if (startIdx == -1 || endIdx == -1) {
                return Mono.error(new UpstoxAuthException("Invalid token format in response"));
            }

            String token = response.substring(startIdx + 1, endIdx);
            return Mono.just(token);
        } catch (Exception e) {
            return Mono.error(new UpstoxAuthException("Failed to parse token response", e));
        }
    }

    /**
     * Generate PKCE code verifier and challenge
     */
    private void generatePKCE() throws Exception {
        // Generate random code verifier (43-128 unreserved chars)
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        this.codeVerifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // Generate code challenge (SHA256 hash of verifier)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        this.codeChallenge = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
    }

    /**
     * Generate random state for CSRF protection
     */
    private String generateRandomState() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return tokenManager.isTokenValid();
    }

    /**
     * Get current access token
     */
    public String getAccessToken() {
        return tokenManager.getAccessToken();
    }
}

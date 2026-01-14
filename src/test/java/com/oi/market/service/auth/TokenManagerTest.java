package com.oi.market.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenManager service
 */
@DisplayName("TokenManager Service Tests")
public class TokenManagerTest {

    private TokenManager tokenManager;

    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager();
    }

    @Test
    @DisplayName("Should set and retrieve access token")
    void testSetAndGetAccessToken() {
        String token = "test-token-123";
        tokenManager.setAccessToken(token);

        assertEquals(token, tokenManager.getAccessToken());
    }

    @Test
    @DisplayName("Should mark token as valid when set")
    void testTokenIsValidAfterSet() {
        tokenManager.setAccessToken("valid-token");
        assertTrue(tokenManager.isTokenValid(), "Token should be valid after being set");
    }

    @Test
    @DisplayName("Should return false for token validity when not set")
    void testTokenIsInvalidWhenNotSet() {
        assertFalse(tokenManager.isTokenValid(), "Token should be invalid when not set");
    }

    @Test
    @DisplayName("Should indicate token refresh is needed when not set")
    void testShouldRefreshWhenNotSet() {
        assertTrue(tokenManager.shouldRefreshToken(), "Should refresh when no token is set");
    }

    @Test
    @DisplayName("Should invalidate token on demand")
    void testInvalidateToken() {
        tokenManager.setAccessToken("valid-token");
        tokenManager.invalidateToken();

        assertFalse(tokenManager.isTokenValid(), "Token should be invalid after invalidation");
    }

    @Test
    @DisplayName("Should clear token state")
    void testClearToken() {
        tokenManager.setAccessToken("token-to-clear");
        tokenManager.clear();

        assertNull(tokenManager.getAccessToken(), "Token should be null after clear");
        assertFalse(tokenManager.isTokenValid(), "Token should be invalid after clear");
    }

    @Test
    @DisplayName("Should return negative time until expiry when token not set")
    void testTimeUntilExpiryWhenNotSet() {
        assertEquals(-1, tokenManager.getTimeUntilExpiry(), "Should return -1 when token not set");
    }

    @Test
    @DisplayName("Should return positive time until refresh when token is set")
    void testTimeUntilRefreshWhenSet() {
        tokenManager.setAccessToken("valid-token");
        long timeUntilRefresh = tokenManager.getTimeUntilRefresh();

        assertTrue(timeUntilRefresh > 0, "Should have positive time until refresh");
    }

    @Test
    @DisplayName("Should return token info string")
    void testGetTokenInfo() {
        tokenManager.setAccessToken("info-token");
        String info = tokenManager.getTokenInfo();

        assertNotNull(info, "Token info should not be null");
        assertTrue(info.contains("Token["), "Token info should contain Token[");
        assertTrue(info.contains("isValid="), "Token info should contain isValid=");
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void testThreadSafety() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                tokenManager.setAccessToken("token-" + i);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                tokenManager.getAccessToken();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertNotNull(tokenManager.getAccessToken(), "Token should be set after concurrent access");
    }
}

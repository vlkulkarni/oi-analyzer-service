package com.oi.market.service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReconnectionStrategy service
 * Note: Tests verify behavior with default values from application.yml
 */
@DisplayName("ReconnectionStrategy Service Tests")
public class ReconnectionStrategyTest {

    private ReconnectionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ReconnectionStrategy();
    }

    @Test
    @DisplayName("Should start with 0 retry count")
    void testInitialRetryCount() {
        assertEquals(0, strategy.getRetryCount(), "Should start with 0 retries");
    }

    @Test
    @DisplayName("Should track retry count")
    void testRetryCount() {
        assertEquals(0, strategy.getRetryCount(), "Should start with 0 retries");

        strategy.incrementRetryCount();
        assertEquals(1, strategy.getRetryCount(), "Should have 1 retry");

        strategy.incrementRetryCount();
        assertEquals(2, strategy.getRetryCount(), "Should have 2 retries");
    }

    @Test
    @DisplayName("Should return non-negative delay on next attempt")
    void testFirstAttemptDelay() {
        long delay = strategy.getNextDelayMs();
        assertTrue(delay >= 0, "Delay should be non-negative");
    }

    @Test
    @DisplayName("Should reset retry count on successful connection")
    void testResetOnSuccess() {
        strategy.incrementRetryCount();
        strategy.incrementRetryCount();
        assertEquals(2, strategy.getRetryCount(), "Should have 2 retries before reset");

        strategy.reset();
        assertEquals(0, strategy.getRetryCount(), "Should reset to 0 after successful connection");
    }

    @Test
    @DisplayName("Should calculate total wait time")
    void testTotalWaitTime() {
        for (int i = 0; i < 3; i++) {
            strategy.incrementRetryCount();
        }

        long totalWait = strategy.getTotalWaitTimeMs();
        assertTrue(totalWait >= 0, "Total wait time should be non-negative");
    }

    @Test
    @DisplayName("Should provide strategy info for logging")
    void testStrategyInfo() {
        strategy.incrementRetryCount();
        String info = strategy.getStrategyInfo();

        assertNotNull(info, "Strategy info should not be null");
        assertTrue(info.contains("ReconnectionStrategy["), "Should contain ReconnectionStrategy[");
        assertTrue(info.contains("attempt="), "Should contain attempt=");
        assertTrue(info.contains("maxRetries="), "Should contain maxRetries=");
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent increments")
    void testThreadSafety() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                strategy.incrementRetryCount();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                strategy.incrementRetryCount();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertEquals(100, strategy.getRetryCount(), "Should have 100 retries after concurrent increments");
    }
}

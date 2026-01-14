package com.oi.market.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exponential backoff reconnection strategy.
 * Implements exponential backoff with jitter for WebSocket reconnection.
 * Backoff sequence: 1s → 2s → 4s → 8s → 16s → 30s (capped)
 */
@Service
public class ReconnectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectionStrategy.class);

    @Value("${upstox.reconnect.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${upstox.reconnect.max-delay-ms:30000}")
    private long maxDelayMs;

    @Value("${upstox.reconnect.max-retries:5}")
    private int maxRetries;

    @Value("${upstox.reconnect.jitter-factor:0.1}")
    private double jitterFactor;

    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final SecureRandom random = new SecureRandom();

    /**
     * Get next delay in milliseconds with exponential backoff
     */
    public long getNextDelayMs() {
        int attempt = retryCount.get();

        // Calculate exponential backoff: initialDelay * 2^attempt
        long exponentialDelay = initialDelayMs * (1L << Math.min(attempt, 4)); // Cap at 2^4 = 16
        long delayMs = Math.min(exponentialDelay, maxDelayMs);

        // Add jitter: ±jitterFactor percentage
        long jitterRange = (long) (delayMs * jitterFactor);
        long jitter = random.nextLong(2 * jitterRange + 1) - jitterRange;
        delayMs = Math.max(initialDelayMs, delayMs + jitter);

        logger.info("Reconnection attempt {} - delay: {}ms", attempt + 1, delayMs);
        return delayMs;
    }

    /**
     * Get current retry count
     */
    public int getRetryCount() {
        return retryCount.get();
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        retryCount.incrementAndGet();
    }

    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount.get() >= maxRetries;
    }

    /**
     * Reset retry count (call on successful connection)
     */
    public void reset() {
        int previousCount = retryCount.getAndSet(0);
        if (previousCount > 0) {
            logger.info("Reconnection successful - retry count reset (was {})", previousCount);
        }
    }

    /**
     * Get total time spent waiting across all reconnection attempts
     */
    public long getTotalWaitTimeMs() {
        long totalWait = 0;
        for (int i = 0; i < retryCount.get(); i++) {
            long exponentialDelay = initialDelayMs * (1L << Math.min(i, 4));
            totalWait += Math.min(exponentialDelay, maxDelayMs);
        }
        return totalWait;
    }

    /**
     * Get strategy info for logging
     */
    public String getStrategyInfo() {
        return String.format("ReconnectionStrategy[attempt=%d, maxRetries=%d, nextDelay=%dms, totalWait=%dms]",
                retryCount.get() + 1,
                maxRetries,
                getNextDelayMs(),
                getTotalWaitTimeMs());
    }

    // Getters for testing
    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public double getJitterFactor() {
        return jitterFactor;
    }
}

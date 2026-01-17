package com.oi.market.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages WebSocket connection state and subscription information.
 * Thread-safe singleton for tracking connection status, subscription count, and metrics.
 */
@Service
public class WebSocketConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger subscriptionCount = new AtomicInteger(0);
    private final AtomicLong lastUpdateTs = new AtomicLong(0);
    private final AtomicLong lastHeartbeatTs = new AtomicLong(0);
    private final AtomicInteger reconnectCount = new AtomicInteger(0);
    private final AtomicLong connectionStartTime = new AtomicLong(0);

    private volatile String lastError;
    private volatile String currentWebSocketUrl;

    /**
     * Mark connection as established
     */
    public void markConnected(String wsUrl) {
        lock.writeLock().lock();
        try {
            connected.set(true);
            currentWebSocketUrl = wsUrl;
            connectionStartTime.set(System.currentTimeMillis());
            lastHeartbeatTs.set(System.currentTimeMillis());
            lastError = null;
            logger.info("WebSocket connected: {}", maskUrl(wsUrl));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Mark connection as disconnected
     */
    public void markDisconnected(String reason) {
        lock.writeLock().lock();
        try {
            connected.set(false);
            lastError = reason;
            logger.warn("WebSocket disconnected. Reason: {}", reason);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Add subscription (increment count)
     */
    public void addSubscription(int count) {
        int newCount = subscriptionCount.addAndGet(count);
        lastUpdateTs.set(System.currentTimeMillis());
        logger.debug("Added {} subscription(s). Total: {}", count, newCount);
    }

    /**
     * Remove subscription (decrement count)
     */
    public void removeSubscription(int count) {
        int newCount = subscriptionCount.addAndGet(-count);
        newCount = Math.max(0, newCount);
        subscriptionCount.set(newCount);
        lastUpdateTs.set(System.currentTimeMillis());
        logger.debug("Removed {} subscription(s). Total: {}", count, newCount);
    }

    /**
     * Get current subscription count
     */
    public int getSubscriptionCount() {
        return subscriptionCount.get();
    }

    /**
     * Update last update timestamp
     */
    public void updateLastUpdateTs() {
        lastUpdateTs.set(System.currentTimeMillis());
    }

    /**
     * Get last update timestamp
     */
    public long getLastUpdateTs() {
        return lastUpdateTs.get();
    }

    /**
     * Update last heartbeat timestamp
     */
    public void updateHeartbeat() {
        lastHeartbeatTs.set(System.currentTimeMillis());
    }

    /**
     * Get last heartbeat timestamp
     */
    public long getLastHeartbeatTs() {
        return lastHeartbeatTs.get();
    }

    /**
     * Check if heartbeat is stale (no heartbeat for given duration)
     */
    public boolean isHeartbeatStale(long maxStaleMs) {
        long currentTime = System.currentTimeMillis();
        long lastHeartbeat = lastHeartbeatTs.get();
        return (currentTime - lastHeartbeat) > maxStaleMs;
    }

    /**
     * Increment reconnect count
     */
    public void incrementReconnectCount() {
        reconnectCount.incrementAndGet();
    }

    /**
     * Get reconnect count
     */
    public int getReconnectCount() {
        return reconnectCount.get();
    }

    /**
     * Get connection start time
     */
    public long getConnectionStartTime() {
        return connectionStartTime.get();
    }

    /**
     * Get uptime in milliseconds
     */
    public long getUptimeMs() {
        long startTime = connectionStartTime.get();
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get last error message
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Set last error
     */
    public void setLastError(String error) {
        lock.writeLock().lock();
        try {
            this.lastError = error;
            logger.error("Connection error: {}", error);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current WebSocket URL
     */
    public String getCurrentWebSocketUrl() {
        return currentWebSocketUrl;
    }

    /**
     * Reset connection state (call when starting fresh connection)
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            connected.set(false);
            subscriptionCount.set(0);
            lastError = null;
            connectionStartTime.set(0);
            lastHeartbeatTs.set(0);
            logger.info("Connection state reset");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get connection status info for logging/debugging
     */
    public String getConnectionStatus() {
        return String.format(
                "WebSocket[connected=%b, subscriptions=%d, uptime=%dms, reconnects=%d, lastError=%s]",
                isConnected(),
                getSubscriptionCount(),
                getUptimeMs(),
                getReconnectCount(),
                lastError != null ? lastError.substring(0, Math.min(50, lastError.length())) : "none"
        );
    }

    /**
     * Mask URL for logging
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return url;
        }
        return url.substring(0, 20) + "...";
    }
}

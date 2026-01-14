package com.oi.market.service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebSocketConnectionManager service
 */
@DisplayName("WebSocketConnectionManager Service Tests")
public class WebSocketConnectionManagerTest {

    private WebSocketConnectionManager manager;

    @BeforeEach
    void setUp() {
        manager = new WebSocketConnectionManager();
    }

    @Test
    @DisplayName("Should start disconnected")
    void testStartDisconnected() {
        assertFalse(manager.isConnected(), "Should start disconnected");
    }

    @Test
    @DisplayName("Should mark as connected with URL")
    void testMarkConnected() {
        String wsUrl = "wss://api.upstox.com/v2/feed";
        manager.markConnected(wsUrl);

        assertTrue(manager.isConnected(), "Should be connected after markConnected");
        assertEquals(wsUrl, manager.getCurrentWebSocketUrl(), "Should store the WebSocket URL");
    }

    @Test
    @DisplayName("Should mark as disconnected")
    void testMarkDisconnected() {
        manager.markConnected("wss://api.upstox.com/v2/feed");
        manager.markDisconnected("Connection lost");

        assertFalse(manager.isConnected(), "Should be disconnected after markDisconnected");
        assertEquals("Connection lost", manager.getLastError(), "Should store the disconnect reason");
    }

    @Test
    @DisplayName("Should track subscription count")
    void testSubscriptionCount() {
        assertEquals(0, manager.getSubscriptionCount(), "Should start with 0 subscriptions");

        manager.addSubscription(5);
        assertEquals(5, manager.getSubscriptionCount(), "Should have 5 subscriptions after add");

        manager.addSubscription(3);
        assertEquals(8, manager.getSubscriptionCount(), "Should have 8 subscriptions total");

        manager.removeSubscription(2);
        assertEquals(6, manager.getSubscriptionCount(), "Should have 6 subscriptions after remove");
    }

    @Test
    @DisplayName("Should prevent negative subscription count")
    void testNegativeSubscriptionCount() {
        manager.removeSubscription(10);
        assertEquals(0, manager.getSubscriptionCount(), "Should not go below 0");
    }

    @Test
    @DisplayName("Should update last update timestamp")
    void testLastUpdateTimestamp() {
        long before = System.currentTimeMillis();
        manager.updateLastUpdateTs();
        long after = System.currentTimeMillis();

        long lastUpdate = manager.getLastUpdateTs();
        assertTrue(lastUpdate >= before && lastUpdate <= after, "Timestamp should be current");
    }

    @Test
    @DisplayName("Should track heartbeat")
    void testHeartbeat() {
        long before = System.currentTimeMillis();
        manager.updateHeartbeat();
        long after = System.currentTimeMillis();

        long lastHeartbeat = manager.getLastHeartbeatTs();
        assertTrue(lastHeartbeat >= before && lastHeartbeat <= after, "Heartbeat should be current");
    }

    @Test
    @DisplayName("Should detect stale heartbeat")
    void testStaleHeartbeat() throws InterruptedException {
        manager.updateHeartbeat();
        Thread.sleep(50);

        assertTrue(manager.isHeartbeatStale(10), "Should detect stale heartbeat");
        assertFalse(manager.isHeartbeatStale(100), "Should not detect fresh heartbeat as stale");
    }

    @Test
    @DisplayName("Should track reconnect count")
    void testReconnectCount() {
        assertEquals(0, manager.getReconnectCount(), "Should start with 0 reconnects");

        manager.incrementReconnectCount();
        assertEquals(1, manager.getReconnectCount(), "Should have 1 reconnect");

        manager.incrementReconnectCount();
        manager.incrementReconnectCount();
        assertEquals(3, manager.getReconnectCount(), "Should have 3 reconnects");
    }

    @Test
    @DisplayName("Should track connection uptime")
    void testConnectionUptime() throws InterruptedException {
        manager.markConnected("wss://api.upstox.com/v2/feed");

        Thread.sleep(100);

        long uptime = manager.getUptimeMs();
        assertTrue(uptime >= 100, "Uptime should be at least 100ms");
    }

    @Test
    @DisplayName("Should return 0 uptime when not connected")
    void testUptimeWhenNotConnected() {
        assertEquals(0, manager.getUptimeMs(), "Uptime should be 0 when not connected");
    }

    @Test
    @DisplayName("Should store and retrieve last error")
    void testLastError() {
        String error = "WebSocket read timeout";
        manager.setLastError(error);

        assertEquals(error, manager.getLastError(), "Should store and retrieve last error");
    }

    @Test
    @DisplayName("Should reset connection state")
    void testReset() {
        manager.markConnected("wss://api.upstox.com/v2/feed");
        manager.addSubscription(5);
        manager.setLastError("Some error");

        manager.reset();

        assertFalse(manager.isConnected(), "Should be disconnected after reset");
        assertEquals(0, manager.getSubscriptionCount(), "Should have 0 subscriptions after reset");
        assertNull(manager.getLastError(), "Should have no error after reset");
    }

    @Test
    @DisplayName("Should provide connection status info")
    void testConnectionStatus() {
        manager.markConnected("wss://api.upstox.com/v2/feed");
        manager.addSubscription(5);
        manager.setLastError("test error");

        String status = manager.getConnectionStatus();

        assertNotNull(status, "Status should not be null");
        assertTrue(status.contains("WebSocket["), "Should contain WebSocket[");
        assertTrue(status.contains("connected=true"), "Should show connected=true");
        assertTrue(status.contains("subscriptions=5"), "Should show subscription count");
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent operations")
    void testThreadSafety() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                manager.addSubscription(1);
                manager.updateHeartbeat();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                manager.getSubscriptionCount();
                manager.getLastHeartbeatTs();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertEquals(100, manager.getSubscriptionCount(), "Should have 100 subscriptions after concurrent access");
    }
}

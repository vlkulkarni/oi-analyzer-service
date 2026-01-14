package com.oi.market.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe token management service.
 * Handles access token storage, expiry validation, and refresh timing.
 * Token expires daily at 3:30 AM IST.
 */
@Service
public class TokenManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int EXPIRY_HOUR = 3;
    private static final int EXPIRY_MINUTE = 30;
    private static final int REFRESH_ADVANCE_MINUTES = 30;  // Refresh 30 minutes before expiry

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private String accessToken;
    private long tokenIssuedAt;  // System time when token was obtained
    private long nextRefreshTime; // System time when we should refresh
    private boolean isValid;

    /**
     * Set the access token
     */
    public void setAccessToken(String token) {
        lock.writeLock().lock();
        try {
            this.accessToken = token;
            this.tokenIssuedAt = System.currentTimeMillis();
            this.nextRefreshTime = calculateNextRefreshTime();
            this.isValid = true;
            logger.info("Access token updated. Next refresh at: {}", formatTime(nextRefreshTime));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current access token
     */
    public String getAccessToken() {
        lock.readLock().lock();
        try {
            return accessToken;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if token is valid (not expired)
     */
    public boolean isTokenValid() {
        lock.readLock().lock();
        try {
            if (!isValid || accessToken == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            long expiryTime = calculateTokenExpiryTime(tokenIssuedAt);

            return currentTime < expiryTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if we should refresh the token
     * Returns true if current time is past nextRefreshTime
     */
    public boolean shouldRefreshToken() {
        lock.readLock().lock();
        try {
            if (!isValid || accessToken == null) {
                return true;
            }

            long currentTime = System.currentTimeMillis();
            return currentTime >= nextRefreshTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get milliseconds until token expires
     */
    public long getTimeUntilExpiry() {
        lock.readLock().lock();
        try {
            if (!isValid || accessToken == null) {
                return -1;
            }

            long currentTime = System.currentTimeMillis();
            long expiryTime = calculateTokenExpiryTime(tokenIssuedAt);

            return Math.max(0, expiryTime - currentTime);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get milliseconds until next refresh is needed
     */
    public long getTimeUntilRefresh() {
        lock.readLock().lock();
        try {
            if (!isValid || accessToken == null) {
                return 0;
            }

            long currentTime = System.currentTimeMillis();
            return Math.max(0, nextRefreshTime - currentTime);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Invalidate the token (e.g., on auth error)
     */
    public void invalidateToken() {
        lock.writeLock().lock();
        try {
            this.isValid = false;
            logger.warn("Access token invalidated");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear token state
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            this.accessToken = null;
            this.isValid = false;
            this.tokenIssuedAt = 0;
            this.nextRefreshTime = 0;
            logger.debug("Token cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Calculate next refresh time: 3:30 AM IST - 30 minutes
     * If we're past that time today, return tomorrow's refresh time
     */
    private long calculateNextRefreshTime() {
        LocalDateTime now = LocalDateTime.now(IST_ZONE);
        LocalDateTime todayRefresh = now.withHour(EXPIRY_HOUR)
                .withMinute(EXPIRY_MINUTE - REFRESH_ADVANCE_MINUTES)
                .withSecond(0);

        if (now.isBefore(todayRefresh)) {
            return todayRefresh.atZone(IST_ZONE).toInstant().toEpochMilli();
        } else {
            // Next day at 3:00 AM IST
            return todayRefresh.plusDays(1).atZone(IST_ZONE).toInstant().toEpochMilli();
        }
    }

    /**
     * Calculate token expiry time: 3:30 AM IST next day
     * Tokens expire at 3:30 AM IST every day
     */
    private long calculateTokenExpiryTime(long issuedAtTime) {
        LocalDateTime issued = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(issuedAtTime),
                IST_ZONE
        );

        LocalDateTime expiryDateTime = issued.withHour(EXPIRY_HOUR)
                .withMinute(EXPIRY_MINUTE)
                .withSecond(0);

        // If token was issued after 3:30 AM, expiry is next day at 3:30 AM
        if (issued.getHour() > EXPIRY_HOUR || 
            (issued.getHour() == EXPIRY_HOUR && issued.getMinute() >= EXPIRY_MINUTE)) {
            expiryDateTime = expiryDateTime.plusDays(1);
        }

        return expiryDateTime.atZone(IST_ZONE).toInstant().toEpochMilli();
    }

    /**
     * Helper to format milliseconds as readable datetime
     */
    private String formatTime(long epochMillis) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                IST_ZONE
        ).toString();
    }

    /**
     * Get token info for logging/debugging
     */
    public String getTokenInfo() {
        lock.readLock().lock();
        try {
            return String.format("Token[isValid=%b, timeUntilExpiry=%dms, timeUntilRefresh=%dms]",
                    isValid,
                    getTimeUntilExpiry(),
                    getTimeUntilRefresh());
        } finally {
            lock.readLock().unlock();
        }
    }
}

package com.oi.market.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper class for recording and reporting metrics related to market data processing
 */
@Component
public class MetricsHelper {
    private static final Logger log = LoggerFactory.getLogger(MetricsHelper.class);

    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong decodeErrorCount = new AtomicLong(0);
    private final AtomicLong reconnectCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> latencyBuckets = new ConcurrentHashMap<>();

    public void recordMessage() {
        messageCount.incrementAndGet();
    }

    public void recordLatency(long latencyMicros) {
        latencyBuckets.put("p50", calculatePercentile(50));
        latencyBuckets.put("p95", calculatePercentile(95));
        latencyBuckets.put("p99", calculatePercentile(99));
    }

    public void recordDecodeError() {
        decodeErrorCount.incrementAndGet();
    }

    public void recordReconnect() {
        reconnectCount.incrementAndGet();
    }

    public long getMessageCount() {
        return messageCount.get();
    }

    public long getDecodeErrorCount() {
        return decodeErrorCount.get();
    }

    public long getReconnectCount() {
        return reconnectCount.get();
    }

    private long calculatePercentile(int percentile) {
        // Placeholder for actual percentile calculation
        return 0L;
    }
}

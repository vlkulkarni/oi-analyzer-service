package com.oi.market.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Object pool for Protobuf message reuse to minimize garbage collection pressure
 */
@Component
public class ProtoMessagePool {
    private static final Logger log = LoggerFactory.getLogger(ProtoMessagePool.class);

    private final Queue<byte[]> bufferPool;
    private final int bufferSize;
    private final int maxPoolSize;

    public ProtoMessagePool() {
        this.bufferSize = 4096; // 4KB typical frame size
        this.maxPoolSize = 1000;
        this.bufferPool = new ConcurrentLinkedQueue<>();
        
        // Pre-allocate initial buffers
        for (int i = 0; i < 100; i++) {
            bufferPool.offer(new byte[bufferSize]);
        }
    }

    /**
     * Acquire a buffer from the pool, or create a new one if pool is empty
     */
    public byte[] acquireBuffer() {
        byte[] buffer = bufferPool.poll();
        if (buffer == null) {
            buffer = new byte[bufferSize];
        }
        return buffer;
    }

    /**
     * Release a buffer back to the pool if pool hasn't reached max size
     */
    public void releaseBuffer(byte[] buffer) {
        if (bufferPool.size() < maxPoolSize) {
            bufferPool.offer(buffer);
        }
    }

    /**
     * Get current pool size
     */
    public int getPoolSize() {
        return bufferPool.size();
    }
}

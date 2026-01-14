package com.oi.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;

@Configuration
public class ExecutorConfig {

    /**
     * Thread executor for I/O-bound WebSocket operations
     * Uses cached thread pool for flexibility with WebSocket connections
     */
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            private final ThreadFactory delegate = Executors.defaultThreadFactory();
            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = delegate.newThread(r);
                t.setName("market-io-" + (++count));
                return t;
            }
        });
    }

    /**
     * Scheduled executor for periodic tasks (token refresh, health checks)
     */
    @Bean(name = "scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(4, new ThreadFactory() {
            private final ThreadFactory delegate = Executors.defaultThreadFactory();
            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = delegate.newThread(r);
                t.setName("market-scheduled-" + (++count));
                return t;
            }
        });
    }

    /**
     * Fork-Join Pool for CPU-bound processing (default behavior)
     */
    @Bean(name = "forkJoinPool")
    public ForkJoinPool forkJoinPool() {
        return ForkJoinPool.commonPool();
    }
}

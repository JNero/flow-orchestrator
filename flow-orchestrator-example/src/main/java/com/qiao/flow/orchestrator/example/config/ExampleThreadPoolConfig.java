package com.qiao.flow.orchestrator.example.config;

import com.qiao.flow.orchestrator.core.threadpool.ThreadPoolConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Example线程池配置
 * 提供默认的线程池实现
 *
 * @author qiao
 */
@Component
public class ExampleThreadPoolConfig implements ThreadPoolConfig {

    @Override
    public ExecutorService getDagCpuThreadPool() {
        return new java.util.concurrent.ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                60L, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(200),
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "example-dag-cpu-" + (++counter));
                    }
                },
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public ExecutorService getDagIoThreadPool() {
        ThreadFactory factory = Thread.ofVirtual().name("example-dag-io-", 1).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Override
    public ExecutorService getOpsItemDimensionThreadPool() {
        ThreadFactory factory = Thread.ofVirtual().name("example-ops-item-", 1).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Override
    public ExecutorService getOpsFactorThreadPool() {
        ThreadFactory factory = Thread.ofVirtual().name("example-ops-factor-", 1).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    @Override
    public ExecutorService getOpsContextDimensionThreadPool() {
        ThreadFactory factory = Thread.ofVirtual().name("example-ops-context-", 1).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }
}

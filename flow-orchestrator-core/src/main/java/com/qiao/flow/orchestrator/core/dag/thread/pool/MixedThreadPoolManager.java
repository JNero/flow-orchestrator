package com.qiao.flow.orchestrator.core.dag.thread.pool;

import lombok.Getter;

import java.util.concurrent.ExecutorService;

/**
 * 混合线程池管理器
 * 管理IO和CPU两种类型的线程池，根据节点类型选择合适的线程池
 * 支持外部注入线程池，完全解耦
 *
 * @author qiao
 */
@Getter
public class MixedThreadPoolManager {

    private final ExecutorService cpuThreadPool;
    private final ExecutorService ioThreadPool;

    public MixedThreadPoolManager(ExecutorService cpuThreadPool, ExecutorService ioThreadPool) {
        this.cpuThreadPool = cpuThreadPool;
        this.ioThreadPool = ioThreadPool;
    }
}
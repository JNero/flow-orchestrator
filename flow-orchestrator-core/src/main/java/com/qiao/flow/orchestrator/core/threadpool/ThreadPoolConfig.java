package com.qiao.flow.orchestrator.core.threadpool;

import java.util.concurrent.ExecutorService;

/**
 * 线程池配置接口
 * 外界通过实现此接口来提供线程池
 *
 * @author qiao
 */
public interface ThreadPoolConfig {

    /**
     * 获取DAG CPU线程池
     * 用于CPU密集型节点执行
     */
    ExecutorService getDagCpuThreadPool();

    /**
     * 获取DAG IO线程池
     * 用于IO密集型节点执行（虚拟线程）
     */
    ExecutorService getDagIoThreadPool();

    /**
     * 获取OPS Item维度线程池
     * 用于物品维度计算
     */
    ExecutorService getOpsItemDimensionThreadPool();

    /**
     * 获取OPS Factor线程池
     * 用于因子计算
     */
    ExecutorService getOpsFactorThreadPool();

    /**
     * 获取OPS Context维度线程池
     * 用于上下文维度计算
     */
    ExecutorService getOpsContextDimensionThreadPool();
}

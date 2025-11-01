package com.qiao.flow.orchestrator.core.dag.context;

import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * DagContext对象池
 * 使用全局对象池复用DagContext对象，减少对象创建和GC压力
 * 支持空闲对象驱逐机制，适合高QPS场景
 *
 * @author qiao
 */
@Slf4j
public class DagContextPool {

    // 核心存储：无锁队列
    private static final Queue<DagContext> POOL = new ConcurrentLinkedQueue<>();

    // 配置参数
    private static final int MAX_POOL_SIZE = 5000; // 池大小上限
    private static final int INITIAL_POOL_SIZE = 500; // 初始池大小
    private static final int MIN_IDLE_SIZE = 100; // 最小空闲对象数

    // 空闲对象驱逐配置 - 可动态调整
    private static final AtomicLong TIME_BETWEEN_EVICTION_RUNS_MILLIS = new AtomicLong(30000); // 30秒检查一次
    private static final AtomicLong MIN_EVICTABLE_IDLE_TIME_MILLIS = new AtomicLong(60000); // 空闲1分钟可被驱逐
    private static final int NUM_TESTS_PER_EVICTION_RUN = 10; // 每次驱逐检查的对象数

    // 驱逐线程
    private static final ScheduledExecutorService EVICTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DagContextPool-Evictor");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> evictionTask;


    // 静态初始化
    static {
        // 预创建一些对象
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            DagContext context = new DagContext();
            POOL.offer(context);
        }
        log.info("DagContextPool initialized with {} objects", INITIAL_POOL_SIZE);

        // 启动驱逐线程
        startEvictionTask();
    }

    /**
     * 启动驱逐任务
     */
    private static void startEvictionTask() {
        if (evictionTask == null || evictionTask.isDone()) {
            evictionTask = EVICTOR.scheduleWithFixedDelay(
                    DagContextPool::evictIdleObjects,
                    TIME_BETWEEN_EVICTION_RUNS_MILLIS.get(),
                    TIME_BETWEEN_EVICTION_RUNS_MILLIS.get(),
                    TimeUnit.MILLISECONDS
            );
            log.info("DagContextPool eviction task started, interval: {}ms", TIME_BETWEEN_EVICTION_RUNS_MILLIS.get());
        }
    }

    /**
     * 重启驱逐任务（用于动态调整间隔）
     */
    private static void restartEvictionTask() {
        if (evictionTask != null && !evictionTask.isDone()) {
            evictionTask.cancel(false);
        }
        startEvictionTask();
    }

    /**
     * 驱逐空闲对象
     */
    private static void evictIdleObjects() {
        try {
            int poolSize = POOL.size();
            if (poolSize <= MIN_IDLE_SIZE) {
                return; // 池中对象太少，不驱逐
            }

            int evictedCount = 0;

            // 检查并驱逐空闲对象
            for (int i = 0; i < NUM_TESTS_PER_EVICTION_RUN; i++) {
                DagContext context = POOL.poll();
                if (context == null) {
                    break;
                }

                // 检查对象是否空闲时间过长
                if (context.getIdleTime() > MIN_EVICTABLE_IDLE_TIME_MILLIS.get()) {
                    // 对象空闲时间过长，直接丢弃
                    evictedCount++;
                } else {
                    // 对象还可以继续使用，放回池中
                    POOL.offer(context);
                }
            }

            if (evictedCount > 0) {

            }

        } catch (Exception e) {
            log.warn("DagContextPool eviction task failed", e);
        }
    }

    /**
     * 借用一个DagContext对象
     */
    public static DagContext borrow() {
        // 尝试从池中获取对象
        DagContext context = POOL.poll();

        if (context == null) {
            // 池为空，创建新对象
            context = new DagContext();
        } else {
            // 记录借用时间
            context.setBorrowTime(System.currentTimeMillis());
        }

        // 清理对象状态
        context.clear();
        return context;
    }

    /**
     * 归还一个DagContext对象
     */
    public static void release(DagContext context) {
        if (context == null) {
            return;
        }

        // 记录归还时间
        context.setReturnTime(System.currentTimeMillis());

        // 检查池大小，避免无限增长
        if (POOL.size() < MAX_POOL_SIZE) {
            // 清理对象状态
            context.clear();

            // 放回池中
            POOL.offer(context);
        }
        // 池满时让对象自然回收
    }


    /**
     * 获取对象池当前大小
     */
    public static int getPoolSize() {
        return POOL.size();
    }

    /**
     * 设置驱逐检查间隔（毫秒）
     *
     * @param intervalMs 间隔时间，单位毫秒
     */
    public static void setEvictionInterval(long intervalMs) {
        if (intervalMs < 1000) {
            log.warn("Eviction interval too small: {}ms, using minimum 1000ms", intervalMs);
            intervalMs = 1000;
        }

        long oldInterval = TIME_BETWEEN_EVICTION_RUNS_MILLIS.getAndSet(intervalMs);
        log.info("DagContextPool eviction interval changed from {}ms to {}ms", oldInterval, intervalMs);

        // 重启驱逐任务以应用新间隔
        restartEvictionTask();
    }

    /**
     * 设置空闲对象可驱逐时间（毫秒）
     *
     * @param idleTimeMs 空闲时间，单位毫秒
     */
    public static void setEvictableIdleTime(long idleTimeMs) {
        if (idleTimeMs < 1000) {
            log.warn("Evictable idle time too small: {}ms, using minimum 1000ms", idleTimeMs);
            idleTimeMs = 1000;
        }

        long oldTime = MIN_EVICTABLE_IDLE_TIME_MILLIS.getAndSet(idleTimeMs);
        log.info("DagContextPool evictable idle time changed from {}ms to {}ms", oldTime, idleTimeMs);
    }

    /**
     * 获取当前驱逐检查间隔（毫秒）
     */
    public static long getEvictionInterval() {
        return TIME_BETWEEN_EVICTION_RUNS_MILLIS.get();
    }

    /**
     * 获取当前空闲对象可驱逐时间（毫秒）
     */
    public static long getEvictableIdleTime() {
        return MIN_EVICTABLE_IDLE_TIME_MILLIS.get();
    }


}

package com.qiao.flow.orchestrator.core.dag.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 集合对象池
 * 复用Set和List对象，减少对象创建和GC压力
 * 支持空闲对象驱逐机制，适合高QPS场景
 *
 * @author qiao
 */
@Slf4j
public class CollectionPool {

    // Set对象池
    private static final Queue<PooledSet> SET_POOL = new ConcurrentLinkedQueue<>();
    private static final int MAX_SET_POOL_SIZE = 1000;
    private static final int INITIAL_SET_POOL_SIZE = 100;
    private static final int MIN_SET_IDLE_SIZE = 20;

    // List对象池
    private static final Queue<PooledList> LIST_POOL = new ConcurrentLinkedQueue<>();
    private static final int MAX_LIST_POOL_SIZE = 1000;
    private static final int INITIAL_LIST_POOL_SIZE = 100;
    private static final int MIN_LIST_IDLE_SIZE = 20;

    // 空闲对象驱逐配置 - 可动态调整
    private static final AtomicLong TIME_BETWEEN_EVICTION_RUNS_MILLIS = new AtomicLong(30000); // 30秒检查一次
    private static final AtomicLong MIN_EVICTABLE_IDLE_TIME_MILLIS = new AtomicLong(60000); // 空闲1分钟可被驱逐
    private static final int NUM_TESTS_PER_EVICTION_RUN = 10; // 每次驱逐检查的对象数

    // 驱逐线程
    private static final ScheduledExecutorService EVICTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CollectionPool-Evictor");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> evictionTask;


    // 静态初始化
    static {
        // 预创建Set对象
        for (int i = 0; i < INITIAL_SET_POOL_SIZE; i++) {
            SET_POOL.offer(new PooledSet(new HashSet<>(16)));
        }

        // 预创建List对象
        for (int i = 0; i < INITIAL_LIST_POOL_SIZE; i++) {
            LIST_POOL.offer(new PooledList(new ArrayList<>(16)));
        }

        log.info("CollectionPool initialized with {} Sets and {} Lists",
                INITIAL_SET_POOL_SIZE, INITIAL_LIST_POOL_SIZE);

        // 启动驱逐线程
        startEvictionTask();
    }

    /**
     * 启动驱逐任务
     */
    private static void startEvictionTask() {
        if (evictionTask == null || evictionTask.isDone()) {
            evictionTask = EVICTOR.scheduleWithFixedDelay(
                    CollectionPool::evictIdleObjects,
                    TIME_BETWEEN_EVICTION_RUNS_MILLIS.get(),
                    TIME_BETWEEN_EVICTION_RUNS_MILLIS.get(),
                    TimeUnit.MILLISECONDS
            );
            log.info("CollectionPool eviction task started, interval: {}ms", TIME_BETWEEN_EVICTION_RUNS_MILLIS.get());
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
            // 驱逐Set对象
            evictIdleSets();

            // 驱逐List对象
            evictIdleLists();

        } catch (Exception e) {
            log.warn("CollectionPool eviction task failed", e);
        }
    }

    /**
     * 驱逐空闲Set对象
     */
    private static void evictIdleSets() {
        int poolSize = SET_POOL.size();
        if (poolSize <= MIN_SET_IDLE_SIZE) {
            return;
        }

        for (int i = 0; i < NUM_TESTS_PER_EVICTION_RUN; i++) {
            PooledSet pooledSet = SET_POOL.poll();
            if (pooledSet == null) {
                break;
            }

            if (pooledSet.getIdleTime() > MIN_EVICTABLE_IDLE_TIME_MILLIS.get()) {
                // 对象空闲时间过长，直接丢弃
            } else {
                // 对象还可以继续使用，放回池中
                SET_POOL.offer(pooledSet);
            }
        }
    }

    /**
     * 驱逐空闲List对象
     */
    private static void evictIdleLists() {
        int poolSize = LIST_POOL.size();
        if (poolSize <= MIN_LIST_IDLE_SIZE) {
            return;
        }

        for (int i = 0; i < NUM_TESTS_PER_EVICTION_RUN; i++) {
            PooledList pooledList = LIST_POOL.poll();
            if (pooledList == null) {
                break;
            }

            if (pooledList.getIdleTime() > MIN_EVICTABLE_IDLE_TIME_MILLIS.get()) {
                // 对象空闲时间过长，直接丢弃
            } else {
                // 对象还可以继续使用，放回池中
                LIST_POOL.offer(pooledList);
            }
        }
    }

    /**
     * 借用一个Set<String>对象
     */
    public static Set<String> borrowSet() {
        PooledSet pooledSet = SET_POOL.poll();
        if (pooledSet == null) {
            pooledSet = new PooledSet(new HashSet<>(16));
        } else {
            pooledSet.setBorrowTime(System.currentTimeMillis());
        }

        return pooledSet.getSet();
    }

    /**
     * 归还一个Set<String>对象
     */
    public static void releaseSet(Set<String> set) {
        if (set == null) {
            return;
        }

        if (SET_POOL.size() < MAX_SET_POOL_SIZE) {
            PooledSet pooledSet = new PooledSet(set);
            pooledSet.setReturnTime(System.currentTimeMillis());
            pooledSet.getSet().clear();
            SET_POOL.offer(pooledSet);
        }
    }

    /**
     * 借用一个List<String>对象
     */
    public static List<String> borrowList() {
        PooledList pooledList = LIST_POOL.poll();
        if (pooledList == null) {
            pooledList = new PooledList(new ArrayList<>(16));
        } else {
            pooledList.setBorrowTime(System.currentTimeMillis());
        }

        return pooledList.getList();
    }

    /**
     * 归还一个List<String>对象
     */
    public static void releaseList(List<String> list) {
        if (list == null) {
            return;
        }

        if (LIST_POOL.size() < MAX_LIST_POOL_SIZE) {
            PooledList pooledList = new PooledList(list);
            pooledList.setReturnTime(System.currentTimeMillis());
            pooledList.getList().clear();
            LIST_POOL.offer(pooledList);
        }
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
        log.info("CollectionPool eviction interval changed from {}ms to {}ms", oldInterval, intervalMs);

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
        log.info("CollectionPool evictable idle time changed from {}ms to {}ms", oldTime, idleTimeMs);
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


    /**
     * 池化的Set包装类
     */
    private static class PooledSet {
        private final Set<String> set;
        private volatile long borrowTime = 0L;
        private volatile long returnTime = 0L;

        public PooledSet(Set<String> set) {
            this.set = set;
        }

        public Set<String> getSet() {
            return set;
        }

        public void setBorrowTime(long borrowTime) {
            this.borrowTime = borrowTime;
        }

        public void setReturnTime(long returnTime) {
            this.returnTime = returnTime;
        }

        public long getIdleTime() {
            if (returnTime == 0L) {
                return 0L;
            }
            return System.currentTimeMillis() - returnTime;
        }
    }

    /**
     * 池化的List包装类
     */
    private static class PooledList {
        private final List<String> list;
        private volatile long borrowTime = 0L;
        private volatile long returnTime = 0L;

        public PooledList(List<String> list) {
            this.list = list;
        }

        public List<String> getList() {
            return list;
        }

        public void setBorrowTime(long borrowTime) {
            this.borrowTime = borrowTime;
        }

        public void setReturnTime(long returnTime) {
            this.returnTime = returnTime;
        }

        public long getIdleTime() {
            if (returnTime == 0L) {
                return 0L;
            }
            return System.currentTimeMillis() - returnTime;
        }
    }


}

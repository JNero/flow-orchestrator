package com.qiao.flow.orchestrator.core.dag.cleanup;

import com.qiao.flow.orchestrator.core.dag.context.DagContextPool;
import com.qiao.flow.orchestrator.core.dag.pool.CollectionPool;
import com.qiao.flow.orchestrator.core.dag.runner.DagAutoRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DAG清理调度器
 * 定期清理DAG框架中的对象，解决Old Gen GC问题
 * 启动时根据配置文件设置清理间隔
 *
 * @author qiao
 */
@Slf4j
@Component
public class DagCleanupScheduler {

    @Autowired
    private DagAutoRunner dagAutoRunner;

    /**
     * 定时清理任务
     * 根据配置文件设置间隔，启动时确定
     * 注意：对象池现在有自动驱逐机制，这里主要是清理缓存
     */
    @Scheduled(fixedRateString = "${dag.cleanup.scheduler-interval:300000}")
    public void scheduledCleanup() {
        try {
            log.info("Starting scheduled DAG cleanup...");

            // 清理缓存（对象池现在有自动驱逐机制）
            dagAutoRunner.forceCleanup();

            log.info("Scheduled DAG cleanup completed");

        } catch (Exception e) {
            log.warn("Scheduled DAG cleanup failed", e);
        }
    }

    /**
     * 设置所有清理相关的间隔
     *
     * @param evictionIntervalMs  驱逐检查间隔（毫秒）
     * @param evictableIdleTimeMs 空闲对象可驱逐时间（毫秒）
     */
    public void setAllIntervals(long evictionIntervalMs, long evictableIdleTimeMs) {
        // 设置对象池驱逐间隔
        DagContextPool.setEvictionInterval(evictionIntervalMs);
        CollectionPool.setEvictionInterval(evictionIntervalMs);

        // 设置空闲对象可驱逐时间
        DagContextPool.setEvictableIdleTime(evictableIdleTimeMs);
        CollectionPool.setEvictableIdleTime(evictableIdleTimeMs);

        log.info("All cleanup intervals updated - Eviction: {}ms, IdleTime: {}ms",
                evictionIntervalMs, evictableIdleTimeMs);
    }
}

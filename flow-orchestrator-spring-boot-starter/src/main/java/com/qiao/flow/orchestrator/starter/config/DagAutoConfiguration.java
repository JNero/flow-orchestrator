package com.qiao.flow.orchestrator.starter.config;

import com.qiao.flow.orchestrator.core.dag.cleanup.DagCleanupScheduler;
import com.qiao.flow.orchestrator.core.dag.thread.pool.MixedThreadPoolManager;
import com.qiao.flow.orchestrator.core.threadpool.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DAG Spring Boot Auto-configuring
 * 实现DAG与Spring的深度融合
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({DagCleanupConfigProperties.class})
public class DagAutoConfiguration {

    /**
     * 混合线程池管理器
     * 只有在提供ThreadPoolConfig实现时才创建
     */
    @Bean
    @ConditionalOnBean(ThreadPoolConfig.class)
    public MixedThreadPoolManager mixedThreadPoolManager(ThreadPoolConfig config) {
        return new MixedThreadPoolManager(
                config.getDagCpuThreadPool(),
                config.getDagIoThreadPool()
        );
    }

    /**
     * DAG清理调度器
     */
    @Bean
    @ConditionalOnMissingBean
    public DagCleanupScheduler dagCleanupScheduler(DagCleanupConfigProperties cleanupProperties) {
        DagCleanupScheduler scheduler = new DagCleanupScheduler();

        // 应用策略配置（优先级：单独配置 > 策略配置 > 默认配置）
        if (cleanupProperties.isEnabled()) {
            scheduler.setAllIntervals(
                    cleanupProperties.getFinalEvictionIntervalMs(),
                    cleanupProperties.getFinalEvictableIdleTimeMs()
            );

            // 输出配置说明
            log.info("DAG cleanup configuration initialization completed:\n{}", cleanupProperties.getConfigDescription());
        }

        return scheduler;
    }
} 
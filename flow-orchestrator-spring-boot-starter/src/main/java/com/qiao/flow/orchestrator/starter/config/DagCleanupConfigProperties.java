package com.qiao.flow.orchestrator.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DAG清理配置属性
 * 支持策略配置和单独配置的优先级
 *
 * @author qiao
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "dag.cleanup")
public class DagCleanupConfigProperties {

    /**
     * 是否启用清理功能
     */
    private boolean enabled = true;

    /**
     * 预设策略配置
     */
    private CleanupPreset preset = CleanupPreset.BALANCED;

    /**
     * 单独配置（优先级高于策略配置）
     * null或0表示使用策略配置
     */
    private Long evictionIntervalMs;      // null表示使用策略配置
    private Long evictableIdleTimeMs;     // null表示使用策略配置
    private Long schedulerInterval;       // null表示使用策略配置

    /**
     * 清理预设策略
     */
    public enum CleanupPreset {
        AGGRESSIVE("激进配置 - 频繁清理，快速驱逐"),
        BALANCED("平衡配置 - 默认配置"),
        CONSERVATIVE("保守配置 - 较少清理，较慢驱逐");

        private final String description;

        CleanupPreset(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取最终的驱逐检查间隔
     * 优先级：单独配置 > 策略配置 > 默认配置
     */
    public long getFinalEvictionIntervalMs() {
        if (evictionIntervalMs != null && evictionIntervalMs > 0) {
            return evictionIntervalMs; // 使用单独配置
        }
        return getPresetEvictionIntervalMs(); // 使用策略配置
    }

    /**
     * 获取最终的空闲对象可驱逐时间
     */
    public long getFinalEvictableIdleTimeMs() {
        if (evictableIdleTimeMs != null && evictableIdleTimeMs > 0) {
            return evictableIdleTimeMs; // 使用单独配置
        }
        return getPresetEvictableIdleTimeMs(); // 使用策略配置
    }

    /**
     * 获取最终的定时任务调度间隔
     */
    public long getFinalSchedulerInterval() {
        if (schedulerInterval != null && schedulerInterval > 0) {
            return schedulerInterval; // 使用单独配置
        }
        return getPresetSchedulerInterval(); // 使用策略配置
    }

    /**
     * 根据策略获取驱逐检查间隔
     */
    private long getPresetEvictionIntervalMs() {
        switch (preset) {
            case AGGRESSIVE:
                return 10000; // 10秒
            case BALANCED:
                return 30000; // 30秒
            case CONSERVATIVE:
                return 60000; // 60秒
            default:
                return 30000; // 默认30秒
        }
    }

    /**
     * 根据策略获取空闲对象可驱逐时间
     */
    private long getPresetEvictableIdleTimeMs() {
        switch (preset) {
            case AGGRESSIVE:
                return 30000; // 30秒
            case BALANCED:
                return 60000; // 60秒
            case CONSERVATIVE:
                return 120000; // 120秒
            default:
                return 60000; // 默认60秒
        }
    }

    /**
     * 根据策略获取定时任务调度间隔
     */
    private long getPresetSchedulerInterval() {
        switch (preset) {
            case AGGRESSIVE:
                return 120000; // 2分钟
            case BALANCED:
                return 300000; // 5分钟
            case CONSERVATIVE:
                return 600000; // 10分钟
            default:
                return 300000; // 默认5分钟
        }
    }

    /**
     * 获取配置说明
     */
    public String getConfigDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("清理配置说明：\n");
        desc.append("- 策略：").append(preset.getDescription()).append("\n");
        desc.append("- 驱逐检查间隔：").append(getFinalEvictionIntervalMs() / 1000).append("秒");
        if (evictionIntervalMs != null && evictionIntervalMs > 0) {
            desc.append(" (单独配置)");
        } else {
            desc.append(" (策略配置)");
        }
        desc.append("\n");

        desc.append("- 空闲可驱逐时间：").append(getFinalEvictableIdleTimeMs() / 1000).append("秒");
        if (evictableIdleTimeMs != null && evictableIdleTimeMs > 0) {
            desc.append(" (单独配置)");
        } else {
            desc.append(" (策略配置)");
        }
        desc.append("\n");

        desc.append("- 定时任务间隔：").append(getFinalSchedulerInterval() / 1000).append("秒");
        if (schedulerInterval != null && schedulerInterval > 0) {
            desc.append(" (单独配置)");
        } else {
            desc.append(" (策略配置)");
        }
        desc.append("\n");

        return desc.toString();
    }
}

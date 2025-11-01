package com.qiao.flow.orchestrator.core.dag.enums;

/**
 * 节点状态
 * 包含执行状态和结果状态
 */
public enum NodeState {
    PENDING,    // 等待执行
    RUNNING,    // 正在执行
    COMPLETED,  // 执行完成
    FAILED,     // 执行失败
    SKIP        // 跳过执行
} 
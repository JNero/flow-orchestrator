package com.qiao.flow.orchestrator.core.dag.callback;

/**
 * 引擎执行前后的回调接口
 */
@FunctionalInterface
public interface IDagCallback {
    void callback();
}
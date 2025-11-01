package com.qiao.flow.orchestrator.core.dag.callback;

import com.qiao.flow.orchestrator.core.dag.wrapper.NodeWrapper;

/**
 * 可调用接口
 */
@FunctionalInterface
public interface ICallable {

    /**
     * 调用回调（声明式API）
     *
     * @param wrapper 节点包装器
     */
    void call(NodeWrapper wrapper);
}

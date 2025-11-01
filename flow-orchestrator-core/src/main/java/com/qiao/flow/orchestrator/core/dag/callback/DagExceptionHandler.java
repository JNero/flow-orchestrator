package com.qiao.flow.orchestrator.core.dag.callback;

import com.qiao.flow.orchestrator.core.dag.context.DagContext;

/**
 * DAG异常处理器接口
 *
 * @author qiao
 */
@FunctionalInterface
public interface DagExceptionHandler<P> {

    /**
     * 处理DAG执行过程中的异常
     *
     * @param exception 异常信息
     * @param input     输入参数
     * @param context   DAG上下文
     */
    void handleException(Exception exception, P input, DagContext context);
} 
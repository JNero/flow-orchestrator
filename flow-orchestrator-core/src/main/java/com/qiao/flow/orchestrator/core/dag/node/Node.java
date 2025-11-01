package com.qiao.flow.orchestrator.core.dag.node;


import com.qiao.flow.orchestrator.core.dag.context.DagContext;

/**
 * Node接口
 * 使用显式上下文传递，避免ThreadLocal的内存泄漏风险
 */
@FunctionalInterface
public interface Node<P> {

    /**
     * 执行节点逻辑
     *
     * @param param   输入参数
     * @param context DAG上下文，包含节点结果、全局变量等
     * @param nodeId  当前节点ID，用于存储结果数据
     * @throws Exception 执行异常
     */
    void execute(P param, DagContext context, String nodeId) throws Exception;

    /**
     * Node执行前回调
     *
     * @param param   输入参数
     * @param context DAG上下文
     * @param nodeId  当前节点ID
     */
    default void onStart(P param, DagContext context, String nodeId) {
    }

    /**
     * Node执行成功后的回调
     * 执行结果可通过 context.getNodeResult(nodeId) 获取
     *
     * @param param   输入参数
     * @param context DAG上下文
     * @param nodeId  当前节点ID
     */
    default void onSuccess(P param, DagContext context, String nodeId) {
    }

    /**
     * Node执行异常后的回调
     * 错误结果可通过 context.getNodeResult(nodeId) 获取
     *
     * @param param   输入参数
     * @param context DAG上下文
     * @param nodeId  当前节点ID
     */
    default void onError(P param, DagContext context, String nodeId) {
    }
}

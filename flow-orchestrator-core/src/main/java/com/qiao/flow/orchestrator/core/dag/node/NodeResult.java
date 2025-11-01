package com.qiao.flow.orchestrator.core.dag.node;

import com.qiao.flow.orchestrator.core.dag.enums.NodeState;
import lombok.Getter;
import lombok.Setter;

/**
 * 节点执行结果
 * 参考taskflow项目的OperatorResult设计，支持完整的节点执行结果管理
 *
 * @author qiao
 */
@Getter
@Setter
public class NodeResult<V> {

    /**
     * 执行结果
     */
    private V result;

    /**
     * 结果状态
     */
    private NodeState state;

    /**
     * 异常信息
     */
    private Throwable exception;

    /**
     * 执行开始时间
     */
    private long startTime;

    /**
     * 执行结束时间
     */
    private long endTime;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    public NodeResult() {
        this.state = NodeState.PENDING;
        this.startTime = System.currentTimeMillis();
    }

    public NodeResult(V result, NodeState state) {
        this(result, state, null);
    }

    public NodeResult(V result, NodeState state, Throwable exception) {
        this.result = result;
        this.state = state;
        this.exception = exception;
        this.startTime = System.currentTimeMillis();
        this.endTime = System.currentTimeMillis();
        this.duration = 0;
    }

    /**
     * 创建默认结果
     */
    public static <V> NodeResult<V> defaultResult() {
        return new NodeResult<>(null, NodeState.SKIP);
    }

    /**
     * 创建成功结果
     */
    public static <V> NodeResult<V> successResult(V result) {
        return new NodeResult<>(result, NodeState.COMPLETED);
    }

    /**
     * 创建失败结果
     */
    public static <V> NodeResult<V> failedResult(Throwable exception) {
        return new NodeResult<>(null, NodeState.FAILED, exception);
    }

    /**
     * 完成执行
     */
    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.duration = this.endTime - this.startTime;
    }

    /**
     * 重置对象状态（用于对象池复用）
     */
    public void reset() {
        this.result = null;
        this.state = NodeState.PENDING;
        this.exception = null;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.duration = 0;
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return state == NodeState.COMPLETED;
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return state == NodeState.FAILED;
    }

    /**
     * 检查是否跳过
     */
    public boolean isSkipped() {
        return state == NodeState.SKIP;
    }

    /**
     * 检查是否完成（成功或失败）
     */
    public boolean isCompleted() {
        return state == NodeState.COMPLETED || state == NodeState.FAILED || state == NodeState.SKIP;
    }

    @Override
    public String toString() {
        return String.format("NodeResult{state=%s, result=%s, duration=%dms, exception=%s}",
                state, result, duration, exception != null ? exception.getMessage() : "null");
    }
}

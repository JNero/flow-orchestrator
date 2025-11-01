package com.qiao.flow.orchestrator.core.dag.wrapper;

import com.qiao.flow.orchestrator.core.dag.callback.IChoose;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.enums.NodeState;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeResult;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.core.dag.utils.NodeBeanNameUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 节点包装器
 * 支持完整的节点包装和生命周期管理
 *
 * @author qiao
 */
@Getter
@Slf4j
public class NodeWrapper<P, V> {

    // 节点基本信息
    private final String id;
    private final String name;
    private final String workflow;
    private final NodeType nodeType;
    private final Node<P> node;

    // 依赖关系
    private final Set<String> dependsOn;
    private final Set<String> weakDependsOn;
    private final Set<NodeWrapper<?, ?>> nextWrappers;
    private final Set<NodeWrapper<?, ?>> dependWrappers;

    // 特殊标记
    private final boolean isStartNode;
    private final boolean isEndNode;

    // 分支选择
    private final IChoose<P> chooser;


    // 执行状态
    private final AtomicReference<NodeState> state;
    private final AtomicInteger indegree;
    private final AtomicInteger weakIndegree; // 新增：弱依赖入度
    private volatile NodeResult<V> nodeResult;
    private volatile Thread executingThread;


    // 是否已初始化
    private volatile boolean initialized = false;

    public NodeWrapper(String id, String name, String workflow, NodeType nodeType, Node<P> node,
                       Set<String> dependsOn, Set<String> weakDependsOn,
                       boolean isStartNode, boolean isEndNode,
                       IChoose<P> chooser) {
        this.id = id;
        this.name = name;
        this.workflow = workflow;
        this.nodeType = nodeType;
        this.node = node;
        this.dependsOn = new HashSet<>(dependsOn);
        this.weakDependsOn = new HashSet<>(weakDependsOn);
        this.nextWrappers = new HashSet<>();
        this.dependWrappers = new HashSet<>();
        this.isStartNode = isStartNode;
        this.isEndNode = isEndNode;
        this.chooser = chooser;
        this.state = new AtomicReference<>(NodeState.PENDING);
        this.indegree = new AtomicInteger(dependsOn.size());
        this.weakIndegree = new AtomicInteger(weakDependsOn.size()); // 初始化弱依赖入度
        this.nodeResult = NodeResult.defaultResult();
    }

    /**
     * 简化构造函数
     */
    public NodeWrapper(String name, Node<P> node) {
        this(name, name, "default", NodeType.CPU, node,
                new HashSet<>(), new HashSet<>(), false, false, null);
    }

    /**
     * 执行节点生命周期
     * 重构后：完全基于状态的结果处理，不向上抛出异常
     */
    public NodeResult<V> execute(P input, DagContext context) {
        // 设置执行线程
        this.executingThread = Thread.currentThread();

        // 设置当前节点ID到DagContext
        context.setCurrentNodeId(this.id);

        // 获取或创建DagContext中的NodeResult
        NodeResult<V> result = getOrCreateNodeResult(context);

        try {
            // 设置状态为运行中
            setState(NodeState.RUNNING);

            // 1. 前置回调
            node.onStart(input, context, id);

            // 2. 核心执行逻辑
            node.execute(input, context, id);

            // 3. 成功回调
            node.onSuccess(input, context, id);

            // 4. 设置成功状态并完成
            result.setState(NodeState.COMPLETED);
            result.complete();

            // 5. 保存结果到NodeWrapper
            this.nodeResult = result;

            return result;

        } catch (Exception e) {
            // 6. 异常处理：设置失败状态，不向上抛出
            log.info("NodeWrapper.execute() caught exception: {}, setting failed state", e.getMessage());
            result.setState(NodeState.FAILED);
            result.setException(e);
            result.complete();

            // 7. 调用错误回调
            node.onError(input, context, id);

            // 8. 保存错误结果到NodeWrapper
            this.nodeResult = result;

            // 9. 返回错误结果，不抛出异常
            log.info("NodeWrapper.execute() returned failed result, state: {}", result.getState());
            return result;
        } finally {
            // 清理执行线程和当前节点ID
            this.executingThread = null;
            context.setCurrentNodeId(null);
        }
    }

    /**
     * 获取或创建DagContext中的NodeResult
     */
    @SuppressWarnings("unchecked")
    private NodeResult<V> getOrCreateNodeResult(DagContext context) {
        NodeResult<?> existingResult = context.getResult(this.id);
        if (existingResult != null) {
            return (NodeResult<V>) existingResult;
        }

        // 如果DagContext中没有，创建一个新的
        NodeResult<V> newResult = new NodeResult<>();
        context.putResult(this.id, newResult);
        return newResult;
    }


    /**
     * 检查节点是否可以执行
     * 支持条件判断和依赖检查
     */
    public boolean canExecute(Set<String> completedNodes) {
        // 检查强依赖入度
        if (indegree.get() > 0) {
            return false;
        }

        // 检查弱依赖入度
        if (weakIndegree.get() > 0) {
            return false;
        }

        return true;
    }

    /**
     * 分支选择
     */
    public Set<String> chooseNext(P input, DagContext context) {
        if (chooser != null) {
            // 使用Class版本的分支选择
            Set<Class<? extends Node<?>>> classSelection = chooser.chooseNext(input, context);
            // 将Class转换为BeanName
            return classSelection.stream()
                    .map(this::getBeanNameByClass)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 根据Class获取BeanName
     */
    private String getBeanNameByClass(Class<? extends Node<?>> nodeClass) {
        return NodeBeanNameUtils.getBeanNameByClass(nodeClass);
    }


    /**
     * 设置状态
     */
    public void setState(NodeState state) {
        this.state.set(state);
    }

    /**
     * 获取状态
     */
    public NodeState getState() {
        return state.get();
    }

    /**
     * 减少强依赖入度
     */
    public void decrementIndegree() {
        indegree.decrementAndGet();
    }

    /**
     * 减少弱依赖入度
     */
    public void decrementWeakIndegree() {
        weakIndegree.decrementAndGet();
    }

    /**
     * 获取强依赖入度
     */
    public int getIndegree() {
        return indegree.get();
    }

    /**
     * 获取弱依赖入度
     */
    public int getWeakIndegree() {
        return weakIndegree.get();
    }

    /**
     * 检查是否为IO节点
     */
    public boolean isIoNode() {
        return nodeType == NodeType.IO;
    }

    /**
     * 检查是否为CPU节点
     */
    public boolean isCpuNode() {
        return nodeType == NodeType.CPU;
    }

    /**
     * 检查是否可以并行执行
     */
    public boolean canParallel() {
        return isIoNode() || (isCpuNode() && !nextWrappers.isEmpty());
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 标记为已初始化
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * 获取执行线程
     */
    public Thread getExecutingThread() {
        return executingThread;
    }


    @Override
    public String toString() {
        return String.format("NodeWrapper{id='%s', name='%s', type=%s, state=%s, indegree=%d, weakIndegree=%d}",
                id, name, nodeType, state.get(), indegree.get(), weakIndegree.get());
    }
}


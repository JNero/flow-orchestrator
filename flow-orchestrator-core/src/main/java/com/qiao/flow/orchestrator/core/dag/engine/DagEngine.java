package com.qiao.flow.orchestrator.core.dag.engine;

import com.qiao.flow.orchestrator.core.dag.callback.ICallable;
import com.qiao.flow.orchestrator.core.dag.callback.IDagCallback;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.enums.DagState;
import com.qiao.flow.orchestrator.core.dag.enums.NodeState;
import com.qiao.flow.orchestrator.core.dag.node.NodeResult;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.core.dag.pool.CollectionPool;
import com.qiao.flow.orchestrator.core.dag.thread.pool.MixedThreadPoolManager;
import com.qiao.flow.orchestrator.core.dag.wrapper.NodeWrapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DAG执行引擎
 *
 * @author qiao
 */
@Slf4j
public class DagEngine<T> {

    // 缓存空集合，避免重复创建
    private static final Set<String> EMPTY_SET = Collections.emptySet();

    // 不可变配置 - 可安全共享
    private final DagEngineConfig config;

    // 本地缓存 - 减少配置访问开销
    private final Map<String, NodeWrapper<?, ?>> localNodeMap;
    private final Map<String, Set<String>> localDependencies;
    private final Map<String, Set<String>> localWeakDependencies;
    private final MixedThreadPoolManager localThreadPoolManager;
    private final long localTimeout;
    private final Set<String> localEndNodes;

    // 位图状态管理器
    private final BitMapStateManager stateManager;
    private final AtomicReference<DagState> dagState;
    private final AtomicInteger failedNodesCount;

    // 活跃节点管理
    private final ConcurrentHashMap<String, Boolean> activeNodes;

    // 回调
    @Setter
    private IDagCallback beforeCallback;
    @Setter
    private IDagCallback afterCallback;
    @Setter
    private ICallable beforeNodeCallback;
    @Setter
    private ICallable afterNodeCallback;
    @Setter
    private boolean enableCallbacks = true;

    private Throwable ex;

    // DagContext管理
    private DagContext dagContext;
    private T businessContext;

    public DagEngine(Map<String, NodeWrapper<?, ?>> nodeMap,
                     Map<String, Set<String>> dependencies,
                     Map<String, Set<String>> weakDependencies,
                     MixedThreadPoolManager threadPoolManager,
                     long timeout) {
        // 初始化开始和结束节点
        String startNode = null;
        Set<String> endNodes = new HashSet<>();

        for (Map.Entry<String, NodeWrapper<?, ?>> entry : nodeMap.entrySet()) {
            NodeWrapper<?, ?> wrapper = entry.getValue();
            if (wrapper.isStartNode()) {
                startNode = entry.getKey();
            }
            if (wrapper.isEndNode()) {
                endNodes.add(entry.getKey());
            }
        }

        // 创建配置
        this.config = new DagEngineConfig(nodeMap, dependencies, weakDependencies,
                threadPoolManager, timeout, startNode, endNodes);


        // 创建本地缓存，减少配置访问开销 - 预分配容量避免扩容
        this.localNodeMap = new HashMap<>(config.getNodeMap().size());
        this.localNodeMap.putAll(config.getNodeMap());
        this.localDependencies = new HashMap<>(config.getDependencies().size());
        this.localDependencies.putAll(config.getDependencies());
        this.localWeakDependencies = new HashMap<>(config.getWeakDependencies().size());
        this.localWeakDependencies.putAll(config.getWeakDependencies());
        this.localThreadPoolManager = config.getThreadPoolManager();
        this.localTimeout = config.getTimeout();
        this.localEndNodes = new HashSet<>(config.getEndNodes().size());
        this.localEndNodes.addAll(config.getEndNodes());

        // 初始化位图状态管理器
        this.stateManager = new BitMapStateManager(nodeMap);
        this.dagState = new AtomicReference<>(DagState.INIT);
        this.failedNodesCount = new AtomicInteger(0);

        // 初始化活跃节点管理
        this.activeNodes = new ConcurrentHashMap<>(nodeMap.size());
        initializeActiveNodes();
    }

    // 新的构造函数，接受配置对象
    public DagEngine(DagEngineConfig config) {
        this.config = config;


        // 创建本地缓存，减少配置访问开销 - 预分配容量避免扩容
        this.localNodeMap = new HashMap<>(config.getNodeMap().size());
        this.localNodeMap.putAll(config.getNodeMap());
        this.localDependencies = new HashMap<>(config.getDependencies().size());
        this.localDependencies.putAll(config.getDependencies());
        this.localWeakDependencies = new HashMap<>(config.getWeakDependencies().size());
        this.localWeakDependencies.putAll(config.getWeakDependencies());
        this.localThreadPoolManager = config.getThreadPoolManager();
        this.localTimeout = config.getTimeout();
        this.localEndNodes = new HashSet<>(config.getEndNodes().size());
        this.localEndNodes.addAll(config.getEndNodes());

        // 初始化位图状态管理器
        this.stateManager = new BitMapStateManager(config.getNodeMap());
        this.dagState = new AtomicReference<>(DagState.INIT);
        this.failedNodesCount = new AtomicInteger(0);

        // 初始化活跃节点管理
        this.activeNodes = new ConcurrentHashMap<>(config.getNodeMap().size());
        initializeActiveNodes();
    }

    /**
     * 初始化活跃节点集合
     */
    private void initializeActiveNodes() {
        // 初始化时，所有节点都是活跃的
        for (String nodeId : localNodeMap.keySet()) {
            activeNodes.put(nodeId, true);
        }
    }

    /**
     * 获取活跃节点集合
     */
    private Set<String> getActiveNodes() {
        // 返回副本，避免外部修改
        return new HashSet<>(activeNodes.keySet());
    }

    /**
     * 从活跃节点中移除节点
     */
    private void removeFromActiveNodes(String nodeId) {
        activeNodes.remove(nodeId);
    }

    /**
     * 处理节点状态变化
     */
    private void onNodeStateChanged(String nodeId, NodeState newState) {

        // 这些状态的节点都应该从活跃节点中移除
        if (newState == NodeState.COMPLETED ||      // 已完成
                newState == NodeState.SKIP ||           // 被跳过
                newState == NodeState.FAILED ||         // 执行失败
                stateManager.isPruned(nodeId)) {        // 被剪枝

            // 从活跃节点中移除
            removeFromActiveNodes(nodeId);
        }
    }

    /**
     * 执行DAG
     */
    public void execute(T input, DagContext dagContext) {
        this.dagContext = dagContext;
        this.businessContext = input;

        long dagStartTime = System.currentTimeMillis();

        dagState.set(DagState.RUNNING);

        // 执行前回调
        if (enableCallbacks && beforeCallback != null) {
            try {
                beforeCallback.callback();
            } catch (Exception callbackException) {
                // 如果beforeCallback执行失败，设置DAG状态为错误
                dagState.set(DagState.ERROR);
                this.ex = callbackException;
                log.warn("Before callback execution failed", callbackException);
                return; // 提前返回，不执行后续逻辑
            }
        }

        // 主执行循环
        while (failedNodesCount.get() == 0 && dagState.get() != DagState.ERROR) {
            // 检查超时
            if (System.currentTimeMillis() - dagStartTime > localTimeout) {
                log.warn("DAG execution timeout, timeout: {}ms", localTimeout);
                dagState.set(DagState.ERROR);
                this.ex = new RuntimeException("DAG execution timeout");
                return;
            }

            // 找到所有可并发执行的节点
            Set<String> executableNodes = findExecutableNodes();

            if (executableNodes.isEmpty()) {
                // 没有可执行的节点，检查是否完成
                if (isDagCompleted()) {
                    break;
                } else {
                    log.warn("No executable nodes found, but DAG not completed, potential deadlock");
                    break;
                }
            }

            // 根据节点数量决定执行方式
            if (executableNodes.size() == 1) {
                // 单个节点直接在当前线程执行
                String nextNode = executableNodes.iterator().next();
                executeNode(nextNode, input);

                // 检查节点执行后是否出错
                if (dagState.get() == DagState.ERROR) {
                    break;
                }
            } else {
                // 多个节点并发执行
                executeNodesConcurrently(executableNodes, input);

                // 检查并发执行后是否出错
                if (dagState.get() == DagState.ERROR) {
                    break;
                }
            }
        }

        // 无论成功还是失败，都要执行后回调
        if (enableCallbacks && afterCallback != null) {
            try {
                afterCallback.callback();
            } catch (Exception callbackException) {
                // 如果afterCallback执行失败，设置DAG状态为错误
                dagState.set(DagState.ERROR);
                this.ex = callbackException;
                log.warn("After callback execution failed", callbackException);
            }
        }

        // 检查最终状态，只有在没有错误的情况下才设置为成功
        if (dagState.get() != DagState.ERROR) {
            dagState.set(DagState.FINISH);
            // DAG执行完成计时
            long dagExecutionTime = System.currentTimeMillis() - dagStartTime;
            log.info("DAG execution completed successfully, execution time: {}ms", dagExecutionTime);
        } else {
            // 有错误的情况下，记录错误信息
            long dagExecutionTime = System.currentTimeMillis() - dagStartTime;
            log.warn("DAG execution failed, execution time: {}ms", dagExecutionTime);
        }

        // 确保清理引用，让GC能回收
        this.businessContext = null;
        this.dagContext = null;
    }

    /**
     * 找到所有可并发执行的节点
     */
    private Set<String> findExecutableNodes() {
        Set<String> executableNodes = CollectionPool.borrowSet();
        try {
            for (String nodeId : localNodeMap.keySet()) {
                // 跳过已完成的节点
                if (stateManager.isCompleted(nodeId) || stateManager.isSkipped(nodeId) || stateManager.isFailed(nodeId)) {
                    continue;
                }

                // 跳过被剪枝的节点
                if (stateManager.isPruned(nodeId)) {
                    continue;
                }

                // 检查是否可以执行
                if (canExecute(nodeId)) {
                    executableNodes.add(nodeId);
                }
            }

            // 使用对象池创建副本，避免直接创建新集合
            Set<String> result = CollectionPool.borrowSet();
            try {
                result.addAll(executableNodes);
                return new HashSet<>(result); // 返回副本
            } finally {
                CollectionPool.releaseSet(result);
            }
        } finally {
            CollectionPool.releaseSet(executableNodes);
        }
    }

    /**
     * 检查节点是否可以执行
     */
    private boolean canExecute(String nodeId) {
        // 检查强依赖 - 使用缓存的空集合，避免创建临时对象
        Set<String> deps = localDependencies.getOrDefault(nodeId, EMPTY_SET);

        for (String dep : deps) {
            // 优化检查顺序：先检查最可能失败的条件
            if (stateManager.isPruned(dep)) {
                continue; // 跳过被剪枝的依赖
            }
            if (!stateManager.isCompleted(dep)) {
                return false; // 快速失败
            }
        }

        // 检查弱依赖 - 使用缓存的空集合，避免创建临时对象
        Set<String> weakDeps = localWeakDependencies.getOrDefault(nodeId, EMPTY_SET);

        if (!weakDeps.isEmpty()) {
            boolean hasWeakDependency = false;
            for (String dep : weakDeps) {
                // 优化检查顺序：先检查最可能失败的条件
                if (stateManager.isPruned(dep)) {
                    continue; // 跳过被剪枝的依赖
                }
                if (stateManager.isCompleted(dep)) {
                    hasWeakDependency = true;
                    break; // 找到第一个完成的依赖就退出
                }
            }
            if (!hasWeakDependency) {
                return false;
            }
        }

        return true;
    }

    /**
     * 并发执行多个节点
     * 重构后：正确处理节点失败，不忽略异常
     */
    private void executeNodesConcurrently(Set<String> nodeIds, T input) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(nodeIds.size()); // 预分配容量

        for (String nodeId : nodeIds) {
            NodeWrapper<?, ?> wrapper = localNodeMap.get(nodeId);
            if (wrapper == null) {
                continue;
            }

            // 根据节点类型选择执行方式
            CompletableFuture<Void> future;
            if (wrapper.getNodeType() == NodeType.IO) {
                // IO密集型节点使用虚拟线程池
                future = CompletableFuture.runAsync(() -> executeNode(nodeId, input), localThreadPoolManager.getIoThreadPool());
            } else {
                // CPU密集型节点使用CPU线程池
                future = CompletableFuture.runAsync(() -> executeNode(nodeId, input), localThreadPoolManager.getCpuThreadPool());
            }

            futures.add(future);
        }

        // 等待所有节点完成，检查是否有失败的节点
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
        } catch (Exception e) {
            // 如果有节点失败，记录错误但不抛出异常
            // 因为 executeNode 已经处理了节点失败的情况
            log.warn("Some nodes in concurrent execution failed, but errors have been handled", e);
        }
    }

    /**
     * 执行单个节点
     */
    private void executeNode(String nodeId, T input) {
        NodeWrapper<?, ?> wrapper = localNodeMap.get(nodeId);
        if (wrapper == null) {
            return;
        }


        // 记录节点开始时间
        long nodeStartTime = System.currentTimeMillis();

        // 节点执行前回调
        if (enableCallbacks && beforeNodeCallback != null) {
            beforeNodeCallback.call(wrapper);
        }

        try {
            NodeWrapper<T, ?> typedWrapper = (NodeWrapper<T, ?>) wrapper;
            NodeResult<?> result = typedWrapper.execute(input, dagContext);

            // 检查执行结果状态
            if (result.getState() == NodeState.FAILED) {
                // 节点执行失败，设置异常状态
                Throwable exception = result.getException();
                this.ex = exception;
                dagState.set(DagState.ERROR);
                log.warn("Node execution failed: {}, exception: {}", nodeId, exception.getMessage());
            } else {
                // 节点执行成功，完成节点
                completeNode(nodeId, nodeStartTime);
            }
        } catch (Throwable throwable) {
            // 节点执行异常，设置异常状态
            this.ex = throwable;
            dagState.set(DagState.ERROR);
            log.warn("Node execution exception: {}, exception: {}", nodeId, throwable.getMessage());
        }
    }

    /**
     * 完成节点执行
     */
    private void completeNode(String nodeId, long nodeStartTime) {
        // 原子性地标记节点为已完成
        if (stateManager.isCompleted(nodeId)) {
            return;
        }

        // 标记节点为完成状态
        stateManager.markCompleted(nodeId);

        // 通知状态变化
        onNodeStateChanged(nodeId, NodeState.COMPLETED);

        // 节点执行后回调
        if (enableCallbacks && afterNodeCallback != null) {
            NodeWrapper<?, ?> wrapper = localNodeMap.get(nodeId);
            afterNodeCallback.call(wrapper);
        }

        // 处理分支选择
        handleBranchSelection(nodeId);

        // 计算节点执行时间
        long nodeExecutionTime = System.currentTimeMillis() - nodeStartTime;
        log.info("Node {} completed in {}ms", nodeId, nodeExecutionTime);
    }


    /**
     * 处理分支选择
     */
    private void handleBranchSelection(String nodeId) {

        NodeWrapper<?, ?> wrapper = localNodeMap.get(nodeId);
        if (wrapper == null || wrapper.getChooser() == null) {
            return;
        }

        NodeWrapper<T, ?> typedWrapper = (NodeWrapper<T, ?>) wrapper;

        // 确保上下文不为null
        if (dagContext == null || businessContext == null) {
            return;
        }

        try {
            // 执行分支选择
            Set<String> branchSelection = typedWrapper.chooseNext(businessContext, dagContext);

            if (branchSelection == null || branchSelection.isEmpty()) {
                return;
            }

            // 原子性地更新选择状态
            for (String selectedNodeId : branchSelection) {
                stateManager.markSelected(selectedNodeId);
            }

            // 递归剪枝整个分支
            Set<String> unreachableNodes = findUnreachableNodes(branchSelection);

            for (String node : unreachableNodes) {
                stateManager.markPruned(node);
                // 通知状态变化
                onNodeStateChanged(node, NodeState.SKIP); // 使用SKIP状态表示被剪枝
            }

        } catch (Exception e) {
            log.warn("Branch selection failed for node {}", nodeId, e);
        }
    }

    /**
     * 查找不可达节点
     */
    private Set<String> findUnreachableNodes(Set<String> selectedNodes) {

        // 计算可达节点集合
        Set<String> reachableNodes = CollectionPool.borrowSet();
        Set<String> unreachableNodes = CollectionPool.borrowSet();

        try {
            reachableNodes.addAll(selectedNodes);

            Queue<String> queue = new LinkedList<>(selectedNodes);

            while (!queue.isEmpty()) {
                String current = queue.poll();

                Set<String> successors = getAllPossibleSuccessors(current);

                for (String successor : successors) {
                    if (!reachableNodes.contains(successor)) {
                        reachableNodes.add(successor);
                        queue.offer(successor);
                    }
                }
            }

            // 添加结束节点到可达集合（结束节点总是可达的）
            reachableNodes.addAll(localEndNodes);

            // 关键优化：只从活跃节点中计算不可达节点
            Set<String> activeNodes = getActiveNodes();

            unreachableNodes.addAll(activeNodes);       // 只添加活跃节点
            unreachableNodes.removeAll(reachableNodes); // 减去可达节点

            // 使用对象池创建副本，避免直接创建新集合
            Set<String> result = CollectionPool.borrowSet();
            try {
                result.addAll(unreachableNodes);
                return new HashSet<>(result); // 返回副本
            } finally {
                CollectionPool.releaseSet(result);
            }
        } finally {
            CollectionPool.releaseSet(reachableNodes);
            CollectionPool.releaseSet(unreachableNodes);
        }
    }

    /**
     * 获取节点的所有可能后继节点
     */
    private Set<String> getAllPossibleSuccessors(String nodeId) {
        Set<String> successors = CollectionPool.borrowSet();
        try {
            // 检查强依赖
            for (Map.Entry<String, Set<String>> entry : localDependencies.entrySet()) {
                if (entry.getValue().contains(nodeId)) {
                    successors.add(entry.getKey());
                }
            }

            // 检查弱依赖
            for (Map.Entry<String, Set<String>> entry : localWeakDependencies.entrySet()) {
                if (entry.getValue().contains(nodeId)) {
                    successors.add(entry.getKey());
                }
            }

            // 使用对象池创建副本，避免直接创建新集合
            Set<String> result = CollectionPool.borrowSet();
            try {
                result.addAll(successors);
                return new HashSet<>(result); // 返回副本
            } finally {
                CollectionPool.releaseSet(result);
            }
        } finally {
            CollectionPool.releaseSet(successors);
        }
    }

    /**
     * 检查DAG是否完成
     */
    private boolean isDagCompleted() {
        for (String endNode : localEndNodes) {
            if (!stateManager.isCompleted(endNode) && !stateManager.isSkipped(endNode)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 获取配置对象
     */
    public DagEngineConfig getConfig() {
        return config;
    }


    // 保留现有的getter和setter方法
    public DagState getDagState() {
        return dagState.get();
    }

    public Map<String, NodeState> getNodeStates() {
        return new HashMap<>(); // 状态管理简化，不再维护nodeStates
    }

    /**
     * 克隆引擎（用于原型模式）
     */
    public DagEngine<T> clone() {
        // 只创建新的引擎实例，复用配置
        DagEngine<T> cloned = new DagEngine<>(this.config);

        // 复制回调配置
        cloned.beforeCallback = this.beforeCallback;
        cloned.afterCallback = this.afterCallback;
        cloned.beforeNodeCallback = this.beforeNodeCallback;
        cloned.afterNodeCallback = this.afterNodeCallback;
        cloned.enableCallbacks = this.enableCallbacks;

        return cloned;
    }


    /**
     * 获取执行异常
     */
    public Throwable getEx() {
        return ex;
    }
}



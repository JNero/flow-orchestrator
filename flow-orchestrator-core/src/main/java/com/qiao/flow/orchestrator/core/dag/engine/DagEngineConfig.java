package com.qiao.flow.orchestrator.core.dag.engine;

import com.qiao.flow.orchestrator.core.dag.thread.pool.MixedThreadPoolManager;
import com.qiao.flow.orchestrator.core.dag.wrapper.NodeWrapper;

import java.util.Map;
import java.util.Set;

/**
 * DAG引擎配置类 - 不可变配置，可安全共享
 * 只存储配置信息，不存储执行状态
 *
 * @author qiao
 */
public class DagEngineConfig {

    // 不可变配置
    private final Map<String, NodeWrapper<?, ?>> nodeMap;
    private final Map<String, Set<String>> dependencies;
    private final Map<String, Set<String>> weakDependencies;
    private final MixedThreadPoolManager threadPoolManager;
    private final long timeout;
    private final String startNode;
    private final Set<String> endNodes;

    public DagEngineConfig(Map<String, NodeWrapper<?, ?>> nodeMap,
                           Map<String, Set<String>> dependencies,
                           Map<String, Set<String>> weakDependencies,
                           MixedThreadPoolManager threadPoolManager,
                           long timeout,
                           String startNode,
                           Set<String> endNodes) {
        this.nodeMap = nodeMap;
        this.dependencies = dependencies;
        this.weakDependencies = weakDependencies;
        this.threadPoolManager = threadPoolManager;
        this.timeout = timeout;
        this.startNode = startNode;
        this.endNodes = endNodes;
    }

    // 从DagEngine创建配置（用于缓存）
    public DagEngineConfig(DagEngine<?> engine) {
        this.nodeMap = engine.getConfig().getNodeMap();
        this.dependencies = engine.getConfig().getDependencies();
        this.weakDependencies = engine.getConfig().getWeakDependencies();
        this.threadPoolManager = engine.getConfig().getThreadPoolManager();
        this.timeout = engine.getConfig().getTimeout();
        this.startNode = engine.getConfig().getStartNode();
        this.endNodes = engine.getConfig().getEndNodes();
    }


    // Getter方法
    public Map<String, NodeWrapper<?, ?>> getNodeMap() {
        return nodeMap;
    }

    public Map<String, Set<String>> getDependencies() {
        return dependencies;
    }

    public Map<String, Set<String>> getWeakDependencies() {
        return weakDependencies;
    }

    public MixedThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getStartNode() {
        return startNode;
    }

    public Set<String> getEndNodes() {
        return endNodes;
    }


    // 创建新的DagEngine实例
    public <T> DagEngine<T> createEngine() {
        return new DagEngine<>(this);
    }
}

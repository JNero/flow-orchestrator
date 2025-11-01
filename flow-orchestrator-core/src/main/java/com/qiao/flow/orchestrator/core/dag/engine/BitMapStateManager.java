package com.qiao.flow.orchestrator.core.dag.engine;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 位图状态管理器
 * 使用BitSet替代HashSet管理节点状态，减少内存使用和CPU开销
 *
 * @author qiao
 */
@Slf4j
public class BitMapStateManager {

    // 位图状态存储
    private final BitSet completedNodes;
    private final BitSet failedNodes;
    private final BitSet skippedNodes;
    private final BitSet prunedNodes;
    private final BitSet selectedNodes;

    // 节点ID到索引的映射
    private final Map<String, Integer> nodeIdToIndex;
    private final Map<Integer, String> indexToNodeId;

    // 节点总数
    private final int nodeCount;

    public BitMapStateManager(Map<String, ?> nodeMap) {
        this.nodeCount = nodeMap.size();

        // 初始化位图
        this.completedNodes = new BitSet(nodeCount);
        this.failedNodes = new BitSet(nodeCount);
        this.skippedNodes = new BitSet(nodeCount);
        this.prunedNodes = new BitSet(nodeCount);
        this.selectedNodes = new BitSet(nodeCount);

        // 初始化映射
        this.nodeIdToIndex = new HashMap<>(nodeCount);
        this.indexToNodeId = new HashMap<>(nodeCount);

        // 建立节点ID到索引的映射
        int index = 0;
        for (String nodeId : nodeMap.keySet()) {
            nodeIdToIndex.put(nodeId, index);
            indexToNodeId.put(index, nodeId);
            index++;
        }

    }

    /**
     * 检查节点是否完成
     */
    public boolean isCompleted(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        return index != null && completedNodes.get(index);
    }

    /**
     * 检查节点是否失败
     */
    public boolean isFailed(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        return index != null && failedNodes.get(index);
    }

    /**
     * 检查节点是否跳过
     */
    public boolean isSkipped(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        return index != null && skippedNodes.get(index);
    }

    /**
     * 检查节点是否被剪枝
     */
    public boolean isPruned(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        return index != null && prunedNodes.get(index);
    }

    /**
     * 检查节点是否被选择
     */
    public boolean isSelected(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        return index != null && selectedNodes.get(index);
    }

    /**
     * 标记节点为完成
     */
    public void markCompleted(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        if (index != null) {
            completedNodes.set(index);
        }
    }

    /**
     * 标记节点为失败
     */
    public void markFailed(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        if (index != null) {
            failedNodes.set(index);
        }
    }

    /**
     * 标记节点为跳过
     */
    public void markSkipped(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        if (index != null) {
            skippedNodes.set(index);
        }
    }

    /**
     * 标记节点为剪枝
     */
    public void markPruned(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        if (index != null) {
            prunedNodes.set(index);
        }
    }

    /**
     * 标记节点为选择
     */
    public void markSelected(String nodeId) {
        Integer index = nodeIdToIndex.get(nodeId);
        if (index != null) {
            selectedNodes.set(index);
        }
    }

    /**
     * 获取所有完成的节点
     */
    public Set<String> getCompletedNodes() {
        return getNodesFromBitSet(completedNodes);
    }

    /**
     * 获取所有失败的节点
     */
    public Set<String> getFailedNodes() {
        return getNodesFromBitSet(failedNodes);
    }

    /**
     * 获取所有跳过的节点
     */
    public Set<String> getSkippedNodes() {
        return getNodesFromBitSet(skippedNodes);
    }

    /**
     * 获取所有被剪枝的节点
     */
    public Set<String> getPrunedNodes() {
        return getNodesFromBitSet(prunedNodes);
    }

    /**
     * 获取所有被选择的节点
     */
    public Set<String> getSelectedNodes() {
        return getNodesFromBitSet(selectedNodes);
    }

    /**
     * 从位图获取节点集合
     */
    private Set<String> getNodesFromBitSet(BitSet bitSet) {
        Set<String> nodes = new HashSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            String nodeId = indexToNodeId.get(i);
            if (nodeId != null) {
                nodes.add(nodeId);
            }
        }
        return nodes;
    }

    /**
     * 获取节点总数
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * 强制清理状态管理器
     * 用于解决Old Gen GC问题
     */
    public void cleanup() {
        completedNodes.clear();
        failedNodes.clear();
        skippedNodes.clear();
        prunedNodes.clear();
        selectedNodes.clear();
        nodeIdToIndex.clear();
        indexToNodeId.clear();

    }

}

package com.qiao.flow.orchestrator.core.dag.context;

import com.qiao.flow.orchestrator.core.dag.enums.NodeState;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeResult;
import com.qiao.flow.orchestrator.core.dag.utils.NodeBeanNameUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG执行上下文
 *
 * @author qiao
 */
@Slf4j
public class DagContext {


    /**
     * 保存每个节点的补充信息
     */
    private Map<String /* nodeId */, NodeResult<?>> nodeResultMap = new ConcurrentHashMap<>();

    /**
     * 通用数据存储，支持节点间数据共享
     */
    private Map<String /* key */, Object> dataMap = new ConcurrentHashMap<>();

    /**
     * 当前执行的节点ID
     */
    private volatile String currentNodeId;

    /**
     * 时间跟踪字段
     */
    private volatile long borrowTime = 0L; // 借用时间
    private volatile long returnTime = 0L;  // 归还时间

    public DagContext() {
    }

    /**
     * 存储节点补充信息
     */
    public void putResult(String nodeId, NodeResult<?> nodeResult) {
        nodeResultMap.put(nodeId, nodeResult);
    }

    /**
     * 获取节点补充信息
     */
    public NodeResult getResult(String nodeId) {
        return nodeResultMap.get(nodeId);
    }

    /**
     * 存储通用数据
     */
    public void putData(String key, Object value) {
        dataMap.put(key, value);
    }

    /**
     * 获取通用数据
     */
    public Object getData(String key) {
        return dataMap.get(key);
    }

    /**
     * 检查是否包含指定数据
     */
    public boolean containsData(String key) {
        return dataMap.containsKey(key);
    }

    /**
     * 移除指定数据
     */
    public Object removeData(String key) {
        return dataMap.remove(key);
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        nodeResultMap.clear();
        dataMap.clear();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return nodeResultMap.isEmpty() && dataMap.isEmpty();
    }

    /**
     * 设置借用时间
     */
    public void setBorrowTime(long borrowTime) {
        this.borrowTime = borrowTime;
    }

    /**
     * 设置归还时间
     */
    public void setReturnTime(long returnTime) {
        this.returnTime = returnTime;
    }

    /**
     * 更新节点状态
     */
    public void updateNodeState(String nodeId, NodeState newState) {
        NodeResult<?> nodeResult = nodeResultMap.get(nodeId);
        if (nodeResult != null) {
            nodeResult.setState(newState);
        }
    }

    /**
     * 更新节点结束时间
     */
    public void updateNodeEndTime(String nodeId, long endTime) {
        NodeResult<?> nodeResult = nodeResultMap.get(nodeId);
        if (nodeResult != null) {
            nodeResult.setEndTime(endTime);
            nodeResult.complete(); // 自动计算执行时长
        }
    }

    /**
     * 计算空闲时间（毫秒）
     * 如果对象还在使用中，返回0
     */
    public long getIdleTime() {
        if (returnTime == 0L) {
            return 0L; // 对象还在使用中
        }
        return System.currentTimeMillis() - returnTime;
    }

    /**
     * 获取借用时间
     */
    public long getBorrowTime() {
        return borrowTime;
    }

    /**
     * 获取归还时间
     */
    public long getReturnTime() {
        return returnTime;
    }


    /**
     * 设置当前节点ID
     */
    public void setCurrentNodeId(String nodeId) {
        this.currentNodeId = nodeId;
    }

    /**
     * 获取当前节点ID
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }


    /**
     * 设置节点结果数据（显式传参版本）
     *
     * @param nodeId 节点ID
     * @param data   业务数据
     * @param <T>    业务数据类型
     * @return 是否设置成功
     */
    public <T> boolean putNodeResult(String nodeId, T data) {
        if (nodeId == null) {
            return false;
        }

        // 创建NodeResult包装器
        NodeResult<T> nodeResult = new NodeResult<>();
        nodeResult.setResult(data);
        nodeResultMap.put(nodeId, nodeResult);
        return true;
    }

    /**
     * 根据节点ID获取结果数据
     *
     * @param nodeId 节点ID
     * @param <T>    结果数据类型
     * @return 业务数据，如果不存在则返回null
     */
    public <T> T getNodeResult(String nodeId) {
        NodeResult<?> nodeResult = nodeResultMap.get(nodeId);
        return nodeResult != null ? (T) nodeResult.getResult() : null;
    }

    /**
     * 根据Node类获取结果数据
     *
     * @param nodeClass Node类
     * @param <T>       结果数据类型
     * @return 业务数据，如果不存在则返回null
     */
    public <T> T getNodeResult(Class<? extends Node<?>> nodeClass) {
        String beanName = NodeBeanNameUtils.getBeanNameByClass(nodeClass);
        return getNodeResult(beanName);
    }


}
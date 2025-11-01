package com.qiao.flow.orchestrator.core.dag.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DAG算法工具类
 * 包含循环检测算法
 *
 * @author qiao
 */
@Slf4j
public class DagAlgorithmUtils {


    /**
     * 检查循环依赖
     *
     * @param nodeMap      节点映射
     * @param dependencies 依赖关系映射
     * @return 是否存在循环依赖
     */
    public static boolean hasCycle(Map<String, ?> nodeMap, Map<String, Set<String>> dependencies) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String nodeId : nodeMap.keySet()) {
            if (!visited.contains(nodeId)) {
                if (hasCycleUtil(nodeId, visited, recStack, dependencies)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查循环依赖工具方法
     */
    private static boolean hasCycleUtil(String nodeId, Set<String> visited, Set<String> recStack,
                                        Map<String, Set<String>> dependencies) {
        visited.add(nodeId);
        recStack.add(nodeId);

        Set<String> dependents = getDependents(nodeId, dependencies);
        for (String dependent : dependents) {
            if (!visited.contains(dependent)) {
                if (hasCycleUtil(dependent, visited, recStack, dependencies)) {
                    return true;
                }
            } else if (recStack.contains(dependent)) {
                return true;
            }
        }

        recStack.remove(nodeId);
        return false;
    }

    /**
     * 获取节点的所有依赖者（反向查找）
     */
    private static Set<String> getDependents(String nodeId, Map<String, Set<String>> dependencies) {
        Set<String> dependents = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(nodeId)) {
                dependents.add(entry.getKey());
            }
        }
        return dependents;
    }

    /**
     * 验证DAG结构
     *
     * @param nodeMap          节点映射
     * @param dependencies     强依赖关系
     * @param weakDependencies 弱依赖关系
     * @throws RuntimeException 如果DAG结构无效
     */
    public static void validateDagStructure(Map<String, ?> nodeMap,
                                            Map<String, Set<String>> dependencies,
                                            Map<String, Set<String>> weakDependencies) {
        // 检查循环依赖
        if (hasCycle(nodeMap, dependencies)) {
            throw new RuntimeException("Circular dependency detected in DAG");
        }

        // 检查弱依赖中的循环
        if (hasCycle(nodeMap, weakDependencies)) {
            throw new RuntimeException("Circular dependency detected in DAG weak dependencies");
        }

        // 检查混合依赖中的循环（强依赖 + 弱依赖）
        Map<String, Set<String>> allDependencies = new HashMap<>(dependencies);
        for (Map.Entry<String, Set<String>> entry : weakDependencies.entrySet()) {
            String nodeId = entry.getKey();
            Set<String> weakDeps = entry.getValue();
            allDependencies.computeIfAbsent(nodeId, k -> new HashSet<>()).addAll(weakDeps);
        }

        if (hasCycle(nodeMap, allDependencies)) {
            throw new RuntimeException("Circular dependency detected in DAG mixed dependencies");
        }

        log.info("DAG structure validation passed, node count: {}", nodeMap.size());
    }
}

package com.qiao.flow.orchestrator.core.dag.utils;

import com.qiao.flow.orchestrator.core.dag.node.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Node Class到BeanName转换工具类
 * 统一管理所有Class到BeanName的转换逻辑
 *
 * @author qiao
 */
public class NodeBeanNameUtils {

    private static volatile Map<Class<? extends Node>, String> classToBeanNameMap;

    /**
     * 初始化Class到BeanName映射
     *
     * @param map 映射关系
     */
    public static void init(Map<Class<? extends Node>, String> map) {
        classToBeanNameMap = new HashMap<>(map);
    }

    /**
     * 根据Class获取Bean名称
     *
     * @param nodeClass Node类
     * @return Bean名称
     */
    public static String getBeanNameByClass(Class<? extends Node> nodeClass) {
        if (classToBeanNameMap == null) {
            throw new IllegalStateException("NodeBeanNameUtils not initialized");
        }

        String beanName = classToBeanNameMap.get(nodeClass);
        if (beanName != null) {
            return beanName;
        }

        // 降级到简单转换
        beanName = nodeClass.getSimpleName();
        beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
        return beanName;
    }
}

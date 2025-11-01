package com.qiao.flow.orchestrator.core.dag.annotation;

import com.qiao.flow.orchestrator.core.dag.callback.IChoose;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;

import java.lang.annotation.*;
import java.util.Set;

/**
 * 节点配置注解
 * 用于声明DAG节点的配置参数
 * 支持自动发现和DAG构建
 * <p>
 * 版本2.1.0更新：
 * - 去掉name属性，直接使用Bean名称作为节点名称
 * - dependsOn和weakDependsOn改为Class引用，支持IDE跳转
 *
 * @author qiao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NodeConfig {

    /**
     * 所属工作流
     * 必填字段，用于分组节点
     *
     * @return 工作流名称
     */
    String workflow();

    /**
     * 节点类型
     *
     * @return 节点类型枚举
     */
    NodeType type() default NodeType.CPU;

    /**
     * 依赖节点
     * 指定当前节点依赖的其他节点类，支持IDE跳转
     *
     * @return 依赖节点类数组
     */
    Class<? extends Node>[] dependsOn() default {};

    /**
     * 弱依赖节点
     * 指定当前节点的弱依赖节点类（ANY_OF：任一完成即可执行），支持IDE跳转
     *
     * @return 弱依赖节点类数组
     */
    Class<? extends Node>[] weakDependsOn() default {};

    // 新增：分支选择器
    Class<? extends IChoose<?>> chooser() default NoChoose.class;

    /**
     * 是否为开始节点
     * 标识当前节点为工作流的入口节点
     * 每个工作流只能有一个开始节点
     *
     * @return 是否为开始节点
     */
    boolean start() default false;

    /**
     * 是否为结束节点
     * 标识当前节点为工作流的出口节点
     * 每个工作流只能有一个结束节点
     *
     * @return 是否为结束节点
     */
    boolean end() default false;

    // 默认空实现（避免未配置时报错）
    class NoChoose implements IChoose<Object> {
        @Override
        public Set<Class<? extends Node<?>>> chooseNext(Object input, DagContext context) {
            return Set.of();
        }
    }

} 
package com.qiao.flow.orchestrator.core.dag.runner;

import com.qiao.flow.orchestrator.core.dag.annotation.NodeConfig;
import com.qiao.flow.orchestrator.core.dag.callback.DagExceptionHandler;
import com.qiao.flow.orchestrator.core.dag.callback.IChoose;
import com.qiao.flow.orchestrator.core.dag.callback.IDagCallback;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.context.DagContextPool;
import com.qiao.flow.orchestrator.core.dag.engine.DagEngine;
import com.qiao.flow.orchestrator.core.dag.engine.DagEngineConfig;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.core.dag.thread.pool.MixedThreadPoolManager;
import com.qiao.flow.orchestrator.core.dag.utils.DagAlgorithmUtils;
import com.qiao.flow.orchestrator.core.dag.utils.NodeBeanNameUtils;
import com.qiao.flow.orchestrator.core.dag.wrapper.NodeWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG自动运行器
 * 负责自动发现节点、构建DAG、执行DAG
 * 使用依赖注入，提高可测试性和性能
 * <p>
 * 性能优化：
 * 1. 缓存已构建的DagEngine实例，避免重复构建
 * 2. 缓存节点列表，避免重复扫描Spring Context
 * 3. 缓存分支选择器和条件判断器实例
 * 4. 使用原型模式确保DAG实例隔离
 *
 * @author qiao
 */
@Slf4j
@Component
public class DagAutoRunner {

    private final ApplicationContext applicationContext;
    private final MixedThreadPoolManager threadPoolManager;

    // 性能优化：简化缓存机制，只保留核心缓存
    private final Map<String, DagEngineConfig> configCache = new ConcurrentHashMap<>();

    // 只读缓存，使用volatile确保可见性，不需要线程安全类
    private volatile List<Node<?>> allNodesCache = null;


    @Autowired
    public DagAutoRunner(ApplicationContext applicationContext, MixedThreadPoolManager threadPoolManager) {
        this.applicationContext = applicationContext;
        this.threadPoolManager = threadPoolManager;
        log.info("DagAutoRunner initialization completed");
    }


    /**
     * 执行指定工作流（统一方法）
     * 支持异常处理器和回调配置，确保afterCallback在finally块中执行
     */
    public <T> void executeWorkflow(String workflowName, T input,
                                    DagExceptionHandler<T> exceptionHandler,
                                    IDagCallback beforeCallback,
                                    IDagCallback afterCallback) {
        try {
            // 获取或构建DAG引擎（原型模式）
            DagEngine<T> engine = getOrBuildEngine(workflowName);

            // 设置回调（由DagEngine统一管理回调执行）
            engine.setBeforeCallback(beforeCallback);
            engine.setAfterCallback(afterCallback);

            // 使用对象池获取DAG上下文
            DagContext dagContext = DagContextPool.borrow();
            try {
                // 执行DAG
                engine.execute(input, dagContext);

                // 检查执行结果
                if (engine.getEx() != null) {
                    // 调用异常处理器
                    if (exceptionHandler != null) {
                        try {
                            Throwable exception = engine.getEx();
                            if (exception instanceof Exception) {
                                exceptionHandler.handleException(
                                        (Exception) exception,
                                        input,
                                        dagContext
                                );
                            } else {
                                // 如果不是Exception类型，创建一个RuntimeException包装
                                exceptionHandler.handleException(
                                        new RuntimeException(exception),
                                        input,
                                        dagContext
                                );
                            }
                        } catch (Exception handlerException) {
                            log.warn("Exception handler failed", handlerException);
                        }
                    }
                }
            } finally {
                // 归还对象到池中
                DagContextPool.release(dagContext);
            }
        } finally {
            // 后回调由DagEngine统一管理，这里不需要重复执行
            // 确保资源清理等操作在这里进行
        }
    }

    /**
     * 获取或构建DAG引擎（原型模式）
     */
    private <T> DagEngine<T> getOrBuildEngine(String workflowName) {
        // 检查缓存
        DagEngineConfig config = configCache.get(workflowName);
        if (config != null) {
            // 从配置创建新的引擎实例
            return config.createEngine();
        }

        // 首次构建
        return buildNewEngine(workflowName);
    }

    /**
     * 构建新的执行引擎（首次创建）
     */
    private <T> DagEngine<T> buildNewEngine(String workflowName) {
        // 获取所有节点（使用缓存）
        List<Node<?>> allNodes = getAllNodes();

        // 过滤指定工作流的节点（使用缓存）
        List<Node<?>> workflowNodes = getOrFilterWorkflowNodes(allNodes, workflowName);

        if (workflowNodes.isEmpty()) {
            throw new IllegalStateException("未找到工作流 '" + workflowName + "' 的节点");
        }

        // 构建节点映射
        Map<String, NodeWrapper<?, ?>> nodeMap = buildNodeMap(workflowNodes);

        // 构建依赖关系
        Map<String, Set<String>> dependencies = buildDependencies(workflowNodes);

        // 构建弱依赖关系
        Map<String, Set<String>> weakDependencies = buildWeakDependencies(workflowNodes);

        // 验证DAG结构（包括循环检测）
        validateDagStructure(workflowName, nodeMap, dependencies, weakDependencies);

        // 创建执行引擎（包含弱依赖）
        DagEngine<T> engine = new DagEngine<>(nodeMap, dependencies, weakDependencies, threadPoolManager, 10000L);

        // 创建配置并缓存
        configCache.put(workflowName, new DagEngineConfig(engine));

        log.info("Engine and prototype built successfully for workflow: {}", workflowName);
        return engine;
    }

    /**
     * 获取或过滤工作流节点（直接过滤，不缓存）
     */
    private List<Node<?>> getOrFilterWorkflowNodes(List<Node<?>> allNodes, String workflowName) {
        List<Node<?>> filteredNodes = new ArrayList<>();
        for (Node<?> node : allNodes) {
            NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
            if (config != null && workflowName.equals(config.workflow())) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    /**
     * 获取所有节点（缓存优化）
     */
    private List<Node<?>> getAllNodes() {
        if (allNodesCache == null) {
            synchronized (this) {
                if (allNodesCache == null) {
                    Collection<Node> nodes = applicationContext.getBeansOfType(Node.class).values();
                    allNodesCache = new ArrayList<>();
                    for (Node node : nodes) {
                        allNodesCache.add((Node<?>) node);
                    }

                }
            }
        }
        return allNodesCache;
    }


    /**
     * 构建节点映射
     */
    private Map<String, NodeWrapper<?, ?>> buildNodeMap(List<Node<?>> nodes) {
        Map<String, NodeWrapper<?, ?>> nodeMap = new HashMap<>();
        Map<Class<? extends Node>, String> classToBeanNameMap = new HashMap<>();

        for (Node<?> node : nodes) {
            NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
            if (config == null) {
                continue;
            }

            // 从Bean获取节点名称
            String nodeId = getBeanNameFromNode(node);
            String workflow = config.workflow();
            NodeType nodeType = config.type();
            boolean isStart = config.start();
            boolean isEnd = config.end();

            // 从Class数组获取依赖节点名称
            Set<String> dependsOn = getBeanNamesFromClasses(config.dependsOn());
            Set<String> weakDependsOn = getBeanNamesFromClasses(config.weakDependsOn());

            // 获取分支选择器（使用缓存）
            IChoose<?> chooser = getOrCreateChooser(config.chooser());

            // 构建classToBeanNameMap
            classToBeanNameMap.put(node.getClass(), nodeId);

            // 创建节点包装器
            NodeWrapper<Object, Object> wrapper = new NodeWrapper(
                    nodeId, nodeId, workflow, nodeType, node,
                    dependsOn, weakDependsOn,
                    isStart, isEnd,
                    chooser
            );

            nodeMap.put(nodeId, wrapper);
        }

        // 在return前初始化工具类
        NodeBeanNameUtils.init(classToBeanNameMap);

        return nodeMap;
    }

    /**
     * 构建依赖关系
     */
    private Map<String, Set<String>> buildDependencies(List<Node<?>> nodes) {
        Map<String, Set<String>> dependencies = new HashMap<>();

        for (Node<?> node : nodes) {
            NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
            if (config == null) {
                continue;
            }

            String nodeId = getBeanNameFromNode(node);
            Set<String> dependsOn = getBeanNamesFromClasses(config.dependsOn());
            dependencies.put(nodeId, dependsOn);
        }

        return dependencies;
    }

    /**
     * 构建弱依赖关系
     */
    private Map<String, Set<String>> buildWeakDependencies(List<Node<?>> nodes) {
        Map<String, Set<String>> weakDependencies = new HashMap<>();

        for (Node<?> node : nodes) {
            NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
            if (config == null) {
                continue;
            }

            String nodeId = getBeanNameFromNode(node);
            Set<String> weakDependsOn = getBeanNamesFromClasses(config.weakDependsOn());
            weakDependencies.put(nodeId, weakDependsOn);
        }

        return weakDependencies;
    }

    /**
     * 根据Class获取Bean名称（通用方法）
     * 注意：此方法在构建阶段使用，需要从ApplicationContext获取
     */
    private String getBeanNameByClass(Class<? extends Node> nodeClass) {
        String[] beanNames = applicationContext.getBeanNamesForType(nodeClass);
        if (beanNames.length > 0) {
            return beanNames[0];
        }
        throw new IllegalStateException("无法找到节点Bean名称: " + nodeClass.getSimpleName());
    }

    /**
     * 从Bean获取节点名称
     */
    private String getBeanNameFromNode(Node<?> node) {
        return getBeanNameByClass(node.getClass());
    }

    /**
     * 根据依赖Class数组获取Bean名称集合
     */
    private Set<String> getBeanNamesFromClasses(Class<? extends Node>[] dependencyClasses) {
        Set<String> dependencies = new HashSet<>();

        for (Class<? extends Node> depClass : dependencyClasses) {
            // 检查@NodeConfig注解
            NodeConfig depConfig = depClass.getAnnotation(NodeConfig.class);
            if (depConfig == null) {
                throw new IllegalStateException(String.format(
                        "依赖的节点类 %s 缺少@NodeConfig注解", depClass.getSimpleName()
                ));
            }

            // 使用通用方法获取Bean名称
            dependencies.add(getBeanNameByClass(depClass));
        }

        return dependencies;
    }

    /**
     * 验证DAG结构（包括循环检测）
     */
    private void validateDagStructure(String workflowName, Map<String, NodeWrapper<?, ?>> nodeMap,
                                      Map<String, Set<String>> dependencies,
                                      Map<String, Set<String>> weakDependencies) {
        try {
            // 使用DagAlgorithmUtils进行循环检测
            DagAlgorithmUtils.validateDagStructure(nodeMap, dependencies, weakDependencies);
            log.info("DAG structure validation passed, workflow: {}", workflowName);
        } catch (RuntimeException e) {
            log.warn("DAG structure validation failed, workflow: {}, error: {}", workflowName, e.getMessage());
            throw e;
        }
    }


    /**
     * 获取或创建分支选择器（直接创建，不缓存）
     */
    private IChoose<?> getOrCreateChooser(Class<? extends IChoose> chooserClass) {
        if (chooserClass == null || chooserClass == IChoose.class) {
            return null;
        }

        try {
            return chooserClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.warn("Failed to create chooser: {}", chooserClass.getName(), e);
            return null;
        }
    }


    /**
     * 强制清理缓存
     * 用于解决Old Gen GC问题
     */
    public void forceCleanup() {
        int configCacheSize = configCache.size();
        configCache.clear();
        allNodesCache = null;
        log.info("DagAutoRunner force cleanup, cleared {} configs and node cache", configCacheSize);
    }

}

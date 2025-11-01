# Flow Orchestrator 技术详解 - 上

## 概述

Flow Orchestrator
是一个基于有向无环图（DAG）和链式工作流的业务流程编排框架，通过声明式配置、智能并发执行、线程池外部化等核心功能，为复杂业务流程提供了优雅的解决方案。框架兼容并支持JDK21和JDK25版本，适用于最新的Java运行环境。

## 1. 框架架构

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    外界应用层                                │
├─────────────────────────────────────────────────────────────┤
│  ThreadPoolConfig实现  │  业务节点实现  │  工作流配置        │
├─────────────────────────────────────────────────────────────┤
│                    Spring Boot自动配置层                     │
├─────────────────────────────────────────────────────────────┤
│  DagAutoConfiguration  │  OpsThreadPoolAutoConfiguration    │
├─────────────────────────────────────────────────────────────┤
│                     框架核心层                               │
├─────────────────────────────────────────────────────────────┤
│  DAG引擎  │  节点管理  │  依赖管理  │  执行调度  │  上下文管理 │
├─────────────────────────────────────────────────────────────┤
│                    线程池管理层                              │
├─────────────────────────────────────────────────────────────┤
│  外部注入线程池  │  IO线程池  │  CPU线程池  │  OPS线程池     │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 核心组件

#### DAG引擎（DagEngine）

- **职责**：DAG执行的核心引擎
- **功能**：节点调度、依赖管理、并发控制
- **特点**：支持强依赖和弱依赖，自动并发优化

#### 节点管理器（NodeWrapper）

- **职责**：节点包装和元数据管理
- **功能**：节点执行、结果存储、状态跟踪
- **特点**：支持节点类型识别、依赖关系管理

#### 线程池管理器

- **DAG线程池**：`MixedThreadPoolManager`管理IO和CPU线程池
- **Chain线程池**：`ChainThreadPoolManager`管理链式工作流线程池
- **特点**：完全外部化，支持业务场景优化

#### 上下文管理（DagContext）

- **职责**：节点间数据传递和状态管理
- **功能**：数据存储、结果传递、生命周期管理
- **特点**：支持多种数据传递方式，类型安全

## 2. 核心实现

### 2.1 节点配置注解

```java

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NodeConfig {

    /**
     * 所属工作流
     * 必填字段，用于分组节点
     */
    String workflow();

    /**
     * 节点类型
     */
    NodeType type() default NodeType.CPU;

    /**
     * 依赖节点
     * 指定当前节点依赖的其他节点类，支持IDE跳转
     */
    Class<? extends Node>[] dependsOn() default {};

    /**
     * 弱依赖节点
     * 指定当前节点的弱依赖节点类（ANY_OF：任一完成即可执行），支持IDE跳转
     */
    Class<? extends Node>[] weakDependsOn() default {};

    /**
     * 分支选择器
     */
    Class<? extends IChoose<?>> chooser() default NoChoose.class;

    /**
     * 是否为开始节点
     */
    boolean start() default false;

    /**
     * 是否为结束节点
     */
    boolean end() default false;
}
```

### 2.2 节点包装器

```java

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
    private final AtomicInteger weakIndegree;
    private volatile NodeResult<V> nodeResult;
    private volatile Thread executingThread;
}
```

### 2.3 DAG引擎

```java

@Slf4j
public class DagEngine<T> {

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
}
```

## 3. 线程池外部化设计

### 3.1 线程池配置接口

```java
public interface ThreadPoolConfig {

    /**
     * 获取DAG CPU线程池
     * 用于CPU密集型节点执行
     */
    ExecutorService getDagCpuThreadPool();

    /**
     * 获取DAG IO线程池
     * 用于IO密集型节点执行（虚拟线程）
     */
    ExecutorService getDagIoThreadPool();

    /**
     * 获取OPS Item维度线程池
     * 用于物品维度计算
     */
    ExecutorService getOpsItemDimensionThreadPool();

    /**
     * 获取OPS Factor线程池
     * 用于因子计算
     */
    ExecutorService getOpsFactorThreadPool();

    /**
     * 获取OPS Context维度线程池
     * 用于上下文维度计算
     */
    ExecutorService getOpsContextDimensionThreadPool();
}
```

### 3.2 混合线程池管理器

```java

@Getter
public class MixedThreadPoolManager {

    private final ExecutorService cpuThreadPool;
    private final ExecutorService ioThreadPool;

    public MixedThreadPoolManager(ExecutorService cpuThreadPool, ExecutorService ioThreadPool) {
        this.cpuThreadPool = cpuThreadPool;
        this.ioThreadPool = ioThreadPool;
    }

    /**
     * 根据节点类型选择线程池
     */
    public ExecutorService getThreadPool(NodeType nodeType) {
        return switch (nodeType) {
            case IO -> ioThreadPool;    // IO任务使用外部注入的线程池
            case CPU -> cpuThreadPool;  // CPU任务使用外部注入的线程池
        };
    }
}
```

### 3.3 OPS线程池管理器

```java

@Getter
public class OpsThreadPoolManager {

    private final ExecutorService itemDimensionThreadPool;
    private final ExecutorService factorThreadPool;
    private final ExecutorService contextDimensionThreadPool;

    public OpsThreadPoolManager(
            ExecutorService itemDimensionThreadPool,
            ExecutorService factorThreadPool,
            ExecutorService contextDimensionThreadPool) {
        this.itemDimensionThreadPool = itemDimensionThreadPool;
        this.factorThreadPool = factorThreadPool;
        this.contextDimensionThreadPool = contextDimensionThreadPool;
    }
}
```

## 4. Spring Boot自动配置

### 4.1 DAG自动配置

```java

@Slf4j
@Configuration
@EnableConfigurationProperties({DagCleanupConfigProperties.class})
public class DagAutoConfiguration {

    /**
     * 混合线程池管理器
     * 强制要求外界提供ThreadPoolConfig实现
     */
    @Bean
    public MixedThreadPoolManager mixedThreadPoolManager(ThreadPoolConfig config) {
        return new MixedThreadPoolManager(
                config.getDagCpuThreadPool(),
                config.getDagIoThreadPool()
        );
    }

    /**
     * DAG清理调度器
     */
    @Bean
    @ConditionalOnMissingBean
    public DagCleanupScheduler dagCleanupScheduler(DagCleanupConfigProperties cleanupProperties) {
        DagCleanupScheduler scheduler = new DagCleanupScheduler();

        if (cleanupProperties.isEnabled()) {
            scheduler.setAllIntervals(
                    cleanupProperties.getFinalEvictionIntervalMs(),
                    cleanupProperties.getFinalEvictableIdleTimeMs()
            );

            log.info("DAG cleanup configuration initialization completed:\n{}",
                    cleanupProperties.getConfigDescription());
        }

        return scheduler;
    }
}
```

### 4.2 OPS线程池自动配置

```java

@Slf4j
@Configuration
public class OpsThreadPoolAutoConfiguration {

    /**
     * OPS线程池管理器
     * 强制要求外界提供ThreadPoolConfig实现
     * 在创建时立即初始化静态线程池
     */
    @Bean
    public OpsThreadPoolManager opsThreadPoolManager(ThreadPoolConfig config) {
        OpsThreadPoolManager manager = new OpsThreadPoolManager(
                config.getOpsItemDimensionThreadPool(),
                config.getOpsFactorThreadPool(),
                config.getOpsContextDimensionThreadPool()
        );

        // 立即初始化静态线程池
        FactorThreadPool.setExecutor(manager.getFactorThreadPool());
        ContextDimensionThreadPool.setExecutor(manager.getContextDimensionThreadPool());
        ItemDimensionThreadPool.setExecutor(manager.getItemDimensionThreadPool());

        log.info("OPS thread pools initialized successfully");

        return manager;
    }
}
```

## 5. 节点执行流程

### 5.1 节点发现和构建

```java
/**
 * 构建节点映射
 */
private Map<String, NodeWrapper<?, ?>> buildNodeMap(List<Node<?>> nodes) {
    Map<String, NodeWrapper<?, ?>> nodeMap = new HashMap<>();

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

        // 创建节点包装器
        NodeWrapper<Object, Object> wrapper = new NodeWrapper(
                nodeId, nodeId, workflow, nodeType, node,
                dependsOn, weakDependsOn,
                isStart, isEnd,
                chooser, this
        );

        nodeMap.put(nodeId, wrapper);
    }

    return nodeMap;
}
```

### 5.2 依赖关系构建

```java
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
```

## 6. 数据传递机制

### 6.1 业务上下文传递

**特点**：数据跟随业务上下文流转，生命周期与业务请求一致

```java
// 节点间数据传递
@NodeConfig(
        workflow = "ranking"
)
public class FeatureExtractionNode implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext input, DagContext context, String nodeId) throws Exception {
        // 获取上游节点结果
        String userProfile = context.getNodeResult("userProfileNode");

        // 执行业务逻辑
        String features = extractFeatures(userProfile);

        // 存储结果
        context.putData("features", features);
    }
}
```

### 6.2 框架上下文传递

**特点**：数据存储在DAG框架中，与业务逻辑分离

```java
// 结果传递示例
@NodeConfig(
        workflow = "ranking"
)
public class RankingAggregationNode implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext input, DagContext context, String nodeId) throws Exception {
        // 获取依赖节点结果
        String features = context.getNodeResult("featureExtractionNode");
        String modelResult = context.getNodeResult("modelPredictionNode");

        // 执行聚合逻辑
        String finalResult = aggregateResults(features, modelResult);

        // 存储最终结果
        context.putNodeResult("rankingAggregationNode", finalResult);
    }
}
```

## 7. 实际应用示例

### 7.1 广告排序工作流

基于实际代码的广告排序示例：

```java
// 检查开关节点
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        start = true
)
public class CheckSwitch implements Node<ExampleContext> {
    // 检查业务开关
}

// 默认排序节点
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {CheckSwitch.class}
)
public class DefaultRank implements Node<ExampleContext> {
    // 执行默认排序逻辑
}

// 白盒排序节点
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.CPU,
        dependsOn = {SelectAlgorithm.class}
)
public class WhiteBoxRank implements Node<ExampleContext> {
    // 执行白盒排序算法
}

// 黑盒排序节点
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.CPU,
        dependsOn = {SelectAlgorithm.class}
)
public class BlackBoxRank implements Node<ExampleContext> {
    // 执行黑盒排序算法
}
```

### 7.2 商品排序工作流

```java
// 商品验证节点
@Component
@NodeConfig(
        workflow = WorkflowNames.CRUISE_PRODUCT_RANKING,
        type = NodeType.IO,
        start = true
)
public class ValidateProduct implements Node<ExampleContext> {
    // 验证商品信息
}

// 商品排序节点
@Component
@NodeConfig(
        workflow = WorkflowNames.CRUISE_PRODUCT_RANKING,
        type = NodeType.CPU,
        dependsOn = {ProductScore.class},
        end = true
)
public class ProductRank implements Node<ExampleContext> {
    // 执行商品排序
}
```

## 8. 性能优化

### 8.1 并发执行优化

**核心机制**：

- 自动识别可并行节点
- 基于依赖关系的智能调度
- 线程池类型匹配（IO/CPU）

**性能提升**：

- 支持高并发场景，提升系统吞吐量
- 自动优化资源使用，降低系统成本
- 智能线程分配，最大化资源利用效率

### 8.2 线程池外部化优化

**性能提升**：

- ✅ **避免重复创建**：框架不再硬编码线程池，减少资源浪费
- ✅ **业务场景优化**：外界可针对具体业务优化线程池参数
- ✅ **内存占用减少**：清理过时代码，减少内存占用
- ✅ **GC压力降低**：减少对象创建，降低垃圾回收压力

**架构优势**：

- ✅ **完全解耦**：框架与线程池实现完全分离
- ✅ **灵活配置**：支持虚拟线程、平台线程、自定义线程池
- ✅ **监控友好**：外界可选择是否使用Cat包装
- ✅ **性能提升**：实测性能提升明显

### 8.3 智能线程池管理

- **任务类型识别**：IO任务使用虚拟线程，CPU任务使用平台线程
- **自动资源分配**：根据任务特点选择最优执行方式，最大化资源利用效率
- **外部化配置**：通过`ThreadPoolConfig`接口实现完全外部化线程池管理

## 9. 总结

Flow Orchestrator
通过声明式配置、智能并发执行、线程池外部化等核心功能，为复杂业务流程提供了优雅的解决方案。框架不仅提升了开发效率和执行性能，还增强了系统稳定性，为广告排名、推荐系统、数据处理等高频业务场景提供了强有力的技术支撑。

### 9.1 核心价值

- **开发效率**：声明式配置，显著降低开发复杂度
- **执行性能**：智能并发执行，充分利用多核资源
- **系统稳定性**：统一的错误处理和状态管理
- **运维友好**：内置监控和调试功能
- **架构优化**：线程池外部化，支持业务场景优化

### 9.2 技术优势

- **高性能**：智能并发执行，支持高并发场景
- **高可用**：完善的错误处理和重试机制
- **高扩展**：灵活的架构设计，支持业务快速迭代
- **高维护**：清晰的代码结构，便于维护和扩展

# Flow Orchestrator Chain 接入文档

## 1. Chain 简介

### 1.1 什么是 Chain

Chain（链式工作流）是 Flow Orchestrator 框架中的另一种工作流编排方式，与 DAG 不同，Chain 是一种**顺序执行**
的工作流模式，每个步骤按顺序执行，前一个步骤的输出作为下一个步骤的输入。

### 1.2 Chain 的特点

- **顺序执行**：步骤按定义顺序依次执行
- **数据传递**：前一步的输出自动传递给下一步
- **简单直观**：适合线性业务流程
- **错误处理**：支持统一的异常处理机制
- **类型安全**：强类型的数据传递
- **多版本JDK支持**：兼容并支持JDK21和JDK25版本

### 1.3 适用场景

- **数据处理管道**：ETL 流程、数据清洗
- **审批流程**：多级审批、状态流转
- **业务编排**：订单处理、支付流程
- **API 调用链**：微服务调用序列

## 2. 快速开始

### 2.1 添加依赖

```xml
<!-- Flow Orchestrator 核心依赖 -->
<dependency>
    <groupId>com.qiao.flow</groupId>
    <artifactId>flow-orchestrator-core</artifactId>
    <version>1.0.0</version>
</dependency>

        <!-- Flow Orchestrator Spring Boot 自动配置 -->
<dependency>
<groupId>com.qiao.flow</groupId>
<artifactId>flow-orchestrator-spring-boot-starter</artifactId>
<version>1.0.0</version>
</dependency>
```

### 2.2 直接开始开发

Chain 是顺序执行的工作流，不需要配置线程池。你可以直接开始定义 Chain 步骤。

## 3. Chain 开发

### 3.1 Chain 的基本概念

Chain 工作流使用**方法引用**和**Lambda 表达式**来定义步骤，不需要实现特定的接口。每个步骤都是一个方法，通过 `ChainWorkFlow`
的链式调用进行组合。

### 3.2 创建 Chain 工作流

```java

@Service
public class DataProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processData(ExampleContext context) {
        // 创建 Chain 工作流
        ChainWorkFlow<ExampleContext> chain = ChainWorkFlow.<ExampleContext>create()
                .addStep("数据验证", this::validateData)
                .addStep("数据处理", this::processData)
                .addStep("结果生成", this::generateResult);

        // 执行 Chain
        chainWorkFlowEngine.execute(chain, context, this::handleException);
    }

    // 步骤方法
    private void validateData(ExampleContext input) {
        log.info("🔍 开始数据验证");

        if (input.getUserId() == null || input.getUserId().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        log.info("✅ 数据验证完成");
    }

    private void processData(ExampleContext input) {
        log.info("⚙️ 开始数据处理");

        String processedData = "processed_" + input.getUserId() + "_" + System.currentTimeMillis();
        input.setProcessedData(processedData);

        log.info("✅ 数据处理完成: {}", processedData);
    }

    private void generateResult(ExampleContext input) {
        log.info("📊 开始结果生成");

        String result = String.format("用户[%s]的处理结果: %s",
                input.getUserId(), input.getProcessedData());
        input.setResult(result);

        log.info("✅ 结果生成完成: {}", result);
    }

    private void handleException(Exception e, String stepName, ExampleContext context) {
        log.error("步骤 {} 执行失败: {}", stepName, e.getMessage());
    }
}
```

## 4. Chain 执行

### 4.1 使用 Chain 工作流

```java

@RestController
@RequestMapping("/api/chain")
public class ChainController {

    @Autowired
    private DataProcessingService dataProcessingService;

    @PostMapping("/process")
    public ResponseEntity<String> processData(@RequestBody ExampleContext context) {
        try {
            dataProcessingService.processData(context);
            return ResponseEntity.ok("处理完成: " + context.getResult());
        } catch (Exception e) {
            log.error("Chain 执行失败", e);
            return ResponseEntity.status(500).body("处理失败: " + e.getMessage());
        }
    }
}
```

### 4.2 业务排名 Chain 示例

以下是一个完整的业务排名 Chain 示例，展示了 Chain 的各种高级特性：

#### 4.2.1 业务上下文定义

```java

@Data
@EqualsAndHashCode(callSuper = false)
public class RankContextInfo extends ExampleContext {

    // 用户偏好相关
    private String userPreference;
    private Double preferenceScore;

    // 用户行为相关
    private String userBehavior;
    private Double behaviorScore;

    // 用户画像相关
    private String userProfile;
    private Double profileScore;

    // 推荐相关
    private Double recommendationScore;
    private String recommendationReason;

    // 风险相关
    private Double riskScore;
    private String riskLevel;
    private String riskReason;

    // 最终结果
    private Double finalScore;
    private String finalDecision;
    private String finalReason;

    // 场景标识
    private boolean blackScene;

    public boolean isBlackScene() {
        return blackScene;
    }
}
```

#### 4.2.2 排名业务服务实现

```java

@Service
public class DefaultRankBizServiceImpl implements RankBizService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    @Override
    public void productRank() {
        long startTime = System.currentTimeMillis();

        // 创建复杂的 Chain 工作流
        ChainWorkFlow<RankContextInfo> chainWorkFlow = createDefaultWorkFlow(startTime);
        RankContextInfo rankContextInfo = new RankContextInfo("user123", "req456");

        // 执行 Chain，带异常处理
        chainWorkFlowEngine.execute(chainWorkFlow, rankContextInfo, this::handleException);
    }

    private void handleException(Exception e, String stepName, RankContextInfo rankContextInfo) {
        log.error("处理失败: {} - {}", stepName, e.getMessage());
    }

    protected ChainWorkFlow<RankContextInfo> createDefaultWorkFlow(long startTime) {
        return ChainWorkFlow.<RankContextInfo>create()
                // 基础步骤
                .addStep("初始化上下文", this::initContext)
                .addStepWithException("获取基础信息", this::getBasicInfo)

                // 条件步骤
                .addConditionalStep("自定义步骤", ctx -> true)
                .addConditionalStep("checkSwitch",
                        this::checkSwitch,
                        ctx -> {
                            // 当 checkSwitch 返回 true 时执行的操作
                            log.info("Switch is on, proceeding with normal flow");
                        },
                        ctx -> {
                            // 当 checkSwitch 返回 false 时执行的操作
                            log.info("Switch is off, using default response");
                            defaultMethod();
                        }
                )

                // 异常处理步骤
                .addConditionalStep("异常处理步骤",
                        ctx -> {
                            riskyOperation(ctx);
                            return true;
                        },
                        e -> log.error("处理异常: {}", e.getMessage())
                )

                // 分支处理
                .addBranch("选择排序处理方式",
                        RankContextInfo::isBlackScene,
                        this::blackBoxRankProcess,
                        this::whiteBoxRank
                )

                // 多分支处理
                .addMultiBranch("复杂排序处理",
                        ctx -> {
                            if (ctx.isBlackScene()) return "BLACK";
                            return "DEFAULT";
                        },
                        Map.of(
                                "BLACK", this::blackBoxRankProcess,
                                "DEFAULT", this::whiteBoxRank)
                );
    }

    // 各种业务方法实现
    private void initContext(RankContextInfo ctx) {
        log.info("初始化排名上下文");
        ctx.setUserLevel("VIP");
        ctx.setBlackScene(Math.random() > 0.5);
    }

    private void getBasicInfo(RankContextInfo ctx) throws Exception {
        log.info("获取用户基础信息");
        ctx.setUserPreference("旅游");
        ctx.setUserBehavior("活跃");
        // 模拟可能的异常
        // throw new RuntimeException("获取信息失败");
    }

    private Boolean checkSwitch(RankContextInfo rankContextInfo) {
        log.info("检查业务开关");
        return true;
    }

    private void defaultMethod() {
        log.info("执行默认方法");
    }

    private void riskyOperation(RankContextInfo ctx) {
        log.info("执行风险操作");
        ctx.setRiskScore(0.1);
    }

    private void whiteBoxRank(RankContextInfo rankContextInfo) throws Exception {
        log.info("执行白盒排序算法");
        rankContextInfo.setFinalScore(0.95);
        rankContextInfo.setFinalDecision("推荐");
        // 模拟异常
        // throw new Exception("白盒排序失败");
    }

    private void blackBoxRankProcess(RankContextInfo rankContextInfo) {
        log.info("执行黑盒排序算法");
        rankContextInfo.setFinalScore(0.88);
        rankContextInfo.setFinalDecision("待定");
    }
}
```

#### 4.2.3 使用示例

```java

@RestController
@RequestMapping("/api/rank")
public class RankController {

    @Autowired
    private RankBizService rankBizService;

    @PostMapping("/product")
    public ResponseEntity<String> rankProduct(@RequestBody RankRequest request) {
        try {
            rankBizService.productRank();
            return ResponseEntity.ok("排名完成");
        } catch (Exception e) {
            log.error("排名失败", e);
            return ResponseEntity.status(500).body("排名失败: " + e.getMessage());
        }
    }
}
```

### 4.2 使用 Chain 工作流

```java

@RestController
@RequestMapping("/api/chain")
public class ChainController {

    @Autowired
    private DataProcessingService dataProcessingService;

    @PostMapping("/process")
    public ResponseEntity<String> processData(@RequestBody ExampleContext context) {
        try {
            String result = dataProcessingService.processData(context);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Chain 执行失败", e);
            return ResponseEntity.status(500).body("处理失败: " + e.getMessage());
        }
    }
}
```

## 5. 高级特性

### 5.1 条件执行

```java

@Service
public class ConditionalProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processWithCondition(ExampleContext context) {
        ChainWorkFlow<ExampleContext> chain = ChainWorkFlow.<ExampleContext>create()
                .addConditionalStep("VIP用户检查",
                        ctx -> "VIP".equals(ctx.getUserLevel()),
                        ctx -> {
                            log.info("VIP用户，执行特殊处理");
                            ctx.setSpecialProcessed(true);
                        },
                        ctx -> {
                            log.info("普通用户，跳过特殊处理");
                            ctx.setSpecialProcessed(false);
                        }
                );

        chainWorkFlowEngine.execute(chain, context, this::handleException);
    }

    private void handleException(Exception e, String stepName, ExampleContext context) {
        log.error("步骤 {} 执行失败: {}", stepName, e.getMessage());
    }
}
```

### 5.2 分支处理

```java

@Service
public class BranchProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processWithBranch(ExampleContext context) {
        ChainWorkFlow<ExampleContext> chain = ChainWorkFlow.<ExampleContext>create()
                .addBranch("选择处理方式",
                        ctx -> ctx.getUserLevel() != null && ctx.getUserLevel().equals("VIP"),
                        this::vipProcessing,
                        this::normalProcessing
                );

        chainWorkFlowEngine.execute(chain, context, this::handleException);
    }

    private void vipProcessing(ExampleContext ctx) {
        log.info("执行VIP用户处理逻辑");
        ctx.setProcessedData("VIP_" + ctx.getUserId());
    }

    private void normalProcessing(ExampleContext ctx) {
        log.info("执行普通用户处理逻辑");
        ctx.setProcessedData("NORMAL_" + ctx.getUserId());
    }

    private void handleException(Exception e, String stepName, ExampleContext context) {
        log.error("步骤 {} 执行失败: {}", stepName, e.getMessage());
    }
}
```

### 5.3 多分支处理

```java

@Service
public class MultiBranchProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processWithMultiBranch(ExampleContext context) {
        ChainWorkFlow<ExampleContext> chain = ChainWorkFlow.<ExampleContext>create()
                .addMultiBranch("复杂处理逻辑",
                        ctx -> {
                            if ("VIP".equals(ctx.getUserLevel())) return "VIP";
                            if ("PREMIUM".equals(ctx.getUserLevel())) return "PREMIUM";
                            return "NORMAL";
                        },
                        Map.of(
                                "VIP", this::vipProcessing,
                                "PREMIUM", this::premiumProcessing,
                                "NORMAL", this::normalProcessing
                        )
                );

        chainWorkFlowEngine.execute(chain, context, this::handleException);
    }

    private void vipProcessing(ExampleContext ctx) {
        log.info("执行VIP用户处理");
        ctx.setProcessedData("VIP_PROCESSED");
    }

    private void premiumProcessing(ExampleContext ctx) {
        log.info("执行PREMIUM用户处理");
        ctx.setProcessedData("PREMIUM_PROCESSED");
    }

    private void normalProcessing(ExampleContext ctx) {
        log.info("执行普通用户处理");
        ctx.setProcessedData("NORMAL_PROCESSED");
    }

    private void handleException(Exception e, String stepName, ExampleContext context) {
        log.error("步骤 {} 执行失败: {}", stepName, e.getMessage());
    }
}
```

## 6. 多 Chain 支持

### 6.1 一个项目中的多个 Chain

**是的，Chain 支持在一个项目中定义多个 Chain！** 每个 Service 都可以定义自己的 Chain 工作流，它们之间相互独立。

```java

@Service
public class OrderProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processOrder(OrderContext context) {
        ChainWorkFlow<OrderContext> orderChain = ChainWorkFlow.<OrderContext>create()
                .addStep("验证订单", this::validateOrder)
                .addStep("计算价格", this::calculatePrice)
                .addStep("生成订单", this::generateOrder);

        chainWorkFlowEngine.execute(orderChain, context, this::handleException);
    }

    // 订单相关方法...
}

@Service
public class PaymentProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    public void processPayment(PaymentContext context) {
        ChainWorkFlow<PaymentContext> paymentChain = ChainWorkFlow.<PaymentContext>create()
                .addStep("验证支付", this::validatePayment)
                .addStep("处理支付", this::processPayment)
                .addStep("更新状态", this::updateStatus);

        chainWorkFlowEngine.execute(paymentChain, context, this::handleException);
    }

    // 支付相关方法...
}
```

### 6.2 Chain 的独立性

- **独立的上下文**：每个 Chain 可以使用不同的上下文类型
- **独立的方法**：每个 Service 定义自己的步骤方法
- **独立的异常处理**：每个 Chain 可以有自己的异常处理策略
- **独立的生命周期**：每个 Chain 的执行互不影响

## 7. 最佳实践

### 7.1 步骤设计原则

1. **单一职责**：每个步骤方法只负责一个明确的功能
2. **无状态设计**：步骤方法不应维护状态，所有数据通过上下文传递
3. **异常处理**：合理处理异常，避免影响整个 Chain
4. **方法命名**：使用清晰的方法名，便于理解和维护

### 7.2 代码组织

1. **服务分离**：不同的业务逻辑使用不同的 Service
2. **方法分组**：相关的步骤方法放在同一个 Service 中
3. **上下文设计**：为不同的业务场景设计合适的上下文类
4. **异常处理**：统一的异常处理策略

### 7.3 性能考虑

1. **方法优化**：优化步骤方法内部的业务逻辑
2. **资源管理**：及时释放资源，避免内存泄漏
3. **缓存利用**：合理使用缓存，避免重复计算
4. **监控告警**：设置监控指标，及时发现问题

## 8. 与 DAG 的对比

| 特性   | Chain | DAG  |
|------|-------|------|
| 执行模式 | 顺序执行  | 并发执行 |
| 复杂度  | 简单    | 复杂   |
| 性能   | 较低    | 较高   |
| 适用场景 | 线性流程  | 复杂依赖 |
| 调试难度 | 容易    | 较难   |
| 扩展性  | 有限    | 很强   |

## 9. 总结

Chain 工作流是 Flow Orchestrator 框架中的重要组成部分，它提供了简单直观的线性工作流编排能力。通过顺序执行和自动数据传递，Chain
特别适合处理线性业务流程，如数据处理管道、审批流程等。

### 9.1 核心优势

- **简单易用**：直观的链式调用，降低学习成本
- **类型安全**：强类型的数据传递，减少运行时错误
- **灵活配置**：支持条件执行、异常处理等高级特性
- **监控友好**：内置监控和调试功能

### 9.2 适用场景

- **数据处理管道**：ETL 流程、数据清洗
- **审批流程**：多级审批、状态流转
- **业务编排**：订单处理、支付流程
- **API 调用链**：微服务调用序列

通过合理使用 Chain 工作流，可以显著提升开发效率，简化业务流程的实现和维护。

# Flow Orchestrator

## 项目简介

Flow Orchestrator 是一个企业级工作流编排框架，支持两种主要的工作流模式：Chain（链式工作流）和 DAG (有向无环图)。框架与 Spring
紧密结合，提供了声明式配置、智能并发执行、线程池外部化等核心功能，适用于复杂业务场景的流程编排。

## 1. 框架亮点

Flow Orchestrator 框架已在企业级生产环境稳定运行，在排序业务系统中经过3个月（截至2025年10月28日）的验证，性能（耗时和内存）显著优于传统开发模式。

### 1.1 工作流模式

- **两种编排方式**：提供DAG（有向无环图）和Chain（链式工作流）两种编排方式，满足不同业务场景需求

### 1.2 DAG框架核心优势

- **多版本JDK支持**：兼容并支持JDK21和JDK25，充分利用最新Java特性
- **线程复用机制**：复用Tomcat请求线程，避免为每个请求启动新线程，显著降低资源消耗
- **智能线程分配**：根据节点类型按需使用虚拟线程或平台线程，优化资源利用效率
- **零额外线程开销**：当图中无并发节点（线性执行）时，不会启动任何额外线程
- **动态链路剪枝**：根据请求数据属性值动态执行对应链路，智能裁剪不需要的执行路径，类似于if else
- **极致性能表现**：框架本身开销极小，30个节点仅需1ms，50个节点仅需2ms

### 1.3 开发体验优势

- **声明式配置**：通过注解轻松配置工作流节点和依赖关系
- **Spring无缝集成**：支持依赖注入和自动配置，与现有Spring项目完美融合
- **类型安全**：强类型的数据传递机制，减少运行时错误

## 2. 产品说明

完整的产品功能和价值说明请参阅：
[Flow-Orchestrator-产品说明文档](./flow-orchestrator-example/docs/Flow-Orchestrator-产品说明文档.md)

## 3. 接入文档

### 3.1 DAG（有向无环图）接入

DAG适用于具有复杂依赖关系的工作流，支持智能并发执行。

详细接入说明请参阅：[Flow-Orchestrator-DAG接入文档](./flow-orchestrator-example/docs/Flow-Orchestrator-接入文档.md)

### 3.2 Chain（链式工作流）接入

Chain适用于线性业务流程，按顺序执行步骤，数据自动传递。

详细接入说明请参阅：[Flow-Orchestrator-Chain接入文档](./flow-orchestrator-example/docs/Flow-Orchestrator-Chain接入文档.md)

## 4. 快速开始

### 4.1 添加依赖

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

### 4.2 选择合适的工作流模式

- **DAG**：适用于复杂依赖、需要并发执行的场景
- **Chain**：适用于线性流程、顺序执行的场景

请根据业务需求选择合适的工作流模式，并参考相应的接入文档进行开发。

## 5. 示例与测试

框架提供了丰富的示例和测试用例，位于 `flow-orchestrator-example/src/test/java` 目录下，这些测试用例展示了不同场景下框架的使用方法：

### 5.1 测试用例说明

#### DAG模式测试用例

- **AdRankingServiceTest.java**：广告排序服务的实现和测试，展示DAG模式在复杂业务场景中的应用
- **MultiDagTest.java**：多DAG工作流测试，展示如何同时管理和执行多个有向无环图工作流
- **PerformanceComparisonTest.java**：DAG框架性能测试
- **RankBizServiceTest.java**：排序业务服务测试，展示框架在实际业务中的集成方式

#### Chain模式测试用例

- **MultiChainTest.java**：多链式工作流测试，展示如何同时管理和执行多个链式工作流

### 5.2 测试用例路径

所有测试用例位于以下路径：
`flow-orchestrator-example/src/test/java/com/qiao/flow/orchestrator/example/service/`

建议通过运行这些测试用例来了解框架的实际使用方式和功能特性。

## 6. 贡献指南

欢迎提交 Issue 和 Pull Request 来改进框架。请确保代码符合项目的编码规范，并添加适当的测试用例。

## 7. 许可证

[Apache License 2.0](LICENSE)

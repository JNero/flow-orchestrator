package com.qiao.flow.orchestrator.example.node;

import com.qiao.flow.orchestrator.core.dag.annotation.NodeConfig;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.example.constants.WorkflowNames;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 特征平台分数节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {MergeOperatorResult.class}
)
public class FeaturePlatformScore implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始特征平台分数计算，节点ID: {}", nodeId);

        // 模拟特征平台分数计算
        double platformScore = 0.92;

        // 使用新的API：显式传递nodeId
        context.putNodeResult(nodeId, platformScore);

        // 也可以使用通用数据存储
        context.putData("platformScore", platformScore);

        log.info("✅ 特征平台分数计算完成: score={}", platformScore);
    }
}

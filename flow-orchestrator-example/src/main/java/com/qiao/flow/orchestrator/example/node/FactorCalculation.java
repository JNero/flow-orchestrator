package com.qiao.flow.orchestrator.example.node;

import com.qiao.flow.orchestrator.core.dag.annotation.NodeConfig;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.example.constants.WorkflowNames;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 因子计算节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {AssembleFeatureDimensions.class}
)
public class FactorCalculation implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始因子计算，节点ID: {}", nodeId);

        // 模拟计算因子结果
        Map<Long, Map<Long, Object>> factorResults = new HashMap<>();
        Map<Long, Object> userFactors = new HashMap<>();
        userFactors.put(1001L, 0.85);
        userFactors.put(1002L, 0.92);
        userFactors.put(1003L, 0.78);
        factorResults.put(12345L, userFactors);

        // 使用 putNodeResult 设置业务数据（显式传递nodeId）
        context.putNodeResult(nodeId, factorResults);

        log.info("✅ 因子计算完成，计算了{}个用户的因子", userFactors.size());
    }
}

package com.qiao.flow.orchestrator.example.node;

import com.qiao.flow.orchestrator.core.dag.annotation.NodeConfig;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import com.qiao.flow.orchestrator.example.constants.WorkflowNames;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 合并算子结果节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.CPU,
        dependsOn = {FactorCalculation.class}
)
public class MergeOperatorResult implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始合并算子结果");

        // 获取FactorCalculation节点的结果
        // 支持IDE跳转，类型安全
        Map<Long, Map<Long, Object>> factorResults = context.getNodeResult(FactorCalculation.class);
        if (factorResults != null) {
            log.info("📊 获取到因子计算结果，用户数量: {}", factorResults.size());

            // 模拟合并算子结果
            String mergedResult = "merged_" + System.currentTimeMillis();
            context.putData("mergedResult", mergedResult);

            log.info("✅ 算子结果合并完成: result={}, 基于{}个用户的因子数据", mergedResult, factorResults.size());
        } else {
            log.warn("⚠️ 未找到FactorCalculation节点的结果");
        }
    }
}

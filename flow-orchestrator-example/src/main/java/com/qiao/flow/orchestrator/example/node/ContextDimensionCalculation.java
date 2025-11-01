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
 * 上下文维度计算节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {GetVersionInfo.class}
)
public class ContextDimensionCalculation implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始上下文维度计算");

        // 模拟上下文维度计算
        String contextDimension = "context_" + System.currentTimeMillis();
        context.putData("contextDimension", contextDimension);

        log.info("✅ 上下文维度计算完成: dimension={}", contextDimension);
    }
}

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
 * 商品评分节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.CRUISE_PRODUCT_RANKING,
        type = NodeType.CPU,
        dependsOn = {ProductValidate.class}
)
public class ProductScore implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始商品评分");

        // 模拟商品评分逻辑
        double score = 0.85;
        context.putData("productScore", score);

        log.info("✅ 商品评分完成: score={}", score);
    }
}

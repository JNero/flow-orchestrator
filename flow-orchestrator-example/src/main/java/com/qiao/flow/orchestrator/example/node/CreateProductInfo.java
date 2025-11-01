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
 * 创建商品信息节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {ValidateProduct.class}
)
public class CreateProductInfo implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始创建商品信息");

        // 模拟创建商品信息
        String productInfo = "product_" + System.currentTimeMillis();
        context.putData("productInfo", productInfo);

        log.info("✅ 商品信息创建完成: info={}", productInfo);
    }
}

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
 * 商品验证节点 - 商品排序DAG的起始节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.CRUISE_PRODUCT_RANKING,
        type = NodeType.IO,
        start = true
)
public class ProductValidate implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始商品验证");

        // 模拟商品验证逻辑
        boolean isValid = true;
        context.putData("productValid", isValid);

        log.info("✅ 商品验证完成: isValid={}", isValid);
    }
}

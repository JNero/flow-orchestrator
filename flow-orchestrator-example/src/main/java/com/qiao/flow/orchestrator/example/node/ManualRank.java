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
 * 手动排序节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.CPU,
        dependsOn = {SortProductList.class}
)
public class ManualRank implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始手动排序");

        // 模拟手动排序
        String manualRank = "manual_" + System.currentTimeMillis();
        context.putData("manualRank", manualRank);

        log.info("✅ 手动排序完成: rank={}", manualRank);
    }
}

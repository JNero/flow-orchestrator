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
 * 数据摄入节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        dependsOn = {FeaturePlatformScore.class, CtrFactor.class, BlackUserLocal.class}
)
public class IngestData implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始数据摄入");

        // 模拟数据摄入
        String ingestedData = "ingested_" + System.currentTimeMillis();
        context.putData("ingestedData", ingestedData);

        log.info("✅ 数据摄入完成: data={}", ingestedData);
    }
}

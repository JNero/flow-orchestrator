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
 * 选择算法节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.CPU,
        dependsOn = {MergeData.class},
        chooser = SelectAlgorithmBranchChooser.class
)
public class SelectAlgorithm implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始选择算法");

        // 根据用户ID选择算法
        String userId = contextInfo.getUserId();
        String selectedAlgorithm;

        switch (userId) {
            case "b":
                selectedAlgorithm = "blackBox";  // b走黑盒排序
                break;
            case "c":
                selectedAlgorithm = "whiteBox";  // c走白盒排序
                break;
            default:
                selectedAlgorithm = "whiteBox";  // 默认白盒排序
                break;
        }

        context.putData("selectedAlgorithm", selectedAlgorithm);

        log.info("✅ 算法选择完成: algorithm={}, userId={}", selectedAlgorithm, userId);
    }
}

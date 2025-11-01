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
 * 检查开关节点 - 起始节点
 */
@Slf4j
@Component
@NodeConfig(
        workflow = WorkflowNames.AD_RANKING,
        type = NodeType.IO,
        start = true,
        chooser = CheckSwitchBranchChooser.class
)
public class CheckSwitch implements Node<ExampleContext> {

    @Override
    public void execute(ExampleContext contextInfo, DagContext context, String nodeId) throws Exception {
        log.info("🚀 开始检查开关状态");

        String userId = contextInfo.getUserId();
        boolean isEnabled;

        // 基于userId决定开关状态
        switch (userId) {
            case "a":
                isEnabled = false;  // a走默认排序
                break;
            case "b":
                isEnabled = true;   // b走白盒排序
                break;
            case "c":
                isEnabled = true;   // c走白盒排序
                break;
            default:
                isEnabled = true;   // 默认开启
                break;
        }

        context.putData("switchEnabled", isEnabled);

        log.info("✅ 开关检查完成: enabled={}, userId={}", isEnabled, userId);
    }
}

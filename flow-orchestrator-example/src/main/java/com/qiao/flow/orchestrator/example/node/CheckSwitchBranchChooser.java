package com.qiao.flow.orchestrator.example.node;

import com.qiao.flow.orchestrator.core.dag.callback.IChoose;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 检查开关分支选择器
 */
@Component
public class CheckSwitchBranchChooser implements IChoose<ExampleContext> {

    @Override
    public Set<Class<? extends Node<?>>> chooseNext(ExampleContext input, DagContext context) {
        // 根据开关状态选择分支
        Boolean switchEnabled = (Boolean) context.getData("switchEnabled");
        if (switchEnabled != null && switchEnabled) {
            return Set.of(GetVersionInfo.class, ContextDimensionCalculation.class);
        } else {
            return Set.of(DefaultRank.class);
        }
    }
}

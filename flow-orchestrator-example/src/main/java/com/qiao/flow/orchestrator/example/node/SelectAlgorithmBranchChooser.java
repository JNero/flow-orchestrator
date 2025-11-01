package com.qiao.flow.orchestrator.example.node;

import com.qiao.flow.orchestrator.core.dag.callback.IChoose;
import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 选择算法分支选择器
 */
@Component
public class SelectAlgorithmBranchChooser implements IChoose<ExampleContext> {

    @Override
    public Set<Class<? extends Node<?>>> chooseNext(ExampleContext input, DagContext context) {
        // 根据算法选择结果选择分支
        String selectedAlgorithm = (String) context.getData("selectedAlgorithm");
        if ("whiteBox".equals(selectedAlgorithm)) {
            return Set.of(WhiteBoxRank.class);
        } else if ("blackBox".equals(selectedAlgorithm)) {
            return Set.of(BlackBoxRank.class);
        } else {
            // 默认走白盒排序
            return Set.of(WhiteBoxRank.class);
        }
    }
}

package com.qiao.flow.orchestrator.core.dag.callback;

import com.qiao.flow.orchestrator.core.dag.context.DagContext;
import com.qiao.flow.orchestrator.core.dag.node.Node;

import java.util.Set;

/**
 * 选择接口
 */
public interface IChoose<T> {

    Set<Class<? extends Node<?>>> chooseNext(T input, DagContext context);
}

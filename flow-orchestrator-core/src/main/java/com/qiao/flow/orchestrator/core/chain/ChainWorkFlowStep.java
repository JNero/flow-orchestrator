package com.qiao.flow.orchestrator.core.chain;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author qiao
 * @date 2025/5/23
 */
@Getter
public class ChainWorkFlowStep<T> {
    private final String name;
    private final Function<T, Boolean> action;
    private final Consumer<Exception> exceptionHandler;

    public ChainWorkFlowStep(String name, Function<T, Boolean> action, Consumer<Exception> exceptionHandler) {
        this.name = name;
        this.action = action;
        this.exceptionHandler = exceptionHandler;
    }
}

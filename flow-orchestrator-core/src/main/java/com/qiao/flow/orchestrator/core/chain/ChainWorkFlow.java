package com.qiao.flow.orchestrator.core.chain;

/**
 * @author qiao
 * @date 2025/5/23
 */

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class ChainWorkFlow<T> {
    private final List<ChainWorkFlowStep<T>> steps = new ArrayList<>();

    public static <T> ChainWorkFlow<T> create() {
        return new ChainWorkFlow<>();
    }

    public ChainWorkFlow<T> addStep(String name, Consumer<T> action) {
        return addConditionalStep(name, ctx -> {
            action.accept(ctx);
            return true;
        });
    }

    public ChainWorkFlow<T> addBranch(String name,
                                      Predicate<T> condition,
                                      ThrowingConsumer<T> ifTrue,
                                      ThrowingConsumer<T> ifFalse) {
        return addConditionalStep(name, ctx -> {
            if (condition.test(ctx)) {
                ifTrue.accept(ctx);
            } else {
                ifFalse.accept(ctx);
            }
            return true;
        });
    }

    public ChainWorkFlow<T> addMultiBranch(String name, Function<T, String> branchSelector,
                                           Map<String, ThrowingConsumer<T>> branches) {
        return addConditionalStep(name, ctx -> {
            String branch = branchSelector.apply(ctx);
            ThrowingConsumer<T> action = branches.get(branch);
            if (action != null) {
                action.accept(ctx);
                return true;
            } else {
                throw new IllegalStateException("Unknown branch: " + branch);
            }
        });
    }

    public ChainWorkFlow<T> addStepWithException(String name, ThrowingConsumer<T> action) {
        return addConditionalStep(name, ctx -> {
            action.accept(ctx);
            return true;
        });
    }

    public ChainWorkFlow<T> addConditionalStep(String name, ThrowingFunction<T, Boolean> condition) {
        return addConditionalStep(name, condition, null);
    }

    public ChainWorkFlow<T> addConditionalStep(String name, ThrowingFunction<T, Boolean> condition, Consumer<Exception> exceptionHandler) {
        return addConditionalStep(name, condition, ctx -> {
        }, ctx -> {
        }, exceptionHandler);
    }

    public ChainWorkFlow<T> addConditionalStep(String name, ThrowingFunction<T, Boolean> condition,
                                               Consumer<T> trueAction, Consumer<T> falseAction) {
        return addConditionalStep(name, condition, trueAction, falseAction, null);
    }

    public ChainWorkFlow<T> addConditionalStep(String name, ThrowingFunction<T, Boolean> condition,
                                               Consumer<T> trueAction, Consumer<T> falseAction,
                                               Consumer<Exception> exceptionHandler) {
        ThrowingFunction<T, Boolean> combinedAction = ctx -> {
            boolean result = condition.apply(ctx);
            if (result) {
                trueAction.accept(ctx);
            } else {
                falseAction.accept(ctx);
            }
            return result;  // 返回条件的结果，决定是否继续执行后续步骤
        };
        steps.add(new ChainWorkFlowStep<>(name, wrapThrowingFunction(combinedAction), exceptionHandler));
        return this;
    }

    private Function<T, Boolean> wrapThrowingFunction(ThrowingFunction<T, Boolean> action) {
        return ctx -> {
            try {
                return action.apply(ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
}


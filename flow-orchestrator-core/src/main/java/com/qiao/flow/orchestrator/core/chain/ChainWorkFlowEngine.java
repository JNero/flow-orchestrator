package com.qiao.flow.orchestrator.core.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author qiao
 * @date 2025/5/23
 */
@Slf4j
@Component
public class ChainWorkFlowEngine {

    public <T> void execute(ChainWorkFlow<T> workflow, T context, TriConsumer<Exception, String, T> exceptionHandler) {
        for (ChainWorkFlowStep<T> step : workflow.getSteps()) {
            String stepName = step.getName();
            try {
                long startTime = System.currentTimeMillis();
                boolean shouldContinue = step.getAction().apply(context);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                log.info("Step {} completed in {} ms", stepName, duration);

                if (!shouldContinue) {
                    log.info("WorkFlow stopped at step: {}", stepName);
                    return;
                }
            } catch (Exception e) {
                log.warn("Step {} failed: {}", stepName, e.getMessage());
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e, stepName, context);
                } else {
                    throw new RuntimeException("Error in step '" + stepName + "'", e);
                }
                return;
            }
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}

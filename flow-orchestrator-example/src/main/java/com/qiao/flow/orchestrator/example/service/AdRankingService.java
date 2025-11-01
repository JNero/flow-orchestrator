package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.dag.runner.DagAutoRunner;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdRankingService {

    @Autowired
    DagAutoRunner dagAutoRunner;

    public String execute(String userId) {
        long startTime = System.currentTimeMillis();
        log.info("AdRankingService execute started");

        ExampleContext contextInfo = new ExampleContext();
        contextInfo.setUserId(userId);
        String level = "";

        // 使用图级别回调替代原来的方法调用
        dagAutoRunner.executeWorkflow("adRanking", contextInfo,
                // 异常处理器
                this::handleException,
                () -> initContext(contextInfo),
                () -> {
                    contextConvertResponse(contextInfo);
                    if (validateRecord(contextInfo)) {
                        recordingContext(contextInfo, startTime);
                    }
                });

        long endTime = System.currentTimeMillis();
        log.info("AdRankingService execute completed successfully, execution time: {}ms", endTime - startTime);

        level = contextInfo.getUserLevel();
        return level;
    }

    /**
     * 处理异常
     */
    private void handleException(Exception exception, ExampleContext input, Object dagContext) {
        log.error("DAG execution failed", exception);
        // 这里可以添加具体的异常处理逻辑
    }

    /**
     * 初始化上下文
     */
    private void initContext(ExampleContext contextInfo) {
        log.info("Initializing context");
        // 这里可以添加具体的初始化逻辑
        // 根据业务需求设置上下文信息，但不要覆盖传入的userId
        contextInfo.setUserName("defaultUserName");
    }

    /**
     * 上下文转换为响应
     */
    private void contextConvertResponse(ExampleContext contextInfo) {
        log.info("Converting context to response");
        // 这里可以添加具体的转换逻辑
    }

    /**
     * 验证记录
     */
    private boolean validateRecord(ExampleContext contextInfo) {

        // 这里可以添加具体的验证逻辑
        return true;
    }

    /**
     * 记录上下文
     */
    private void recordingContext(ExampleContext contextInfo, long startTime) {
        long endTime = System.currentTimeMillis();
        log.info("Recording context - execution time: {}ms", endTime - startTime);
        // 这里可以添加具体的记录逻辑
    }
}

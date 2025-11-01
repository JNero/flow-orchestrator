package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.chain.ChainWorkFlow;
import com.qiao.flow.orchestrator.core.chain.ChainWorkFlowEngine;
import com.qiao.flow.orchestrator.example.entity.RankContextInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 默认排名业务服务实现
 * 展示 Chain 工作流的各种用法
 *
 * @author qiao
 * @date 2025/5/23
 */
@Slf4j
@Service
public class DefaultRankBizServiceImpl implements RankBizService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    @Override
    public void productRank() {
        long startTime = System.currentTimeMillis();

        ChainWorkFlow<RankContextInfo> chainWorkFlow = createDefaultWorkFlow(startTime);
        RankContextInfo rankContextInfo = new RankContextInfo("user123", "req456");
        chainWorkFlowEngine.execute(chainWorkFlow, rankContextInfo, this::handleException);
    }

    private void handleException(Exception e, String stepName, RankContextInfo rankContextInfo) {
        log.error("处理失败: {} - {}", stepName, e.getMessage());
    }

    protected ChainWorkFlow<RankContextInfo> createDefaultWorkFlow(long startTime) {
        return ChainWorkFlow.<RankContextInfo>create()
                .addStep("初始化上下文", this::initContext)  // 使用 Consumer<T> 版本
                .addStepWithException("获取基础信息", this::getBasicInfo)  // 使用 Consumer<T> 版本
                .addConditionalStep("自定义步骤", ctx -> true)
                .addConditionalStep("checkSwitch",
                        this::checkSwitch,
                        ctx -> {
                            // 当 checkSwitch 返回 true 时执行的操作
                            log.info("Switch is on, proceeding with normal flow");
                        },
                        ctx -> {
                            // 当 checkSwitch 返回 false 时执行的操作
                            log.info("Switch is off, using default response");
                            defaultMethod();
                            // 不需要做任何特殊处理，工作流会在这里自动结束
                        }
                )
                .addConditionalStep("异常处理步骤",
                        ctx -> {
                            riskyOperation(ctx);
                            return true;
                        },
                        e -> log.error("处理异常: {}", e.getMessage())
                ).addBranch("选择排序处理方式",
                        RankContextInfo::isBlackScene,
                        this::blackBoxRankProcess,
                        this::whiteBoxRank
                ).addMultiBranch("复杂排序处理",
                        ctx -> {
                            if (ctx.isBlackScene()) return "BLACK";
                            return "DEFAULT";
                        },
                        Map.of(
                                "BLACK", this::blackBoxRankProcess,
                                "DEFAULT", this::whiteBoxRank)
                );
    }

    private void defaultMethod() {
        log.info("defaultMethod");
    }

    private void whiteBoxRank(RankContextInfo rankContextInfo) throws Exception {
        log.info("whiteBoxRank");
        throw new Exception("");
    }

    private void blackBoxRankProcess(RankContextInfo rankContextInfo) {
        log.info("blackBoxRankProcess");
    }

    private void riskyOperation(RankContextInfo ctx) {
        log.info("riskyOperation");
    }

    private void getBasicInfo(RankContextInfo ctx) throws Exception {
        log.info("getBasicInfo");
//        throw new RuntimeException();
    }

    private Boolean checkSwitch(RankContextInfo rankContextInfo) {
        log.info("checkSwitch");
        return true;
    }

    protected void initContext(RankContextInfo ctx) {
        System.out.println("initContext");
    }
}

package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.chain.ChainWorkFlow;
import com.qiao.flow.orchestrator.core.chain.ChainWorkFlowEngine;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 通知服务
 * 展示通知处理的 Chain 工作流
 *
 * @author qiao
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    /**
     * 发送通知
     */
    public void sendNotification(ExampleContext context) {
        log.info("📢 开始发送通知: {}", context.getRequestId());

        ChainWorkFlow<ExampleContext> notificationChain = ChainWorkFlow.<ExampleContext>create()
                .addStep("准备通知内容", this::prepareNotification)
                .addStep("选择通知渠道", this::selectNotificationChannels)
                .addConditionalStep("用户偏好检查",
                        ctx -> "VIP".equals(ctx.getUserLevel()),
                        ctx -> {
                            log.info("VIP用户，使用优先渠道");
                            ctx.setBusinessData("PRIORITY_CHANNEL");
                        },
                        ctx -> {
                            log.info("普通用户，使用标准渠道");
                            ctx.setBusinessData("STANDARD_CHANNEL");
                        }
                )
                .addMultiBranch("多渠道发送",
                        ctx -> {
                            if ("PRIORITY_CHANNEL".equals(ctx.getBusinessData())) {
                                return "PRIORITY";
                            }
                            return "STANDARD";
                        },
                        Map.of(
                                "PRIORITY", this::sendPriorityNotification,
                                "STANDARD", this::sendStandardNotification
                        )
                )
                .addStep("记录发送结果", this::recordNotificationResult);

        chainWorkFlowEngine.execute(notificationChain, context, this::handleException);

        log.info("✅ 通知发送完成: {}", context.getRequestId());
    }

    // 通知处理步骤方法
    private void prepareNotification(ExampleContext ctx) {
        log.info("📝 准备通知内容");
        String content = "用户 " + ctx.getUserId() + " 的通知内容";
        ctx.setBusinessData(content);
        log.info("通知内容: {}", content);
    }

    private void selectNotificationChannels(ExampleContext ctx) {
        log.info("📡 选择通知渠道");
        // 模拟选择通知渠道的逻辑
        String channels = "EMAIL,SMS,PUSH";
        ctx.setBusinessData(channels);
        log.info("选择的渠道: {}", channels);
    }

    private void sendPriorityNotification(ExampleContext ctx) {
        log.info("⭐ 发送优先通知");
        // 优先通知的特殊处理
        ctx.setBusinessData("PRIORITY_SENT");
        log.info("优先通知发送完成");
    }

    private void sendStandardNotification(ExampleContext ctx) {
        log.info("📤 发送标准通知");
        // 标准通知的处理
        ctx.setBusinessData("STANDARD_SENT");
        log.info("标准通知发送完成");
    }

    private void recordNotificationResult(ExampleContext ctx) {
        log.info("📊 记录通知发送结果");
        String result = (String) ctx.getBusinessData();
        if (result != null) {
            log.info("通知发送结果: {}", result);
        }
    }

    private void handleException(Exception e, String stepName, ExampleContext context) {
        log.error("通知发送失败 - 步骤: {}, 请求ID: {}, 错误: {}",
                stepName, context.getRequestId(), e.getMessage());
    }
}

package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.chain.ChainWorkFlow;
import com.qiao.flow.orchestrator.core.chain.ChainWorkFlowEngine;
import com.qiao.flow.orchestrator.example.entity.PaymentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付处理服务
 * 展示支付处理的 Chain 工作流
 *
 * @author qiao
 */
@Slf4j
@Service
public class PaymentProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    /**
     * 处理支付
     */
    public void processPayment(PaymentContext context) {
        log.info("💳 开始处理支付: {}", context.getPaymentId());

        ChainWorkFlow<PaymentContext> paymentChain = ChainWorkFlow.<PaymentContext>create()
                .addStep("验证支付信息", this::validatePayment)
                .addStep("风险评估", this::riskAssessment)
                .addConditionalStep("验证码检查",
                        ctx -> ctx.isNeedVerification(),
                        ctx -> {
                            log.info("需要验证码验证");
                            this.sendVerificationCode(ctx);
                        },
                        ctx -> {
                            log.info("无需验证码");
                        }
                )
                .addBranch("风险处理",
                        ctx -> ctx.isHighRisk(),
                        this::handleHighRiskPayment,
                        this::handleNormalPayment
                )
                .addMultiBranch("支付方式处理",
                        ctx -> ctx.getPaymentMethod(),
                        Map.of(
                                "CREDIT_CARD", this::processCreditCard,
                                "ALIPAY", this::processAlipay,
                                "WECHAT", this::processWechat,
                                "BANK_TRANSFER", this::processBankTransfer
                        )
                )
                .addStep("记录支付结果", this::recordPaymentResult);

        chainWorkFlowEngine.execute(paymentChain, context, this::handleException);

        log.info("✅ 支付处理完成: {} - 状态: {}", context.getPaymentId(), context.getPaymentStatus());
    }

    // 支付处理步骤方法
    private void validatePayment(PaymentContext ctx) {
        log.info("🔍 验证支付信息");
        if (ctx.getPaymentId() == null || ctx.getPaymentId().isEmpty()) {
            throw new IllegalArgumentException("支付ID不能为空");
        }
        if (ctx.getAmount() == null || ctx.getAmount() <= 0) {
            throw new IllegalArgumentException("支付金额必须大于0");
        }
        if (ctx.getPaymentMethod() == null || ctx.getPaymentMethod().isEmpty()) {
            throw new IllegalArgumentException("支付方式不能为空");
        }
        log.info("✅ 支付信息验证通过");
    }

    private void riskAssessment(PaymentContext ctx) {
        log.info("🛡️ 进行风险评估");
        // 模拟风险评估逻辑
        boolean isHighRisk = ctx.getAmount() > 5000 || Math.random() < 0.1;
        ctx.setHighRisk(isHighRisk);

        boolean needVerification = ctx.getAmount() > 1000 || isHighRisk;
        ctx.setNeedVerification(needVerification);

        log.info("风险评估结果 - 高风险: {}, 需要验证: {}", isHighRisk, needVerification);
    }

    private void sendVerificationCode(PaymentContext ctx) {
        log.info("📱 发送验证码到用户手机");
        // 模拟发送验证码
        ctx.setPaymentStatus("WAITING_VERIFICATION");
    }

    private void handleHighRiskPayment(PaymentContext ctx) {
        log.info("⚠️ 处理高风险支付");
        ctx.setPaymentStatus("HIGH_RISK_REVIEW");
        // 高风险支付的特殊处理
    }

    private void handleNormalPayment(PaymentContext ctx) {
        log.info("✅ 处理正常支付");
        ctx.setPaymentStatus("PROCESSING");
    }

    private void processCreditCard(PaymentContext ctx) {
        log.info("💳 处理信用卡支付");
        ctx.setPaymentChannel("CREDIT_CARD_CHANNEL");
        ctx.setPaymentResult("SUCCESS");
    }

    private void processAlipay(PaymentContext ctx) {
        log.info("🟦 处理支付宝支付");
        ctx.setPaymentChannel("ALIPAY_CHANNEL");
        ctx.setPaymentResult("SUCCESS");
    }

    private void processWechat(PaymentContext ctx) {
        log.info("🟢 处理微信支付");
        ctx.setPaymentChannel("WECHAT_CHANNEL");
        ctx.setPaymentResult("SUCCESS");
    }

    private void processBankTransfer(PaymentContext ctx) {
        log.info("🏦 处理银行转账");
        ctx.setPaymentChannel("BANK_CHANNEL");
        ctx.setPaymentResult("PENDING");
    }

    private void recordPaymentResult(PaymentContext ctx) {
        log.info("📝 记录支付结果");
        if ("SUCCESS".equals(ctx.getPaymentResult())) {
            ctx.setPaymentStatus("COMPLETED");
        } else if ("PENDING".equals(ctx.getPaymentResult())) {
            ctx.setPaymentStatus("PENDING");
        } else {
            ctx.setPaymentStatus("FAILED");
            ctx.setFailureReason("支付处理失败");
        }
    }

    private void handleException(Exception e, String stepName, PaymentContext context) {
        log.error("支付处理失败 - 步骤: {}, 支付ID: {}, 错误: {}",
                stepName, context.getPaymentId(), e.getMessage());
        context.setPaymentStatus("FAILED");
        context.setFailureReason("处理异常: " + e.getMessage());
    }
}

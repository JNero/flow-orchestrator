package com.qiao.flow.orchestrator.example.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付上下文信息
 * 用于支付处理 Chain 示例
 *
 * @author qiao
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PaymentContext extends ExampleContext {

    /**
     * 支付ID
     */
    private String paymentId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 支付金额
     */
    private Double amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 支付渠道
     */
    private String paymentChannel;

    /**
     * 是否需要验证码
     */
    private boolean needVerification;

    /**
     * 是否高风险交易
     */
    private boolean highRisk;

    /**
     * 支付结果
     */
    private String paymentResult;

    /**
     * 失败原因
     */
    private String failureReason;

    public PaymentContext() {
        super();
    }

    public PaymentContext(String userId, String requestId) {
        super(userId, requestId);
    }

    public boolean isNeedVerification() {
        return needVerification;
    }

    public void setNeedVerification(boolean needVerification) {
        this.needVerification = needVerification;
    }

    public boolean isHighRisk() {
        return highRisk;
    }

    public void setHighRisk(boolean highRisk) {
        this.highRisk = highRisk;
    }
}

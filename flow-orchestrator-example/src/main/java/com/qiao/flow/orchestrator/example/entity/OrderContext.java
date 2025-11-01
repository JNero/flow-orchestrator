package com.qiao.flow.orchestrator.example.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单上下文信息
 * 用于订单处理 Chain 示例
 *
 * @author qiao
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderContext extends ExampleContext {

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 商品列表
     */
    private String products;

    /**
     * 订单金额
     */
    private Double amount;

    /**
     * 折扣金额
     */
    private Double discountAmount;

    /**
     * 最终金额
     */
    private Double finalAmount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 是否VIP订单
     */
    private boolean vipOrder;

    /**
     * 是否需要发票
     */
    private boolean needInvoice;

    public OrderContext() {
        super();
    }

    public OrderContext(String userId, String requestId) {
        super(userId, requestId);
    }

    public boolean isVipOrder() {
        return vipOrder;
    }

    public void setVipOrder(boolean vipOrder) {
        this.vipOrder = vipOrder;
    }

    public boolean isNeedInvoice() {
        return needInvoice;
    }

    public void setNeedInvoice(boolean needInvoice) {
        this.needInvoice = needInvoice;
    }
}

package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.chain.ChainWorkFlow;
import com.qiao.flow.orchestrator.core.chain.ChainWorkFlowEngine;
import com.qiao.flow.orchestrator.example.entity.OrderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 订单处理服务
 * 展示订单处理的 Chain 工作流
 *
 * @author qiao
 */
@Slf4j
@Service
public class OrderProcessingService {

    @Autowired
    private ChainWorkFlowEngine chainWorkFlowEngine;

    /**
     * 处理订单
     */
    public void processOrder(OrderContext context) {
        log.info("🛒 开始处理订单: {}", context.getOrderId());

        ChainWorkFlow<OrderContext> orderChain = ChainWorkFlow.<OrderContext>create()
                .addStep("验证订单信息", this::validateOrder)
                .addStep("计算订单金额", this::calculateAmount)
                .addConditionalStep("VIP折扣检查",
                        ctx -> ctx.isVipOrder(),
                        ctx -> {
                            log.info("VIP用户，应用额外折扣");
                            ctx.setDiscountAmount(ctx.getAmount() * 0.1);
                        },
                        ctx -> {
                            log.info("普通用户，无额外折扣");
                            ctx.setDiscountAmount(0.0);
                        }
                )
                .addStep("计算最终金额", this::calculateFinalAmount)
                .addBranch("选择处理方式",
                        ctx -> ctx.getAmount() > 1000,
                        this::processHighValueOrder,
                        this::processNormalOrder
                )
                .addMultiBranch("发票处理",
                        ctx -> {
                            if (ctx.isNeedInvoice()) return "INVOICE";
                            return "NO_INVOICE";
                        },
                        Map.of(
                                "INVOICE", this::generateInvoice,
                                "NO_INVOICE", this::skipInvoice
                        )
                )
                .addStep("更新订单状态", this::updateOrderStatus);

        chainWorkFlowEngine.execute(orderChain, context, this::handleException);

        log.info("✅ 订单处理完成: {} - 状态: {}", context.getOrderId(), context.getOrderStatus());
    }

    // 订单处理步骤方法
    private void validateOrder(OrderContext ctx) {
        log.info("📋 验证订单信息");
        if (ctx.getOrderId() == null || ctx.getOrderId().isEmpty()) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (ctx.getAmount() == null || ctx.getAmount() <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }
        log.info("✅ 订单验证通过");
    }

    private void calculateAmount(OrderContext ctx) {
        log.info("💰 计算订单金额");
        // 模拟计算逻辑
        ctx.setAmount(100.0 + Math.random() * 900); // 100-1000之间的随机金额
        log.info("订单金额: {}", ctx.getAmount());
    }

    private void calculateFinalAmount(OrderContext ctx) {
        log.info("💳 计算最终金额");
        ctx.setFinalAmount(ctx.getAmount() - ctx.getDiscountAmount());
        log.info("最终金额: {} (原价: {}, 折扣: {})",
                ctx.getFinalAmount(), ctx.getAmount(), ctx.getDiscountAmount());
    }

    private void processHighValueOrder(OrderContext ctx) {
        log.info("💎 处理高价值订单 (金额: {})", ctx.getAmount());
        ctx.setOrderStatus("HIGH_VALUE_PROCESSING");
        // 高价值订单的特殊处理逻辑
    }

    private void processNormalOrder(OrderContext ctx) {
        log.info("📦 处理普通订单 (金额: {})", ctx.getAmount());
        ctx.setOrderStatus("NORMAL_PROCESSING");
        // 普通订单的处理逻辑
    }

    private void generateInvoice(OrderContext ctx) {
        log.info("🧾 生成发票");
        ctx.setOrderStatus(ctx.getOrderStatus() + "_WITH_INVOICE");
    }

    private void skipInvoice(OrderContext ctx) {
        log.info("⏭️ 跳过发票生成");
    }

    private void updateOrderStatus(OrderContext ctx) {
        log.info("📝 更新订单状态");
        if (ctx.getOrderStatus() == null) {
            ctx.setOrderStatus("COMPLETED");
        }
    }

    private void handleException(Exception e, String stepName, OrderContext context) {
        log.error("订单处理失败 - 步骤: {}, 订单ID: {}, 错误: {}",
                stepName, context.getOrderId(), e.getMessage());
    }
}

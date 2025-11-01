package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import com.qiao.flow.orchestrator.example.entity.OrderContext;
import com.qiao.flow.orchestrator.example.entity.PaymentContext;
import com.qiao.flow.orchestrator.example.entity.RankContextInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多 Chain 测试类
 * 展示多个 Chain 同时独立工作
 *
 * @author qiao
 */
@Slf4j
@SpringBootTest
public class MultiChainTest {

    @Autowired
    private RankBizService rankBizService;

    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 测试多个 Chain 顺序执行
     */
    @Test
    public void testMultipleChainsSequential() {
        log.info("🧪 === 开始测试多个 Chain 顺序执行 ===");

        try {
            // 1. 执行排名 Chain
            log.info("1️⃣ 执行排名 Chain");
            rankBizService.productRank();

            // 2. 执行订单处理 Chain
            log.info("2️⃣ 执行订单处理 Chain");
            OrderContext orderContext = new OrderContext("user123", "order_req_001");
            orderContext.setOrderId("ORDER_001");
            orderContext.setAmount(500.0);
            orderContext.setVipOrder(true);
            orderContext.setNeedInvoice(true);
            orderProcessingService.processOrder(orderContext);

            // 3. 执行支付处理 Chain
            log.info("3️⃣ 执行支付处理 Chain");
            PaymentContext paymentContext = new PaymentContext("user123", "payment_req_001");
            paymentContext.setPaymentId("PAY_001");
            paymentContext.setOrderId("ORDER_001");
            paymentContext.setAmount(orderContext.getFinalAmount());
            paymentContext.setPaymentMethod("CREDIT_CARD");
            paymentProcessingService.processPayment(paymentContext);

            // 4. 执行通知 Chain
            log.info("4️⃣ 执行通知 Chain");
            ExampleContext notificationContext = new ExampleContext("user123", "notification_req_001");
            notificationContext.setUserLevel("VIP");
            notificationService.sendNotification(notificationContext);

            log.info("✅ 所有 Chain 顺序执行完成");

        } catch (Exception e) {
            log.error("❌ Chain 顺序执行失败", e);
            throw e;
        }
    }

    /**
     * 测试多个 Chain 并发执行
     */
    @Test
    public void testMultipleChainsConcurrent() {
        log.info("🧪 === 开始测试多个 Chain 并发执行 ===");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            // 创建多个异步任务
            CompletableFuture<Void> rankTask = CompletableFuture.runAsync(() -> {
                log.info("🔄 异步执行排名 Chain");
                rankBizService.productRank();
            }, executor);

            CompletableFuture<Void> orderTask = CompletableFuture.runAsync(() -> {
                log.info("🔄 异步执行订单处理 Chain");
                OrderContext orderContext = new OrderContext("user456", "order_req_002");
                orderContext.setOrderId("ORDER_002");
                orderContext.setAmount(800.0);
                orderContext.setVipOrder(false);
                orderContext.setNeedInvoice(false);
                orderProcessingService.processOrder(orderContext);
            }, executor);

            CompletableFuture<Void> paymentTask = CompletableFuture.runAsync(() -> {
                log.info("🔄 异步执行支付处理 Chain");
                PaymentContext paymentContext = new PaymentContext("user789", "payment_req_002");
                paymentContext.setPaymentId("PAY_002");
                paymentContext.setOrderId("ORDER_002");
                paymentContext.setAmount(800.0);
                paymentContext.setPaymentMethod("ALIPAY");
                paymentProcessingService.processPayment(paymentContext);
            }, executor);

            CompletableFuture<Void> notificationTask = CompletableFuture.runAsync(() -> {
                log.info("🔄 异步执行通知 Chain");
                ExampleContext notificationContext = new ExampleContext("user101", "notification_req_002");
                notificationContext.setUserLevel("NORMAL");
                notificationService.sendNotification(notificationContext);
            }, executor);

            // 等待所有任务完成
            CompletableFuture.allOf(rankTask, orderTask, paymentTask, notificationTask).join();

            log.info("✅ 所有 Chain 并发执行完成");

        } catch (Exception e) {
            log.error("❌ Chain 并发执行失败", e);
            throw e;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 测试不同上下文的 Chain 独立工作
     */
    @Test
    public void testIndependentChains() {
        log.info("🧪 === 开始测试独立 Chain 工作 ===");

        try {
            // 创建不同的上下文
            RankContextInfo rankContext = new RankContextInfo("rank_user", "rank_req");
            rankContext.setUserLevel("VIP");
            rankContext.setBlackScene(true);

            OrderContext orderContext = new OrderContext("order_user", "order_req");
            orderContext.setOrderId("ORDER_003");
            orderContext.setAmount(1200.0);
            orderContext.setVipOrder(true);
            orderContext.setNeedInvoice(true);

            PaymentContext paymentContext = new PaymentContext("payment_user", "payment_req");
            paymentContext.setPaymentId("PAY_003");
            paymentContext.setOrderId("ORDER_003");
            paymentContext.setAmount(1200.0);
            paymentContext.setPaymentMethod("WECHAT");

            ExampleContext notificationContext = new ExampleContext("notify_user", "notify_req");
            notificationContext.setUserLevel("PREMIUM");

            // 分别执行不同的 Chain
            log.info("执行排名 Chain (RankContextInfo)");
            rankBizService.productRank();

            log.info("执行订单 Chain (OrderContext)");
            orderProcessingService.processOrder(orderContext);

            log.info("执行支付 Chain (PaymentContext)");
            paymentProcessingService.processPayment(paymentContext);

            log.info("执行通知 Chain (ExampleContext)");
            notificationService.sendNotification(notificationContext);

            log.info("✅ 独立 Chain 测试完成");

        } catch (Exception e) {
            log.error("❌ 独立 Chain 测试失败", e);
            throw e;
        }
    }

    /**
     * 测试 Chain 异常处理
     */
    @Test
    public void testChainExceptionHandling() {
        log.info("🧪 === 开始测试 Chain 异常处理 ===");

        try {
            // 创建一个会导致异常的订单上下文
            OrderContext invalidOrderContext = new OrderContext("error_user", "error_req");
            // 不设置必要的字段，应该会触发异常
            // invalidOrderContext.setOrderId(null); // 故意不设置订单ID

            log.info("执行会导致异常的订单 Chain");
            orderProcessingService.processOrder(invalidOrderContext);

        } catch (Exception e) {
            log.info("✅ 异常被正确捕获: {}", e.getMessage());
        }

        try {
            // 创建一个会导致异常的支付上下文
            PaymentContext invalidPaymentContext = new PaymentContext("error_user", "error_req");
            // 不设置必要的字段，应该会触发异常
            // invalidPaymentContext.setPaymentId(null); // 故意不设置支付ID

            log.info("执行会导致异常的支付 Chain");
            paymentProcessingService.processPayment(invalidPaymentContext);

        } catch (Exception e) {
            log.info("✅ 异常被正确捕获: {}", e.getMessage());
        }

        log.info("✅ Chain 异常处理测试完成");
    }
}

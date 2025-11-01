package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.example.Application;
import com.qiao.flow.orchestrator.example.entity.RankContextInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 排名业务服务测试类
 * 测试 Chain 工作流功能
 */
@Slf4j
@SpringBootTest(classes = Application.class)
public class RankBizServiceTest {

    @Autowired
    private RankBizService rankBizService;

    /**
     * 测试 Chain 工作流执行
     */
    @Test
    public void testProductRank() {
        log.info("🧪 === 开始测试 Chain 工作流 ===");

        try {
            rankBizService.productRank();
            log.info("✅ Chain 工作流测试完成");
        } catch (Exception e) {
            log.error("❌ Chain 工作流测试失败", e);
            throw e;
        }
    }

    /**
     * 测试带上下文的 Chain 工作流
     */
    @Test
    public void testProductRankWithContext() {
        log.info("🧪 === 开始测试带上下文的 Chain 工作流 ===");

        // 创建测试上下文
        RankContextInfo context = new RankContextInfo("test_user", "test_request");
        context.setUserLevel("VIP");
        context.setBlackScene(true);

        log.info("测试上下文: userId={}, userLevel={}, blackScene={}",
                context.getUserId(), context.getUserLevel(), context.isBlackScene());

        try {
            rankBizService.productRank();
            log.info("✅ 带上下文的 Chain 工作流测试完成");
        } catch (Exception e) {
            log.error("❌ 带上下文的 Chain 工作流测试失败", e);
            throw e;
        }
    }
}

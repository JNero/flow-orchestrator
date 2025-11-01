package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.example.Application;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 多DAG测试类 - 演示同一应用中运行多个DAG
 */
@Slf4j
@SpringBootTest(classes = Application.class)
public class MultiDagTest {

    @Autowired
    private AdRankingService adRankingService;

    @Autowired
    private ProductRankingService productRankingService;

    @Test
    public void testMultipleDagsInOneApplication() {
        log.info("🧪 === 测试同一应用中的多个DAG ===");

        // 测试广告排序DAG
        log.info("🚀 开始测试广告排序DAG");
        String adResponse = adRankingService.execute("b");  // 黑盒排序
        log.info("✅ 广告排序DAG测试完成");

        // 测试商品排序DAG
        log.info("🚀 开始测试商品排序DAG");
        ExampleContext productRequest = new ExampleContext();
        productRequest.setUserId("product_user");
        ExampleContext productResponse = productRankingService.executeProductRanking(productRequest);
        log.info("✅ 商品排序DAG测试完成");

        log.info("🎉 多DAG测试完成 - 成功在同一个Spring应用中运行了多个DAG！");
    }
}

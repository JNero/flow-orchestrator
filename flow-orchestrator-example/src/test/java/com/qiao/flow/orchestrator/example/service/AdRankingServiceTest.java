package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.example.Application;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * adRanking模拟服务测试类
 * 测试DAG框架的CPU和IO节点以及分支选择功能
 */
@Slf4j
@SpringBootTest(classes = Application.class)
public class AdRankingServiceTest {

    @Autowired
    AdRankingService adRankingService;

    /**
     * 测试分支A - 默认排序 (userId = "a")
     */
    @Test
    public void testAdRankingBranchA() throws Exception {
        log.info("🧪 === 开始测试分支A - 默认排序 (userId=a) ===");
        String userId = "a";
        String level = adRankingService.execute(userId);
        log.info("level {}", level);
        log.info("✅ 分支A测试完成");
    }

    /**
     * 测试分支B - 黑盒排序 (userId = "b")
     */
    @Test
    public void testAdRankingBranchB() throws Exception {
        log.info("🧪 === 开始测试分支B - 黑盒排序 (userId=b) ===");
        String userId = "b";
        String level = adRankingService.execute(userId);
        log.info("level {}", level);
        log.info("✅ 分支B测试完成");
    }

    /**
     * 测试分支C - 白盒排序 (userId = "c")
     */
    @Test
    public void testAdRankingBranchC() throws Exception {
        log.info("🧪 === 开始测试分支C - 白盒排序 (userId=c) ===");
        String userId = "c";
        String level = adRankingService.execute(userId);
        log.info("level {}", level);
        log.info("✅ 分支C测试完成");
    }

}

package com.qiao.flow.orchestrator.example.service;

import com.qiao.flow.orchestrator.core.dag.runner.DagAutoRunner;
import com.qiao.flow.orchestrator.example.entity.ExampleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 商品排序服务
 */
@Slf4j
@Service
public class ProductRankingService {

    @Autowired
    private DagAutoRunner dagAutoRunner;

    /**
     * 执行商品排序DAG
     */
    public ExampleContext executeProductRanking(ExampleContext request) {
        log.info("ProductRankingService execute started");

        try {
            // 执行商品排序DAG
            dagAutoRunner.executeWorkflow("cruiseProductRanking", request, null, null, null);
            return request;
        } catch (Exception e) {
            log.error("商品排序执行失败", e);
            throw new RuntimeException("商品排序执行失败", e);
        } finally {
            log.info("ProductRankingService execute completed");
        }
    }
}

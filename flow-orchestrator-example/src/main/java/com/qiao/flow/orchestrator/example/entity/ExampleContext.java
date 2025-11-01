package com.qiao.flow.orchestrator.example.entity;

import lombok.Data;

/**
 * 示例上下文信息
 * 用于演示 DAG 框架的使用
 *
 * @author qiao
 */
@Data
public class ExampleContext {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 业务数据
     */
    private Object businessData;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 用户级别
     */
    private String userLevel;

    /**
     * 用户名
     */
    private String userName;

    public ExampleContext() {
        this.createTime = System.currentTimeMillis();
    }

    public ExampleContext(String userId, String requestId) {
        this();
        this.userId = userId;
        this.requestId = requestId;
    }
}

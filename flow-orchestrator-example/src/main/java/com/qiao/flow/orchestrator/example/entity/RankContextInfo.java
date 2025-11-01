package com.qiao.flow.orchestrator.example.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务排名上下文信息
 * 用于 Chain 工作流示例
 *
 * @author qiao
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RankContextInfo extends ExampleContext {

    /**
     * 用户偏好
     */
    private String userPreference;

    /**
     * 偏好评分
     */
    private Double preferenceScore;

    /**
     * 用户行为
     */
    private String userBehavior;

    /**
     * 行为评分
     */
    private Double behaviorScore;

    /**
     * 用户画像
     */
    private String userProfile;

    /**
     * 画像评分
     */
    private Double profileScore;

    /**
     * 推荐分数
     */
    private Double recommendationScore;

    /**
     * 推荐原因
     */
    private String recommendationReason;

    /**
     * 风险分数
     */
    private Double riskScore;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 风险原因
     */
    private String riskReason;

    /**
     * 最终分数
     */
    private Double finalScore;

    /**
     * 最终决策
     */
    private String finalDecision;

    /**
     * 最终原因
     */
    private String finalReason;

    /**
     * 是否黑盒场景
     */
    private boolean blackScene;

    public RankContextInfo() {
        super();
    }

    public RankContextInfo(String userId, String requestId) {
        super(userId, requestId);
    }

    public boolean isBlackScene() {
        return blackScene;
    }

    public void setBlackScene(boolean blackScene) {
        this.blackScene = blackScene;
    }
}

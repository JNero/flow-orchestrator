package com.qiao.flow.orchestrator.example.constants;

/**
 * 工作流名称常量类
 * 统一管理所有工作流名称，便于维护和引用
 *
 * @author qiao
 */
public class WorkflowNames {

    /**
     * 广告排序工作流
     */
    public static final String AD_RANKING = "adRanking";

    /**
     * 邮轮产品排序工作流
     */
    public static final String CRUISE_PRODUCT_RANKING = "cruiseProductRanking";

    /**
     * 客户产品排序工作流
     */
    public static final String CUSTOMER_PRODUCT_RANKING = "customerProductRanking";

    /**
     * 商品推荐工作流
     */
    public static final String PRODUCT_RECOMMENDATION = "productRecommendation";

    /**
     * 内容排序工作流
     */
    public static final String CONTENT_RANKING = "contentRanking";

    // 私有构造函数，防止实例化
    private WorkflowNames() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}


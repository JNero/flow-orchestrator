package com.qiao.flow.orchestrator.starter.config;

import com.qiao.flow.orchestrator.starter.controller.DagVisualizationController;
import com.qiao.flow.orchestrator.starter.visualization.DagMermaidGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DAG WebAuto-configuring类
 * 当应用是Web应用时，Auto-configuringDAG可视化功能
 *
 * @author qiao
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class DagWebAutoConfiguration {

    /**
     * 配置DagMermaidGenerator Bean
     */
    @Bean
    public DagMermaidGenerator dagMermaidGenerator(ApplicationContext applicationContext) {
        return new DagMermaidGenerator(applicationContext);
    }

    /**
     * 配置DagVisualizationController Bean
     */
    @Bean
    public DagVisualizationController dagVisualizationController(DagMermaidGenerator dagMermaidGenerator) {
        return new DagVisualizationController(dagMermaidGenerator);
    }
} 
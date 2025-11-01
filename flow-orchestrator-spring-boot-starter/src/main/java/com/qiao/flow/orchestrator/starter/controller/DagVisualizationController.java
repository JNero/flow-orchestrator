package com.qiao.flow.orchestrator.starter.controller;

import com.qiao.flow.orchestrator.starter.visualization.DagMermaidGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DAG可视化控制器
 * 提供基于Mermaid的DAG图查看功能
 *
 * @author qiao
 */
@Slf4j
@RestController
@RequestMapping("/dag")
public class DagVisualizationController {

    private final DagMermaidGenerator dagMermaidGenerator;

    @Autowired
    public DagVisualizationController(DagMermaidGenerator dagMermaidGenerator) {
        this.dagMermaidGenerator = dagMermaidGenerator;
    }

    /**
     * 获取DAG可视化页面
     */
    @GetMapping("/{workflowName}")
    public ResponseEntity<String> getDagVisualization(@PathVariable String workflowName) {
        try {
            log.info("Generating DAG visualization for: {}", workflowName);

            String htmlContent = dagMermaidGenerator.generateHtmlPage(workflowName);

            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                log.warn("Failed to generate DAG visualization for: {}", workflowName);
                return ResponseEntity.status(404).body("DAG visualization not found");
            }

            log.info("DAG visualization generated successfully for: {}", workflowName);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlContent);

        } catch (Exception e) {
            log.warn("Failed to get DAG visualization for: {}", workflowName, e);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
} 
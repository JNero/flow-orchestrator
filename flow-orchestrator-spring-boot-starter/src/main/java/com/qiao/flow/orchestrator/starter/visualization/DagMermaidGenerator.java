package com.qiao.flow.orchestrator.starter.visualization;

import com.qiao.flow.orchestrator.core.dag.annotation.NodeConfig;
import com.qiao.flow.orchestrator.core.dag.node.Node;
import com.qiao.flow.orchestrator.core.dag.node.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * DAG Mermaid可视化生成器
 * 生成基于Mermaid的HTML页面，支持节点类型、依赖关系等可视化
 */
@Slf4j
public class DagMermaidGenerator {

    private final ApplicationContext applicationContext;

    public DagMermaidGenerator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 生成完整的HTML页面
     */
    public String generateHtmlPage(String workflowName) {
        String mermaidCode = generateMermaidCode(workflowName);
        if (mermaidCode.trim().isEmpty()) {
            log.warn("Failed to generate Mermaid code for workflow: {}", workflowName);
            return "";
        }

        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>DAG Visualization - ").append(workflowName).append("</title>\n");
            html.append("    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@10.6.1/dist/mermaid.min.js\"></script>\n");
            html.append("    <style>\n");
            html.append("        body {\n");
            html.append("            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n");
            html.append("            margin: 0;\n");
            html.append("            padding: 20px;\n");
            html.append("            background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);\n");
            html.append("            min-height: 100vh;\n");
            html.append("        }\n");
            html.append("        .container {\n");
            html.append("            max-width: 1400px;\n");
            html.append("            margin: 0 auto;\n");
            html.append("            background: white;\n");
            html.append("            border-radius: 12px;\n");
            html.append("            box-shadow: 0 20px 40px rgba(0,0,0,0.1);\n");
            html.append("            overflow: hidden;\n");
            html.append("        }\n");
            html.append("        .header {\n");
            html.append("            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);\n");
            html.append("            color: #333;\n");
            html.append("            padding: 30px;\n");
            html.append("            text-align: center;\n");
            html.append("            border-bottom: 1px solid #dee2e6;\n");
            html.append("        }\n");
            html.append("        .header h1 {\n");
            html.append("            margin: 0;\n");
            html.append("            font-size: 2.5em;\n");
            html.append("            font-weight: 300;\n");
            html.append("        }\n");
            html.append("        .header p {\n");
            html.append("            margin: 10px 0 0 0;\n");
            html.append("            opacity: 0.9;\n");
            html.append("            font-size: 1.1em;\n");
            html.append("        }\n");
            html.append("        .content {\n");
            html.append("            padding: 40px;\n");
            html.append("        }\n");
            html.append("        .mermaid {\n");
            html.append("            text-align: center;\n");
            html.append("            margin: 20px 0;\n");
            html.append("        }\n");
            html.append("        .legend {\n");
            html.append("            margin-top: 30px;\n");
            html.append("            padding: 20px;\n");
            html.append("            background: #f8f9fa;\n");
            html.append("            border-radius: 8px;\n");
            html.append("            border-left: 4px solid #34495e;\n");
            html.append("        }\n");
            html.append("        .legend h3 {\n");
            html.append("            margin: 0 0 15px 0;\n");
            html.append("            color: #333;\n");
            html.append("        }\n");
            html.append("        .legend-item {\n");
            html.append("            display: inline-block;\n");
            html.append("            margin: 5px 15px 5px 0;\n");
            html.append("            padding: 8px 12px;\n");
            html.append("            border-radius: 6px;\n");
            html.append("            color: white;\n");
            html.append("            font-size: 0.9em;\n");
            html.append("        }\n");
            html.append("        .cpu-legend { background: #4a90e2; }\n");
            html.append("        .io-legend { background: #f59e0b; }\n");
            html.append("        .chooser-legend { background: #52c41a; }\n");
            html.append("\n");
            html.append("        .strong-dep-legend { \n");
            html.append("            background: linear-gradient(to right, #2c3e50 0%, #2c3e50 80%, transparent 80%, transparent 100%);\n");
            html.append("            background-size: 40px 2px;\n");
            html.append("            background-repeat: repeat-x;\n");
            html.append("            width: 40px;\n");
            html.append("            margin: 0 8px;\n");
            html.append("            height: 2px;\n");
            html.append("        }\n");
            html.append("        .weak-dep-legend { \n");
            html.append("            background: linear-gradient(to right, #7f8c8d 0%, #7f8c8d 20%, transparent 20%, transparent 40%, #7f8c8d 40%, #7f8c8d 60%, transparent 60%, transparent 80%, #7f8c8d 80%, #7f8c8d 100%);\n");
            html.append("            background-size: 30px 2px;\n");
            html.append("            background-repeat: repeat-x;\n");
            html.append("            width: 30px;\n");
            html.append("            margin: 0 8px;\n");
            html.append("            height: 2px;\n");
            html.append("        }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <div class=\"header\">\n");
            html.append("            <h1>DAG可视化</h1>\n");
            html.append("            <p>工作流: ").append(workflowName).append("</p>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"content\">\n");
            html.append("            <div class=\"mermaid\">\n");
            html.append(mermaidCode);
            html.append("            </div>\n");
            html.append("            \n");
            html.append("            <div class=\"legend\">\n");
            html.append("                <h3>图例说明</h3>\n");
            html.append("                <div style=\"display: flex; flex-wrap: wrap; gap: 15px;\">\n");
            html.append("                    <div class=\"legend-item cpu-legend\">CPU节点</div>\n");
            html.append("                    <div class=\"legend-item io-legend\">IO节点</div>\n");
            html.append("                    <div class=\"legend-item chooser-legend\">分支选择器</div>\n");
            html.append("                    <div style=\"display: flex; align-items: center;\">\n");
            html.append("                        <div class=\"strong-dep-legend\"></div>\n");
            html.append("                        <span style=\"margin-left: 10px;\">强依赖</span>\n");
            html.append("                    </div>\n");
            html.append("                    <div style=\"display: flex; align-items: center;\">\n");
            html.append("                        <div class=\"weak-dep-legend\"></div>\n");
            html.append("                        <span style=\"margin-left: 10px;\">弱依赖</span>\n");
            html.append("                    </div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            html.append("    \n");
            html.append("    <script>\n");
            html.append("        mermaid.initialize({\n");
            html.append("            startOnLoad: true,\n");
            html.append("            theme: 'default',\n");
            html.append("            flowchart: {\n");
            html.append("                useMaxWidth: true,\n");
            html.append("                htmlLabels: true\n");
            html.append("            }\n");
            html.append("        });\n");
            html.append("    </script>\n");
            html.append("</body>\n");
            html.append("</html>");

            return html.toString();
        } catch (Exception e) {
            log.warn("Failed to generate HTML page for workflow: {}", workflowName, e);
            return "";
        }
    }

    /**
     * 生成Mermaid代码
     */
    private String generateMermaidCode(String workflowName) {
        try {
            // 1. 获取工作流中的所有节点
            Map<String, Node> workflowNodes = applicationContext.getBeansOfType(Node.class);
            if (workflowNodes.isEmpty()) {
                log.warn("No nodes found for workflow: {}", workflowName);
                return "";
            }

            // 2. 过滤出属于指定工作流的节点
            List<Node<?>> filteredNodes = new ArrayList<>();
            for (Node node : workflowNodes.values()) {
                NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
                if (config != null && workflowName.equals(config.workflow())) {
                    filteredNodes.add(node);
                }
            }

            if (filteredNodes.isEmpty()) {
                log.warn("No nodes found for workflow: {}", workflowName);
                return "";
            }

            // 3. 构建依赖关系
            Map<String, Set<String>> dependencies = new HashMap<>();
            Map<String, Set<String>> weakDependencies = new HashMap<>();
            Map<String, NodeType> nodeTypes = new HashMap<>();
            Map<String, String> choosers = new HashMap<>();

            for (Node node : filteredNodes) {
                NodeConfig config = node.getClass().getAnnotation(NodeConfig.class);
                if (config != null) {
                    // 从Bean获取节点名称
                    String nodeName = getBeanNameFromNode(node);
                    nodeTypes.put(nodeName, config.type());

                    // 构建强依赖关系
                    if (config.dependsOn().length > 0) {
                        dependencies.put(nodeName, getDependencyNamesFromClasses(config.dependsOn()));
                    }

                    // 构建弱依赖关系
                    if (config.weakDependsOn().length > 0) {
                        weakDependencies.put(nodeName, getDependencyNamesFromClasses(config.weakDependsOn()));
                    }

                    // 记录分支选择器
                    if (config.chooser() != NodeConfig.NoChoose.class) {
                        choosers.put(nodeName, config.chooser().getSimpleName());
                    }

                }
            }

            // 4. 生成Mermaid代码
            return generateMermaidCode(workflowName, nodeTypes, dependencies, weakDependencies, choosers);

        } catch (Exception e) {
            log.warn("Failed to generate Mermaid code for workflow: {}", workflowName, e);
            return "";
        }
    }

    /**
     * 生成Mermaid代码
     */
    private String generateMermaidCode(String workflowName, Map<String, NodeType> nodeTypes, Map<String, Set<String>> dependencies, Map<String, Set<String>> weakDependencies, Map<String, String> choosers) {
        StringBuilder mermaid = new StringBuilder();

        // 添加图表类型和标题
        mermaid.append("graph TD\n");
        mermaid.append("    %% DAG Workflow: ").append(workflowName).append("\n");
        mermaid.append("    %% Generated by Rank-Ops Framework\n\n");

        // 定义节点样式 - 使用更成熟的颜色方案
        mermaid.append("    %% Node Styles\n");
        mermaid.append("    classDef cpuNode fill:#4a90e2,stroke:#3a7bc8,stroke-width:1px,color:#fff\n");
        mermaid.append("    classDef ioNode fill:#f59e0b,stroke:#e67e22,stroke-width:1px,color:#fff\n");
        mermaid.append("    classDef chooserNode fill:#52c41a,stroke:#389e0d,stroke-width:1px,color:#fff\n");

        // 添加所有节点
        for (String nodeName : nodeTypes.keySet()) {
            NodeType nodeType = nodeTypes.get(nodeName);
            String nodeClass = getNodeClass(nodeType);
            String nodeLabel = getNodeLabel(nodeName);

            // 检查是否为分支选择器节点
            if (choosers.containsKey(nodeName)) {
                nodeClass = "chooserNode";
            }


            mermaid.append("    ").append(nodeName).append("[\"").append(nodeLabel).append("\"]\n");
            mermaid.append("    class ").append(nodeName).append(" ").append(nodeClass).append("\n");
        }

        mermaid.append("\n");

        // 添加强依赖关系（实线）
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String targetNode = entry.getKey();
            Set<String> sourceNodes = entry.getValue();

            for (String sourceNode : sourceNodes) {
                mermaid.append("    ").append(sourceNode).append(" --> ").append(targetNode).append("\n");
            }
        }

        // 添加弱依赖关系（虚线）
        for (Map.Entry<String, Set<String>> entry : weakDependencies.entrySet()) {
            String targetNode = entry.getKey();
            Set<String> sourceNodes = entry.getValue();

            for (String sourceNode : sourceNodes) {
                mermaid.append("    ").append(sourceNode).append(" -.-> ").append(targetNode).append("\n");
            }
        }

        return mermaid.toString();
    }

    /**
     * 获取节点样式类名
     */
    private String getNodeClass(NodeType nodeType) {
        return switch (nodeType) {
            case CPU -> "cpuNode";
            case IO -> "ioNode";
        };
    }

    /**
     * 获取节点标签
     */
    private String getNodeLabel(String nodeName) {
        return nodeName;
    }

    /**
     * 从Bean获取节点名称
     */
    private String getBeanNameFromNode(Node<?> node) {
        String[] beanNames = applicationContext.getBeanNamesForType(node.getClass());
        if (beanNames.length > 0) {
            return beanNames[0];
        }
        throw new IllegalStateException("无法获取Bean名称: " + node.getClass().getSimpleName());
    }

    /**
     * 从Class数组获取依赖节点名称
     */
    private Set<String> getDependencyNamesFromClasses(Class<? extends Node>[] dependencyClasses) {
        Set<String> dependencies = new HashSet<>();

        for (Class<? extends Node> depClass : dependencyClasses) {
            // 检查是否注册为Bean
            String[] beanNames = applicationContext.getBeanNamesForType(depClass);
            if (beanNames.length > 0) {
                dependencies.add(beanNames[0]);
            } else {
                // 降级到类名转换
                String className = depClass.getSimpleName();
                if (className.endsWith("Node")) {
                    className = className.substring(0, className.length() - 4);
                }
                dependencies.add(Character.toLowerCase(className.charAt(0)) + className.substring(1));
            }
        }

        return dependencies;
    }
} 
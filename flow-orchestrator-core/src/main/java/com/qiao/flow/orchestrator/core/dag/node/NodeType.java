package com.qiao.flow.orchestrator.core.dag.node;

/**
 * 节点类型枚举
 *
 * @author qiao
 */
public enum NodeType {

    /**
     * IO密集型节点
     * 主要用于网络请求、文件操作、数据库查询等IO操作
     * 特点：等待时间较长，CPU占用较少
     */
    IO("IO密集型"),

    /**
     * CPU密集型节点
     * 主要用于计算、数据处理、算法执行等CPU密集型操作
     * 特点：CPU占用较高，执行时间相对较短
     */
    CPU("CPU密集型");

    private final String description;

    NodeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
} 
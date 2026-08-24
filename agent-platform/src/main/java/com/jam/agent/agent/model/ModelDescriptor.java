package com.jam.agent.agent.model;

/** 一个供应商对外提供的模型元数据。 */
public record ModelDescriptor(
        String modelName,
        String displayName,
        boolean supportsImageInput,
        boolean supportsTools) {

    public ModelDescriptor(String modelName, String displayName) {
        this(modelName, displayName, false, true);
    }
}

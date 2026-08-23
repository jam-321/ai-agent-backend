package com.jam.agent.agent.model.protocol;

/** 解析供应商凭据引用，避免各协议适配器重复处理环境变量。 */
public final class ModelCredentials {

    private ModelCredentials() {
    }

    public static String resolve(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        if (!configured.startsWith("env:")) {
            return configured;
        }
        String variableName = configured.substring("env:".length()).trim();
        return variableName.isEmpty() ? null : System.getenv(variableName);
    }

    public static boolean isUsable(String apiKey) {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.toLowerCase().contains("dummy");
    }
}

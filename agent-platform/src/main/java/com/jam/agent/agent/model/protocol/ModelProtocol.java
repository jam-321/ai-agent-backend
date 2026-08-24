package com.jam.agent.agent.model.protocol;

/** 模型供应商使用的请求协议；供应商品牌和协议类型相互独立。 */
public final class ModelProtocol {

    public static final String OPENAI_CHAT_COMPLETIONS = "OPENAI_CHAT_COMPLETIONS";
    public static final String OPENAI_RESPONSES = "OPENAI_RESPONSES";
    public static final String ANTHROPIC_MESSAGES = "ANTHROPIC_MESSAGES";

    private static final String LEGACY_OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    private ModelProtocol() {
    }

    public static String normalize(String protocolType) {
        if (protocolType == null || protocolType.isBlank()) {
            return OPENAI_CHAT_COMPLETIONS;
        }
        String normalized = protocolType.trim().toUpperCase();
        return LEGACY_OPENAI_COMPATIBLE.equals(normalized)
                ? OPENAI_CHAT_COMPLETIONS
                : normalized;
    }
}

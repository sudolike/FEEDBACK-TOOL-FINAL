package com.cen.ai;

import java.util.List;

/**
 * 统一的 AI Chat 客户端抽象。
 *
 * 项目支持多个后端：
 *   - dashscope（阿里通义千问 SDK）
 *   - pai-eas（自有 PAI EAS 部署，OpenAI 兼容 / 原生 predict 两种模式）
 *
 * 切换由配置项 {@code ai.provider} 决定（dashscope / pai-eas）。
 * 调用方只需依赖该接口，不关心底层实现。
 */
public interface AiClient {

    /**
     * 同步对话调用。
     *
     * @param systemPrompt 系统提示，可空
     * @param messages     对话消息列表（含历史 + 当前用户消息）
     * @return 模型回答；调用失败时抛 {@link AiCallException}
     */
    String chat(String systemPrompt, List<AiMessage> messages) throws AiCallException;

    /**
     * 是否就绪（已正确配置 endpoint / token）。
     * 上层用于决定是否走 LLM 路径还是直接走本地兜底。
     */
    boolean isReady();

    /** Provider 标识，用于日志。 */
    String providerName();
}

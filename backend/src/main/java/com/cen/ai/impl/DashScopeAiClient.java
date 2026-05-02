package com.cen.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.cen.ai.AiCallException;
import com.cen.ai.AiClient;
import com.cen.ai.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云通义千问 DashScope SDK 实现。
 *
 * 默认实现：当 {@code ai.provider} 未设置或等于 {@code dashscope} 时启用。
 * 兼容旧逻辑（对话 / 总结 / 数据分析全部走 dashscope SDK）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope", matchIfMissing = true)
public class DashScopeAiClient implements AiClient {

    @Value("${ai-api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen-plus}")
    private String model;

    @Resource
    private Generation generation;

    @PostConstruct
    public void printStartupBanner() {
        log.info("[AI Provider] dashscope ready={} model={} apiKeyLen={}",
                isReady(), model, apiKey == null ? 0 : apiKey.length());
    }

    @Override
    public String chat(String systemPrompt, List<AiMessage> messages) throws AiCallException {
        if (!isReady()) {
            throw new AiCallException("DashScope API key 未配置");
        }
        try {
            List<Message> dsMessages = new ArrayList<>();
            if (StrUtil.isNotBlank(systemPrompt)) {
                dsMessages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build());
            }
            if (messages != null) {
                for (AiMessage m : messages) {
                    dsMessages.add(Message.builder()
                            .role(safeRole(m.getRole()))
                            .content(m.getContent() == null ? "" : m.getContent())
                            .build());
                }
            }
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(dsMessages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.9)
                    .build();
            GenerationResult result = generation.call(param);
            String reply = result.getOutput().getChoices().get(0).getMessage().getContent();
            return reply == null ? "" : reply;
        } catch (Exception e) {
            log.warn("DashScope 调用失败：{}", e.getMessage());
            throw new AiCallException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isReady() {
        return StrUtil.isNotBlank(apiKey);
    }

    @Override
    public String providerName() {
        return "dashscope";
    }

    private String safeRole(String r) {
        if (r == null) return Role.USER.getValue();
        switch (r.toLowerCase()) {
            case "system": return Role.SYSTEM.getValue();
            case "assistant": return Role.ASSISTANT.getValue();
            default: return Role.USER.getValue();
        }
    }
}

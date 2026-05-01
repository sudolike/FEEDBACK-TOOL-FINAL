package com.cen.controller;

import com.cen.ai.AiClient;
import com.cen.ai.AiMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * 通用 AI 对话调试入口（保留作为 smoke-test）。
 * 实际后端调用统一走 {@link AiClient}，由 {@code ai.provider} 配置切换 dashscope / pai-eas。
 */
@RestController
@RequestMapping("ai/")
public class AliAliController {

    @Resource
    private AiClient aiClient;

    /**
     * 简易 AI 测试接口：前端发文本，后端调当前 provider 返回回答。
     */
    @PostMapping(value = "aliTyqw")
    public String send(@RequestBody String content) {
        if (!aiClient.isReady()) {
            return "AI 服务暂未配置，请检查 ai.provider 与对应 endpoint/token";
        }
        try {
            String reply = aiClient.chat(null,
                    Collections.singletonList(AiMessage.user(content == null ? "" : content)));
            return reply == null ? "" : reply;
        } catch (Exception e) {
            return "AI 调用失败：" + e.getMessage();
        }
    }
}

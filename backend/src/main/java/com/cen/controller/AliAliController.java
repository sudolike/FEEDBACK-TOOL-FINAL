package com.cen.controller;

import com.cen.ai.AiClient;
import com.cen.ai.AiMessage;
import com.cen.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 探活：返回当前激活的 provider + 是否就绪。无需 token 即可访问，便于排错。
     * 例：GET /ai/status →
     *   { code:200, data:{ provider:"pai-eas[openai]", ready:true } }
     */
    @GetMapping("status")
    public Result status() {
        Map<String, Object> body = new HashMap<>();
        body.put("provider", aiClient.providerName());
        body.put("ready", aiClient.isReady());
        return Result.success(body);
    }

    /**
     * 探活进阶：实际打一次 LLM，确认网络/认证/解析全链路可用。
     * 例：GET /ai/ping?msg=hi
     */
    @GetMapping("ping")
    public Result ping(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "ping") String msg) {
        Map<String, Object> body = new HashMap<>();
        body.put("provider", aiClient.providerName());
        body.put("ready", aiClient.isReady());
        if (!aiClient.isReady()) {
            body.put("ok", false);
            body.put("error", "provider 未配置就绪");
            return Result.success(body);
        }
        try {
            long t0 = System.currentTimeMillis();
            String reply = aiClient.chat(null,
                    Collections.singletonList(AiMessage.user(msg)));
            body.put("ok", true);
            body.put("latencyMs", System.currentTimeMillis() - t0);
            body.put("replyPreview", reply == null ? "" : (reply.length() > 200 ? reply.substring(0, 200) + "..." : reply));
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
        }
        return Result.success(body);
    }
}

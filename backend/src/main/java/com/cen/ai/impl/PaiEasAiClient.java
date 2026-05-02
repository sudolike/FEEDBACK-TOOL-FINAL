package com.cen.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cen.ai.AiCallException;
import com.cen.ai.AiClient;
import com.cen.ai.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 PAI EAS 自部署模型适配器。
 *
 * 支持两种调用模式（由 {@code ai.eas.mode} 决定）：
 * <ul>
 *   <li>{@code openai}（默认）— OpenAI 兼容 chat/completions 协议，适配 vLLM / TGI / 通义官方蓝图。
 *       会把 endpoint 末尾的 {@code /api/predict/<service>} 自动替换为 {@code /v1/chat/completions}，
 *       请求 body 格式：{@code {model,messages,stream:false,top_p}}。</li>
 *   <li>{@code predict} — PAI EAS 原生 predict 协议。直接 POST 到配置 endpoint，
 *       请求 body 默认为 {@code {prompt,system,history,messages}}（绝大多数官方 LLM 部署模板都接受），
 *       同时把 messages 字段一并传入以兼容自定义 model.py。</li>
 * </ul>
 *
 * Token 鉴权：
 *   PAI EAS 的 token 直接放入 Authorization header（不带 Bearer 前缀）。
 *   兼容场景下，如果首次返回 401 会自动用 Bearer 重试一次。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "pai-eas")
public class PaiEasAiClient implements AiClient {

    @Value("${ai.eas.endpoint:}")
    private String endpoint;

    @Value("${ai.eas.token:}")
    private String token;

    @Value("${ai.eas.model:default}")
    private String model;

    /** openai | predict */
    @Value("${ai.eas.mode:openai}")
    private String mode;

    @Value("${ai.eas.timeoutMs:180000}")
    private int timeoutMs;

    /**
     * 是否开启 Qwen3 / DeepSeek-R1 等思考模式。
     * 默认 false：通过 vLLM 的 chat_template_kwargs.enable_thinking=false 走快速模式，
     * 响应明显更快；当模型不支持该开关时该字段会被 jinja 模板静默忽略，无副作用。
     */
    @Value("${ai.eas.thinking:false}")
    private boolean thinking;

    @Value("${ai.eas.maxTokens:1024}")
    private int maxTokens;

    @PostConstruct
    public void printStartupBanner() {
        // 启动时一次性打印关键参数，便于在 docker logs 里直接确认 provider 是否生效
        String safeEndpoint = endpoint == null || endpoint.length() < 28
                ? endpoint
                : endpoint.substring(0, 28) + "...";
        log.info("[AI Provider] pai-eas ready={} mode={} thinking={} model={} endpoint={} tokenLen={}",
                isReady(), mode, thinking, model, safeEndpoint,
                token == null ? 0 : token.length());
    }

    @Override
    public String chat(String systemPrompt, List<AiMessage> messages) throws AiCallException {
        if (!isReady()) {
            throw new AiCallException("PAI EAS endpoint / token 未配置");
        }
        boolean openai = !"predict".equalsIgnoreCase(mode);
        String url = openai ? toOpenAiUrl(endpoint) : endpoint;
        String body = openai ? buildOpenAiBody(systemPrompt, messages) : buildPredictBody(systemPrompt, messages);

        // 第一次：Authorization 直接放 token
        HttpResp resp = doPost(url, body, token, false);
        if (resp.code == 401 || resp.code == 403) {
            log.warn("PAI EAS 直接 token 鉴权失败 ({})，尝试 Bearer 前缀重试", resp.code);
            resp = doPost(url, body, token, true);
        }
        if (resp.code < 200 || resp.code >= 300) {
            throw new AiCallException("PAI EAS HTTP " + resp.code + ": " + safeBody(resp.body));
        }

        return openai ? parseOpenAiReply(resp.body) : parsePredictReply(resp.body);
    }

    @Override
    public boolean isReady() {
        return StrUtil.isNotBlank(endpoint) && StrUtil.isNotBlank(token);
    }

    @Override
    public String providerName() {
        return "pai-eas[" + ("predict".equalsIgnoreCase(mode) ? "predict" : "openai") + "]";
    }

    /* -------------------- 协议构造 -------------------- */

    /**
     * EAS 上的 vLLM / OpenAI 兼容服务，标准路径是
     *   <endpoint>/v1/chat/completions
     * 这里 endpoint 一般包含 /api/predict/<svc> 前缀（EAS proxy 用它路由到容器），
     * 必须保留前缀并直接追加 /v1/chat/completions（不要剥前缀！）。
     */
    private String toOpenAiUrl(String raw) {
        String u = raw.trim();
        if (u.contains("/v1/chat/completions")) return u;
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u + "/v1/chat/completions";
    }

    private String buildOpenAiBody(String systemPrompt, List<AiMessage> messages) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", false);
        body.put("top_p", 0.9);
        if (maxTokens > 0) body.put("max_tokens", maxTokens);

        // vLLM 扩展：透传 chat_template 参数（Qwen3 thinking switch 等）。
        // 服务端模型若无对应模板字段，会被 jinja 静默忽略，不会报错。
        JSONObject ctk = new JSONObject();
        ctk.put("enable_thinking", thinking);
        body.put("chat_template_kwargs", ctk);

        JSONArray arr = new JSONArray();
        if (StrUtil.isNotBlank(systemPrompt)) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            arr.add(sys);
        }
        if (messages != null) {
            for (AiMessage m : messages) {
                JSONObject mp = new JSONObject();
                mp.put("role", normRole(m.getRole()));
                mp.put("content", m.getContent() == null ? "" : m.getContent());
                arr.add(mp);
            }
        }
        body.put("messages", arr);
        return body.toJSONString();
    }

    /**
     * 原生 predict body：兼容多种官方 / 自定义模板。
     * 同时塞入 prompt / system / history / messages 四种字段，
     * 让接收端按需要的字段提取。
     */
    private String buildPredictBody(String systemPrompt, List<AiMessage> messages) {
        // 历史消息（除最后一条）转成 [["q","a"], ...] 格式
        List<List<String>> history = new ArrayList<>();
        String prompt = "";
        if (messages != null && !messages.isEmpty()) {
            // 最后一条 user 作为 prompt
            AiMessage last = messages.get(messages.size() - 1);
            prompt = last.getContent() == null ? "" : last.getContent();
            // 配对：user -> assistant
            String pendingQ = null;
            for (int i = 0; i < messages.size() - 1; i++) {
                AiMessage m = messages.get(i);
                String r = normRole(m.getRole());
                String c = m.getContent() == null ? "" : m.getContent();
                if ("user".equals(r)) {
                    pendingQ = c;
                } else if ("assistant".equals(r) && pendingQ != null) {
                    List<String> turn = new ArrayList<>();
                    turn.add(pendingQ);
                    turn.add(c);
                    history.add(turn);
                    pendingQ = null;
                }
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        if (StrUtil.isNotBlank(systemPrompt)) {
            body.put("system", systemPrompt);
        }
        body.put("history", history);

        // 一并把 messages 也带上，兼容 OpenAI 风格 server
        JSONArray arr = new JSONArray();
        if (StrUtil.isNotBlank(systemPrompt)) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            arr.add(sys);
        }
        if (messages != null) {
            for (AiMessage m : messages) {
                JSONObject mp = new JSONObject();
                mp.put("role", normRole(m.getRole()));
                mp.put("content", m.getContent() == null ? "" : m.getContent());
                arr.add(mp);
            }
        }
        body.put("messages", arr);
        body.put("top_p", 0.9);
        body.put("temperature", 0.7);
        body.put("stream", false);
        return JSON.toJSONString(body);
    }

    /* -------------------- 响应解析 -------------------- */

    private String parseOpenAiReply(String body) {
        try {
            JSONObject root = JSON.parseObject(body);
            if (root == null) throw new AiCallException("EAS 响应为空");
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiCallException("EAS 响应缺少 choices: " + safeBody(body));
            }
            JSONObject ch0 = choices.getJSONObject(0);
            JSONObject msg = ch0.getJSONObject("message");
            if (msg != null) {
                // 优先取 message.content
                String content = msg.getString("content");
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
                // Qwen3 / DeepSeek-R1 等思考模型在 thinking 模式下，
                // content=null，真正回答放在 reasoning / reasoning_content 字段。
                String reasoning = msg.getString("reasoning");
                if (reasoning != null && !reasoning.trim().isEmpty()) {
                    return reasoning;
                }
                String reasoningContent = msg.getString("reasoning_content");
                if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                    return reasoningContent;
                }
            }
            // 部分 OpenAI 兼容服务把内容放在 ch0.text
            if (ch0.containsKey("text")) return ch0.getString("text");
            throw new AiCallException("EAS 响应无可识别 content: " + safeBody(body));
        } catch (AiCallException e) {
            throw e;
        } catch (Exception e) {
            throw new AiCallException("EAS OpenAI 响应解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 原生 predict 兼容多种返回结构：
     *  - {"response":"..."} （ChatGLM 模板）
     *  - {"output":"..."} 或 {"output":{"text":"..."}}
     *  - {"data":{"output":"..."}} （PAI 蓝图常见包裹）
     *  - {"choices":[{"message":{...}}]} （OpenAI 风格）
     *  - 直接字符串
     */
    private String parsePredictReply(String body) {
        if (body == null) return "";
        String trimmed = body.trim();
        // 直接字符串响应（无 JSON 包裹）
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            JSONObject root = JSON.parseObject(trimmed);
            if (root == null) return trimmed;

            // OpenAI 兼容返回（不少 PAI 蓝图也会返回这种结构）
            if (root.containsKey("choices")) {
                return parseOpenAiReply(trimmed);
            }
            String s = pickString(root, "response", "answer", "reply", "result", "text");
            if (s != null) return s;
            JSONObject output = root.getJSONObject("output");
            if (output != null) {
                String os = pickString(output, "text", "answer", "content");
                if (os != null) return os;
                JSONArray choices = output.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject ch0 = choices.getJSONObject(0);
                    JSONObject msg = ch0.getJSONObject("message");
                    if (msg != null && msg.containsKey("content")) return msg.getString("content");
                }
            }
            JSONObject data = root.getJSONObject("data");
            if (data != null) {
                String ds = pickString(data, "output", "text", "answer", "response", "content", "result");
                if (ds != null) return ds;
            }
            // 兜底：回原文
            log.warn("PAI EAS predict 响应无可识别字段，原样返回：{}", safeBody(trimmed));
            return trimmed;
        } catch (Exception e) {
            log.warn("PAI EAS predict 响应解析失败，原样返回：{}", e.getMessage());
            return trimmed;
        }
    }

    private String pickString(JSONObject obj, String... keys) {
        for (String k : keys) {
            Object v = obj.get(k);
            if (v instanceof String && !((String) v).trim().isEmpty()) {
                return (String) v;
            }
        }
        return null;
    }

    /* -------------------- HTTP -------------------- */

    private static class HttpResp {
        int code; String body;
        HttpResp(int c, String b) { this.code = c; this.body = b; }
    }

    private HttpResp doPost(String url, String json, String authToken, boolean withBearer) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(Math.min(timeoutMs, 30_000));
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            String auth = withBearer ? ("Bearer " + authToken) : authToken;
            conn.setRequestProperty("Authorization", auth);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = is == null ? "" : readStream(is);
            return new HttpResp(code, body);
        } catch (IOException e) {
            throw new AiCallException("PAI EAS 网络异常: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /* -------------------- utils -------------------- */

    private String normRole(String r) {
        if (r == null) return "user";
        switch (r.toLowerCase()) {
            case "system": return "system";
            case "assistant": return "assistant";
            default: return "user";
        }
    }

    private static String safeBody(String s) {
        if (s == null) return "";
        return s.length() > 1000 ? s.substring(0, 1000) + "..." : s;
    }
}

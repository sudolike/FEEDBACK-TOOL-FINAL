package com.cen.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色化对话消息（与 OpenAI / DashScope 通用格式对齐）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    /** system / user / assistant */
    private String role;

    /** 消息正文 */
    private String content;

    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }
}

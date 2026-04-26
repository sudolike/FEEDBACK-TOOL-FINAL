package com.cen.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequestDTO {

    /** 当前用户ID（前端传或从 JWT 取） */
    private Long userId;

    /** 用户角色 student/teacher */
    private String role;

    /** 会话ID（同一个对话保持不变） */
    private String sessionId;

    /** 用户输入 */
    private String message;

    /** 可选：限定课程范围检索 */
    private Long courseId;

    /** 可选：使用场景 chat/summarize/recommend/difficulty */
    private String scene;

    /** 历史消息（前端可携带，避免每次往返查 DB） */
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role;
        private String content;
    }
}

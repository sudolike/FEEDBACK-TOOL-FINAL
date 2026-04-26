package com.cen.controller.dto;

import com.cen.entity.KbChunk;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponseDTO {
    private String sessionId;
    private String reply;
    private List<KbChunk> citations;
    private Long latencyMs;
}

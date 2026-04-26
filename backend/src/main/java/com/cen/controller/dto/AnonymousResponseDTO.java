package com.cen.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷答案对外接口的匿名化包装。
 * 注意：不暴露 studentId 真实值，使用一次性散列后的 anonymousId。
 */
@Data
public class AnonymousResponseDTO {

    private Long id;
    private String anonymousId;
    private String answers;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;
}

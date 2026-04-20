package com.cen.controller.dto;

import com.cen.entity.Questionnaires;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionnaireResponseDTO {
    private Long id;
    private Questionnaires questionnaire;
    private String answers;
    private LocalDateTime submittedAt;
} 
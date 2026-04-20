package com.cen.controller.dto;

import com.cen.entity.Questionnaires;
import lombok.Data;

@Data
public class QuestionnaireWithStatusDTO {
    private Questionnaires questionnaire;
    private Integer status;
    private String statusDescription;
    private Boolean hasSubmitted;
} 
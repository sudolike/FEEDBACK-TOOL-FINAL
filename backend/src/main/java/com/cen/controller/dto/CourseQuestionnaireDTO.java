package com.cen.controller.dto;

import lombok.Data;

@Data
public class CourseQuestionnaireDTO {
    private Long courseId;
    private Long questionnaireId;
    private Integer status;
} 
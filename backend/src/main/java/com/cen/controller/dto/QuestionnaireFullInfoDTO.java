package com.cen.controller.dto;

import com.cen.entity.Courses;
import com.cen.entity.Questionnaires;
import com.cen.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionnaireFullInfoDTO {
    private Questionnaires questionnaire;  // 问卷信息
    private Courses course;  // 课程信息
    private User teacher;  // 教师信息
    private Integer status;  // 问卷状态
    private String statusDescription;  // 状态描述
    private Boolean hasSubmitted;  // 是否已提交
    private LocalDateTime createdAt;  // 创建时间
} 
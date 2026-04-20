package com.cen.controller.dto;

import com.cen.entity.Questionnaires;
import lombok.Data;

@Data
public class QuestionnaireSubmissionStatsDTO {
    private Questionnaires questionnaire;  // 问卷信息
    private Integer status;  // 问卷状态
    private String statusDescription;  // 状态描述
    private Long totalStudents;  // 总学生数
    private Long submittedCount;  // 已提交人数
    private Double submissionRate;  // 提交率
} 
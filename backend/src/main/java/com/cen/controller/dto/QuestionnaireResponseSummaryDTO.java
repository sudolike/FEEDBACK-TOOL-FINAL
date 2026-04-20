package com.cen.controller.dto;

import com.cen.entity.Questionnaires;
import lombok.Data;
import java.util.List;

@Data
public class QuestionnaireResponseSummaryDTO {
    private Questionnaires questionnaire;  // 问卷基本信息
    private Integer totalQuestions;  // 题目总数
    private Integer totalResponses;  // 填写总数
    private Double completionRate;   // 完成率
    private List<String> answers;    // 所有答案数组
} 
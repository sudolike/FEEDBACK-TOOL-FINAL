package com.cen.controller.dto;

import com.cen.entity.QuestionnaireResponses;
import com.cen.entity.User;
import lombok.Data;

@Data
public class QuestionnaireResponseDetailDTO {
    private QuestionnaireResponses response;  // 问卷答案
    private User student;  // 学生信息
} 
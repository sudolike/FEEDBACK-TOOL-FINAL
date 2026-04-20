package com.cen.controller.dto;

import lombok.Data;

@Data
public class CourseStudentDTO {
    private Long courseId;
    private Long studentId;
    private String studentIds;  // 用于批量绑定，逗号分隔的学生ID
} 
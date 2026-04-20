package com.cen.controller.dto;

import com.cen.entity.CourseFeedback;
import com.cen.entity.User;
import lombok.Data;


//将课程反馈信息（CourseFeedback）和提交反馈的学生信息（User）组合在一起进行传输

@Data
public class CourseFeedbackDTO {
    private CourseFeedback feedback;  // 反馈信息
    private User student;  // 学生信息
} 
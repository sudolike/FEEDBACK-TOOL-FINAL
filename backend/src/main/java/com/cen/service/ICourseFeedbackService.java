package com.cen.service;

import com.cen.entity.CourseFeedback;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.controller.dto.CourseFeedbackDTO;
import java.util.List;


public interface ICourseFeedbackService extends IService<CourseFeedback> {
    // 获取课程反馈及用户信息
    List<CourseFeedbackDTO> getCourseFeedbacksWithUser(Long courseId);
}

package com.cen.service.impl;

import com.cen.entity.CourseFeedback;
import com.cen.mapper.CourseFeedbackMapper;
import com.cen.service.ICourseFeedbackService;
import com.cen.mapper.UserMapper;
import com.cen.entity.User;
import com.cen.controller.dto.CourseFeedbackDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
public class CourseFeedbackServiceImpl extends ServiceImpl<CourseFeedbackMapper, CourseFeedback> implements ICourseFeedbackService {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<CourseFeedbackDTO> getCourseFeedbacksWithUser(Long courseId) {
        // 1. 获取课程的所有反馈
        QueryWrapper<CourseFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .orderByDesc("created_at");
        List<CourseFeedback> feedbacks = this.list(queryWrapper);
        
        if (feedbacks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有学生ID
        List<Long> studentIds = feedbacks.stream()
                .map(CourseFeedback::getStudentId)
                .distinct()
                .collect(Collectors.toList());
                
        // 3. 批量查询学生信息
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Long, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, student -> student));
                
        // 4. 组装返回数据
        return feedbacks.stream().map(feedback -> {
            CourseFeedbackDTO dto = new CourseFeedbackDTO();
            dto.setFeedback(feedback);
            dto.setStudent(studentMap.get(feedback.getStudentId()));
            return dto;
        }).collect(Collectors.toList());
    }
}

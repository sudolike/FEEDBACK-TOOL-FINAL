package com.cen.service;

import com.cen.entity.CourseStudents;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cen.entity.User;

import java.util.List;


public interface ICourseStudentsService extends IService<CourseStudents> {

    // 批量绑定学生
    boolean bindStudents(Long courseId, String studentIds);
    
    // 解除单个学生绑定
    boolean unbindStudent(Long courseId, Long studentId);

    // 查询课程关联的学生列表
    List<User> getStudentsByCourseId(Long courseId);
}

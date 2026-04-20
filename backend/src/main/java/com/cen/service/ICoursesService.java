package com.cen.service;

import com.cen.entity.Courses;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ICoursesService extends IService<Courses> {
    // 查询学生关联的课程列表
    List<Courses> getCoursesByStudentId(Long studentId);
}

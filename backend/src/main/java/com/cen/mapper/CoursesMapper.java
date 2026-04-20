package com.cen.mapper;

import com.cen.entity.Courses;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface CoursesMapper extends BaseMapper<Courses> {
    List<Courses> getCoursesByStudentId(Long studentId);

    // 获取课程的学生数量
    @Select("SELECT COUNT(*) FROM sys_course_students WHERE course_id = #{courseId}")
    Long getStudentCountByCourseId(Long courseId);
}

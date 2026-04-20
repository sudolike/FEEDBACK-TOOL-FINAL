package com.cen.service.impl;

import com.cen.entity.CourseStudents;
import com.cen.mapper.CourseStudentsMapper;
import com.cen.service.ICourseStudentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.cen.entity.User;
import com.cen.mapper.UserMapper;


@Service
public class CourseStudentsServiceImpl extends ServiceImpl<CourseStudentsMapper, CourseStudents> implements ICourseStudentsService {

    private final UserMapper userMapper;

    public CourseStudentsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public boolean bindStudents(Long courseId, String studentIds) {
        // 1. 如果学生ID为空，直接返回
        if (StringUtils.isBlank(studentIds)) {
            return true;
        }
        
        // 2. 将学生ID字符串转换为List
        List<Long> sIds = Arrays.stream(studentIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
                
        // 3. 批量创建新的关联关系
        List<CourseStudents> relations = sIds.stream()
                .map(sId -> new CourseStudents(null, courseId, sId, LocalDateTime.now()))
                .collect(Collectors.toList());
                
        // 4. 批量保存
        return this.saveBatch(relations);
    }

    @Override
    @Transactional
    public boolean unbindStudent(Long courseId, Long studentId) {
        QueryWrapper<CourseStudents> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .eq("student_id", studentId);
        return this.remove(queryWrapper);
    }

    @Override
    public List<User> getStudentsByCourseId(Long courseId) {
        return userMapper.getStudentsByCourseId(courseId);
    }
}

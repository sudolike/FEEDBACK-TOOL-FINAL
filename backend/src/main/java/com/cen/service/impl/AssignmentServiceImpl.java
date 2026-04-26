package com.cen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.entity.Assignment;
import com.cen.entity.Courses;
import com.cen.mapper.AssignmentMapper;
import com.cen.mapper.CoursesMapper;
import com.cen.service.IAssignmentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentServiceImpl extends ServiceImpl<AssignmentMapper, Assignment> implements IAssignmentService {

    @Resource private CoursesMapper coursesMapper;

    @Override
    public List<Assignment> listByCourse(Long courseId) {
        QueryWrapper<Assignment> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).orderByDesc("created_at");
        return list(qw);
    }

    @Override
    public List<Assignment> listByStudent(Long studentId) {
        List<Courses> cs = coursesMapper.getCoursesByStudentId(studentId);
        if (cs.isEmpty()) return new ArrayList<>();
        List<Long> ids = cs.stream().map(Courses::getId).collect(Collectors.toList());
        QueryWrapper<Assignment> qw = new QueryWrapper<>();
        qw.in("course_id", ids).eq("status", 1).orderByDesc("created_at");
        return list(qw);
    }
}

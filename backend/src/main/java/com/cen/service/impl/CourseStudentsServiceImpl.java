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
        if (StringUtils.isBlank(studentIds)) {
            return true;
        }

        List<Long> sIds = Arrays.stream(studentIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 兼容老接口：直接绑定 = 已批准的选课关系（来自管理员/教师后台直接关联）
        LocalDateTime now = LocalDateTime.now();
        List<CourseStudents> relations = sIds.stream()
                .map(sId -> {
                    CourseStudents cs = new CourseStudents();
                    cs.setCourseId(courseId);
                    cs.setStudentId(sId);
                    cs.setStatus(CourseStudents.STATUS_APPROVED);
                    cs.setSource(CourseStudents.SOURCE_TEACHER_INVITE);
                    cs.setReviewedAt(now);
                    cs.setCreatedAt(now);
                    return cs;
                })
                .collect(Collectors.toList());

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

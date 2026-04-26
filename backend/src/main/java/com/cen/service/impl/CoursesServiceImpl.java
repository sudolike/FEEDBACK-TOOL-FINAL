package com.cen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.common.Constants;
import com.cen.entity.Courses;
import com.cen.exception.ServiceException;
import com.cen.mapper.CoursesMapper;
import com.cen.service.ICoursesService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class CoursesServiceImpl extends ServiceImpl<CoursesMapper, Courses> implements ICoursesService {

    @Resource
    private CoursesMapper coursesMapper;

    @Override
    public List<Courses> getCoursesByStudentId(Long studentId) {
        return coursesMapper.getCoursesByStudentId(studentId);
    }

    @Override
    public Courses submitCourseProposal(Courses courses, Long actorId) {
        if (courses.getName() == null || courses.getName().trim().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "课程名称不能为空");
        }
        if (courses.getCode() == null || courses.getCode().trim().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "课程代码不能为空");
        }
        if (courses.getTeacherId() == null) {
            courses.setTeacherId(actorId);
        }

        if (courses.getId() != null) {
            Courses existing = getById(courses.getId());
            if (existing == null) {
                throw new ServiceException(Constants.CODE_404, "课程不存在");
            }
            if (Courses.STATUS_APPROVED.equals(existing.getStatus())) {
                throw new ServiceException(Constants.CODE_400,
                        "课程已通过审批，关键信息只能由管理员修改");
            }
            if (existing.getTeacherId() != null
                    && !existing.getTeacherId().equals(actorId)) {
                throw new ServiceException(Constants.CODE_403, "无权修改他人课程");
            }
        }

        courses.setStatus(Courses.STATUS_PENDING);
        courses.setRejectReason(null);
        courses.setReviewedBy(null);
        courses.setReviewedAt(null);
        saveOrUpdate(courses);
        return courses;
    }

    @Override
    public Courses reviewCourse(Long courseId, boolean approve, String reason, Long adminId) {
        Courses c = getById(courseId);
        if (c == null) {
            throw new ServiceException(Constants.CODE_404, "课程不存在");
        }
        if (approve) {
            c.setStatus(Courses.STATUS_APPROVED);
            c.setRejectReason(null);
        } else {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ServiceException(Constants.CODE_400, "驳回必须填写理由");
            }
            c.setStatus(Courses.STATUS_REJECTED);
            c.setRejectReason(reason.trim());
        }
        c.setReviewedBy(adminId);
        c.setReviewedAt(LocalDateTime.now());
        updateById(c);
        return c;
    }
}

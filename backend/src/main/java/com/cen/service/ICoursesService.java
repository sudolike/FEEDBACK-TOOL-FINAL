package com.cen.service;

import com.cen.entity.Courses;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ICoursesService extends IService<Courses> {
    // 查询学生关联的课程列表（仅 status=approved）
    List<Courses> getCoursesByStudentId(Long studentId);

    /**
     * 教师提交课程申请：
     *  - 强制覆盖 status = pending，清空 reject_reason / reviewedBy / reviewedAt
     *  - 若提供了 id，则只允许 pending / rejected 状态的课程被修改
     */
    Courses submitCourseProposal(Courses courses, Long actorId);

    /**
     * 管理员审批课程：
     *  - approve == true → status = approved
     *  - approve == false → status = rejected, 必须传 reason
     */
    Courses reviewCourse(Long courseId, boolean approve, String reason, Long adminId);
}

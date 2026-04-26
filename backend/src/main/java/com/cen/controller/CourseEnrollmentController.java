package com.cen.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Result;
import com.cen.entity.CourseStudents;
import com.cen.entity.Courses;
import com.cen.entity.User;
import com.cen.mapper.CoursesMapper;
import com.cen.mapper.UserMapper;
import com.cen.service.ICourseStudentsService;
import com.cen.utils.AuthContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 选课工作流：学生申请加入 + 教师邀请/审批 + 学生回应邀请。
 *
 * 状态机：
 *   学生主动申请（POST /enrollments/student/apply）→ pending+student_apply
 *     → 教师 approve  → approved
 *     → 教师 reject   → rejected
 *
 *   教师主动邀请（POST /enrollments/teacher/invite）→ pending+teacher_invite
 *     → 学生 accept   → approved
 *     → 学生 decline  → 行被删除
 *
 *   只有 status=approved 的关系才会出现在学生"我的课程"和教师课程的"在册学生"中。
 */
@RestController
@RequestMapping("/enrollments")
public class CourseEnrollmentController {

    @Resource
    private ICourseStudentsService enrollmentService;

    @Resource
    private CoursesMapper coursesMapper;

    @Resource
    private UserMapper userMapper;

    /* ============================================================
     * 学生侧
     * ============================================================ */

    /** 学生申请加入课程 */
    @PostMapping("/student/apply")
    public Result studentApply(@RequestBody Map<String, Object> body) {
        Long me = AuthContext.requireRole("student");
        Long courseId = toLong(body.get("courseId"));
        if (courseId == null) return Result.error(400, "缺少 courseId");
        String message = body.get("message") == null ? null : String.valueOf(body.get("message"));

        Courses course = coursesMapper.selectById(courseId);
        if (course == null) return Result.error(404, "课程不存在");
        if (course.getStatus() != null && !Courses.STATUS_APPROVED.equals(course.getStatus())) {
            return Result.error(403, "课程尚未通过审批，无法申请");
        }

        CourseStudents existing = findRelation(courseId, me);
        if (existing != null) {
            if (CourseStudents.STATUS_APPROVED.equals(existing.getStatus())) {
                return Result.error(400, "你已在该课程中");
            }
            if (CourseStudents.STATUS_PENDING.equals(existing.getStatus())) {
                if (CourseStudents.SOURCE_TEACHER_INVITE.equals(existing.getSource())) {
                    return Result.error(400, "教师已邀请你加入，请到\"邀请\"页接受");
                }
                return Result.error(400, "你已申请，等待教师审批");
            }
            // rejected → 允许重新申请：复用同一行
            existing.setStatus(CourseStudents.STATUS_PENDING);
            existing.setSource(CourseStudents.SOURCE_STUDENT_APPLY);
            existing.setApplyMessage(message);
            existing.setRejectReason(null);
            existing.setReviewedAt(null);
            enrollmentService.updateById(existing);
            return Result.success(existing);
        }

        CourseStudents row = new CourseStudents();
        row.setCourseId(courseId);
        row.setStudentId(me);
        row.setStatus(CourseStudents.STATUS_PENDING);
        row.setSource(CourseStudents.SOURCE_STUDENT_APPLY);
        row.setApplyMessage(message);
        row.setCreatedAt(LocalDateTime.now());
        enrollmentService.save(row);
        return Result.success(row);
    }

    /** 学生：撤回自己未处理的申请 / 拒绝教师的邀请 */
    @PostMapping("/student/cancel/{id}")
    public Result studentCancel(@PathVariable Long id) {
        Long me = AuthContext.requireRole("student");
        CourseStudents row = enrollmentService.getById(id);
        if (row == null) return Result.error(404, "记录不存在");
        if (!Objects.equals(row.getStudentId(), me)) return Result.error(403, "无权操作");
        if (CourseStudents.STATUS_APPROVED.equals(row.getStatus())) {
            return Result.error(400, "已加入的课程请使用退课");
        }
        enrollmentService.removeById(id);
        return Result.success();
    }

    /** 学生：接受教师邀请 */
    @PostMapping("/student/accept/{id}")
    public Result studentAccept(@PathVariable Long id) {
        Long me = AuthContext.requireRole("student");
        CourseStudents row = enrollmentService.getById(id);
        if (row == null) return Result.error(404, "记录不存在");
        if (!Objects.equals(row.getStudentId(), me)) return Result.error(403, "无权操作");
        if (!CourseStudents.SOURCE_TEACHER_INVITE.equals(row.getSource())
                || !CourseStudents.STATUS_PENDING.equals(row.getStatus())) {
            return Result.error(400, "不是待接受的邀请");
        }
        row.setStatus(CourseStudents.STATUS_APPROVED);
        row.setReviewedAt(LocalDateTime.now());
        enrollmentService.updateById(row);
        return Result.success(row);
    }

    /** 学生：主动退出已加入的课程 */
    @PostMapping("/student/leave/{id}")
    public Result studentLeave(@PathVariable Long id) {
        Long me = AuthContext.requireRole("student");
        CourseStudents row = enrollmentService.getById(id);
        if (row == null) return Result.error(404, "记录不存在");
        if (!Objects.equals(row.getStudentId(), me)) return Result.error(403, "无权操作");
        enrollmentService.removeById(id);
        return Result.success();
    }

    /** 学生：我的所有选课关系（含申请/邀请/被拒），可按 status 过滤 */
    @GetMapping("/student/my")
    public Result studentMy(@RequestParam(required = false) String status) {
        Long me = AuthContext.requireRole("student");
        QueryWrapper<CourseStudents> qw = new QueryWrapper<>();
        qw.eq("student_id", me);
        if (Strings.isNotEmpty(status)) qw.eq("status", status);
        qw.orderByDesc("id");
        List<CourseStudents> rows = enrollmentService.list(qw);

        // 一次性加载相关课程，避免 N+1
        Set<Long> courseIds = rows.stream().map(CourseStudents::getCourseId).collect(Collectors.toSet());
        Map<Long, Courses> courseMap = courseIds.isEmpty()
                ? Collections.emptyMap()
                : coursesMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(Courses::getId, c -> c));

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("enrollment", r);
            m.put("course", courseMap.get(r.getCourseId()));
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /* ============================================================
     * 教师侧
     * ============================================================ */

    /** 教师：查看课程的所有选课关系（含状态），用于课程详情学生 Tab */
    @GetMapping("/teacher/course/{courseId}")
    public Result teacherCourseEnrollments(@PathVariable Long courseId,
                                           @RequestParam(required = false) String status) {
        Long me = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        Courses course = coursesMapper.selectById(courseId);
        if (course == null) return Result.error(404, "课程不存在");
        if (!"admin".equals(role)) {
            if (!"teacher".equals(role)) return Result.error(403, "仅教师/管理员可查看");
            if (!Objects.equals(course.getTeacherId(), me)) return Result.error(403, "只能查看自己的课程");
        }

        QueryWrapper<CourseStudents> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId);
        if (Strings.isNotEmpty(status)) qw.eq("status", status);
        qw.orderByDesc("id");
        List<CourseStudents> rows = enrollmentService.list(qw);

        Set<Long> studentIds = rows.stream().map(CourseStudents::getStudentId).collect(Collectors.toSet());
        Map<Long, User> userMap = studentIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(studentIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("enrollment", r);
            User u = userMap.get(r.getStudentId());
            if (u != null) u.setPassword(null);
            m.put("student", u);
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /**
     * 教师：邀请学生加入课程。
     * body: { courseId, studentId? , username? }
     * 优先使用 studentId；缺失时按 username 查找学生。
     */
    @PostMapping("/teacher/invite")
    public Result teacherInvite(@RequestBody Map<String, Object> body) {
        Long me = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        if (!"teacher".equals(role) && !"admin".equals(role)) {
            return Result.error(403, "仅教师/管理员可邀请");
        }

        Long courseId = toLong(body.get("courseId"));
        if (courseId == null) return Result.error(400, "缺少 courseId");
        Courses course = coursesMapper.selectById(courseId);
        if (course == null) return Result.error(404, "课程不存在");
        if (!"admin".equals(role) && !Objects.equals(course.getTeacherId(), me)) {
            return Result.error(403, "只能邀请到自己的课程");
        }
        if (!Courses.STATUS_APPROVED.equals(course.getStatus())) {
            return Result.error(403, "课程未通过审批，无法邀请学生");
        }

        Long studentId = toLong(body.get("studentId"));
        if (studentId == null) {
            String username = body.get("username") == null ? null : String.valueOf(body.get("username")).trim();
            if (username == null || username.isEmpty()) return Result.error(400, "请提供 studentId 或 username");
            QueryWrapper<User> uq = new QueryWrapper<>();
            uq.eq("username", username);
            User u = userMapper.selectOne(uq);
            if (u == null) return Result.error(404, "找不到用户：" + username);
            if (!"student".equals(u.getRole())) return Result.error(400, "只能邀请学生角色");
            studentId = u.getId();
        } else {
            User u = userMapper.selectById(studentId);
            if (u == null) return Result.error(404, "学生不存在");
            if (!"student".equals(u.getRole())) return Result.error(400, "目标用户不是学生");
        }

        CourseStudents existing = findRelation(courseId, studentId);
        if (existing != null) {
            if (CourseStudents.STATUS_APPROVED.equals(existing.getStatus())) {
                return Result.error(400, "该学生已加入该课程");
            }
            if (CourseStudents.STATUS_PENDING.equals(existing.getStatus())) {
                return Result.error(400, "该学生已有待处理的关系");
            }
            // rejected → 复用为新邀请
            existing.setStatus(CourseStudents.STATUS_PENDING);
            existing.setSource(CourseStudents.SOURCE_TEACHER_INVITE);
            existing.setApplyMessage(null);
            existing.setRejectReason(null);
            existing.setReviewedAt(null);
            enrollmentService.updateById(existing);
            return Result.success(existing);
        }

        CourseStudents row = new CourseStudents();
        row.setCourseId(courseId);
        row.setStudentId(studentId);
        row.setStatus(CourseStudents.STATUS_PENDING);
        row.setSource(CourseStudents.SOURCE_TEACHER_INVITE);
        row.setCreatedAt(LocalDateTime.now());
        enrollmentService.save(row);
        return Result.success(row);
    }

    /** 教师：批准学生申请 */
    @PostMapping("/teacher/approve/{id}")
    public Result teacherApprove(@PathVariable Long id) {
        return reviewByTeacher(id, true, null);
    }

    /** 教师：驳回学生申请；body: { reason } */
    @PostMapping("/teacher/reject/{id}")
    public Result teacherReject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return reviewByTeacher(id, false, reason);
    }

    /** 教师：从课程中移除学生（已批准的关系） */
    @PostMapping("/teacher/remove/{id}")
    public Result teacherRemove(@PathVariable Long id) {
        Long me = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        CourseStudents row = enrollmentService.getById(id);
        if (row == null) return Result.error(404, "记录不存在");
        Courses course = coursesMapper.selectById(row.getCourseId());
        if (course == null) return Result.error(404, "课程不存在");
        if (!"admin".equals(role)) {
            if (!"teacher".equals(role)) return Result.error(403, "无权操作");
            if (!Objects.equals(course.getTeacherId(), me)) return Result.error(403, "只能管理自己的课程");
        }
        enrollmentService.removeById(id);
        return Result.success();
    }

    /* ============================================================
     * 内部
     * ============================================================ */

    private Result reviewByTeacher(Long id, boolean approve, String reason) {
        Long me = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        CourseStudents row = enrollmentService.getById(id);
        if (row == null) return Result.error(404, "记录不存在");
        Courses course = coursesMapper.selectById(row.getCourseId());
        if (course == null) return Result.error(404, "课程不存在");
        if (!"admin".equals(role)) {
            if (!"teacher".equals(role)) return Result.error(403, "无权操作");
            if (!Objects.equals(course.getTeacherId(), me)) return Result.error(403, "只能管理自己的课程");
        }
        if (!CourseStudents.STATUS_PENDING.equals(row.getStatus())
                || !CourseStudents.SOURCE_STUDENT_APPLY.equals(row.getSource())) {
            return Result.error(400, "不是待教师审批的申请");
        }
        row.setStatus(approve ? CourseStudents.STATUS_APPROVED : CourseStudents.STATUS_REJECTED);
        row.setRejectReason(approve ? null : reason);
        row.setReviewedAt(LocalDateTime.now());
        enrollmentService.updateById(row);
        return Result.success(row);
    }

    private CourseStudents findRelation(Long courseId, Long studentId) {
        QueryWrapper<CourseStudents> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).eq("student_id", studentId);
        return enrollmentService.getOne(qw);
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}

package com.cen.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Result;
import com.cen.entity.Courses;
import com.cen.entity.User;
import com.cen.mapper.UserMapper;
import com.cen.service.ICoursesService;
import com.cen.utils.AuthContext;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 课程相关接口（覆盖 学生 / 教师 / 管理员 三端）。
 *
 * 角色 → 暴露能力：
 *   - student：只能看到 status=approved 的课程
 *   - teacher：可见自己名下所有课程（含 pending / rejected），可申请新建 / 修改未通过的
 *   - admin  ：可见全平台课程，可对 pending 进行 approve / reject
 *
 * 路径前缀：
 *   - /courses/admin/**  → 仅管理员
 *   - /courses/teacher/** → 教师/管理员
 *   - /courses/student/** → 学生/教师/管理员
 */
@RestController
@RequestMapping("/courses")
public class CoursesController {

    @Resource
    private ICoursesService coursesService;

    @Resource
    private UserMapper userMapper;

    /* ============================================================
     * 公共 / 学生侧
     * ============================================================ */

    /** 通用查询：默认仅返回 status=approved；admin 可以指定 status；teacher 可只查自己。 */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long teacherId,
                       @RequestParam(required = false) String status) {
        QueryWrapper<Courses> qw = new QueryWrapper<>();
        if (teacherId != null) {
            qw.eq("teacher_id", teacherId);
        }
        String role = AuthContext.currentRole();
        if ("admin".equals(role)) {
            if (status != null && !status.trim().isEmpty()) {
                qw.eq("status", status);
            }
        } else if ("teacher".equals(role)) {
            Long me = AuthContext.currentUserId();
            if (teacherId == null && me != null) {
                qw.eq("teacher_id", me);
            }
        } else {
            qw.and(w -> w.eq("status", Courses.STATUS_APPROVED).or().isNull("status"));
        }
        qw.orderByDesc("id");
        return Result.success(coursesService.list(qw));
    }

    /** 学生选课列表（仅 approved，已在 mapper 内做过滤） */
    @GetMapping("/studentList")
    public Result studentList(@RequestParam Long studentId) {
        return Result.success(coursesService.getCoursesByStudentId(studentId));
    }

    @GetMapping("/student/{studentId}")
    public Result getCoursesByStudentId(@PathVariable Long studentId) {
        return Result.success(coursesService.getCoursesByStudentId(studentId));
    }

    /**
     * 学生发现课程：分页 + 关键词（课程名 / 课程编号）。
     * 仅返回 status=approved，可选过滤掉已加入或已申请的课程（默认会保留以便前端给出对应状态徽章）。
     */
    @GetMapping("/discover")
    public Result discover(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {
        QueryWrapper<Courses> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("status", Courses.STATUS_APPROVED).or().isNull("status"));
        if (Strings.isNotEmpty(keyword)) {
            String k = keyword.trim();
            qw.and(w -> w.like("name", k).or().like("code", k));
        }
        qw.orderByDesc("id");
        return Result.success(coursesService.page(new Page<>(pageNum, pageSize), qw));
    }

    /* ============================================================
     * 教师侧
     * ============================================================ */

    /**
     * 教师提交课程申请（新建或更新未通过的）。
     * - 新建：必须传 name/code，自动落 status=pending
     * - 已通过的课程不允许通过此接口修改
     */
    @PostMapping("/teacher/submit")
    public Result teacherSubmit(@RequestBody Courses courses) {
        Long actor = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        if (!"teacher".equals(role) && !"admin".equals(role)) {
            return Result.error(403, "仅教师或管理员可提交课程");
        }
        if ("teacher".equals(role)) {
            courses.setTeacherId(actor);
        }
        Courses saved = coursesService.submitCourseProposal(courses, actor);
        return Result.success(saved);
    }

    /** 教师查看自己提交的所有课程（任何状态） */
    @GetMapping("/teacher/my")
    public Result teacherMy(@RequestParam(required = false) Long teacherId) {
        Long tid = teacherId != null ? teacherId : AuthContext.requireUserId();
        QueryWrapper<Courses> qw = new QueryWrapper<>();
        qw.eq("teacher_id", tid).orderByDesc("id");
        return Result.success(coursesService.list(qw));
    }

    /* ============================================================
     * 管理员侧
     * ============================================================ */

    /** 管理员：分页查看全平台课程 */
    @GetMapping("/admin/page")
    public Result adminPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(defaultValue = "") String name,
                            @RequestParam(defaultValue = "") String status) {
        AuthContext.requireRole("admin");
        QueryWrapper<Courses> qw = new QueryWrapper<>();
        qw.like(Strings.isNotEmpty(name), "name", name);
        qw.eq(Strings.isNotEmpty(status), "status", status);
        qw.orderByDesc("id");
        return Result.success(coursesService.page(new Page<>(pageNum, pageSize), qw));
    }

    /** 管理员：待审批列表（含教师信息，前端直接展示） */
    @GetMapping("/admin/pending")
    public Result adminPending() {
        AuthContext.requireRole("admin");
        QueryWrapper<Courses> qw = new QueryWrapper<>();
        qw.eq("status", Courses.STATUS_PENDING).orderByAsc("id");
        List<Courses> list = coursesService.list(qw);

        List<Map<String, Object>> rows = list.stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("course", c);
            if (c.getTeacherId() != null) {
                User u = userMapper.selectById(c.getTeacherId());
                row.put("teacher", u);
            }
            return row;
        }).collect(Collectors.toList());
        return Result.success(rows);
    }

    /** 管理员：通过 */
    @PostMapping("/admin/approve/{id}")
    public Result adminApprove(@PathVariable Long id) {
        Long admin = AuthContext.requireRole("admin");
        return Result.success(coursesService.reviewCourse(id, true, null, admin));
    }

    /** 管理员：驳回 */
    @PostMapping("/admin/reject/{id}")
    public Result adminReject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long admin = AuthContext.requireRole("admin");
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return Result.success(coursesService.reviewCourse(id, false, reason, admin));
    }

    /** 管理员：直接删除（不进入审批流） */
    @PostMapping("/admin/delete/{id}")
    public Result adminDelete(@PathVariable Long id) {
        AuthContext.requireRole("admin");
        return Result.success(coursesService.removeById(id));
    }

    /* ============================================================
     * 兼容旧接口（保留给历史调用方）
     * ============================================================ */

    /**
     * 旧 /save 接口：
     *  - 教师调用 → 实际转发到 submitCourseProposal（带审批流）
     *  - 管理员调用 → 直接落库（保持向后兼容）
     */
    @PostMapping("/save")
    public Result save(@RequestBody Courses courses) {
        Long actor = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        if ("admin".equals(role)) {
            if (courses.getStatus() == null) {
                courses.setStatus(Courses.STATUS_APPROVED);
                courses.setReviewedBy(actor);
            }
            return Result.success(coursesService.saveOrUpdate(courses));
        }
        return Result.success(coursesService.submitCourseProposal(courses, actor));
    }

    /** 删除：教师只能删自己未通过的；管理员可删任意 */
    @PostMapping("/delete")
    public Result delete(@RequestBody Courses courses) {
        Long actor = AuthContext.requireUserId();
        String role = AuthContext.currentRole();
        Courses existing = coursesService.getById(courses.getId());
        if (existing == null) return Result.error(404, "课程不存在");
        if (!"admin".equals(role)) {
            if (existing.getTeacherId() == null || !existing.getTeacherId().equals(actor)) {
                return Result.error(403, "无权删除他人课程");
            }
            if (Courses.STATUS_APPROVED.equals(existing.getStatus())) {
                return Result.error(403, "已通过的课程仅管理员可删除");
            }
        }
        return Result.success(coursesService.removeById(courses.getId()));
    }

    @PostMapping("/del/batch")
    public Result batch(@RequestBody List<Integer> ids) {
        AuthContext.requireRole("admin");
        return Result.success(coursesService.removeBatchByIds(ids));
    }

    @GetMapping("/getById")
    public Result findOne(@PathVariable Courses courses) {
        return Result.success(coursesService.getById(courses.getId()));
    }

    /**
     * 获取全部课程列表并按学年学期分组 - 教师端 My Courses 主数据源。
     * 仅返回该教师名下课程（任何状态）。
     */
    @GetMapping("/allList")
    public Result allList(@RequestParam(required = false) Long teacherId) {
        QueryWrapper<Courses> queryWrapper = new QueryWrapper<>();
        if (teacherId != null) {
            queryWrapper.eq("teacher_id", teacherId);
        }
        queryWrapper.orderByDesc("academic_year")
                   .orderByAsc("semester")
                   .orderByDesc("id");

        List<Courses> coursesList = coursesService.list(queryWrapper);

        Map<String, Map<Integer, List<Courses>>> groupedMap = coursesList.stream()
                .filter(c -> c.getAcademicYear() != null && c.getSemester() != null)
                .collect(Collectors.groupingBy(
                        Courses::getAcademicYear,
                        () -> new TreeMap<String, Map<Integer, List<Courses>>>((a, b) -> b.compareTo(a)),
                        Collectors.groupingBy(
                                Courses::getSemester,
                                TreeMap::new,
                                Collectors.toList()
                        )
                ));

        List<YearData> resultList = groupedMap.entrySet().stream()
                .map(yearEntry -> {
                    YearData yearData = new YearData();
                    yearData.setYear(yearEntry.getKey());
                    yearData.setSemesters(yearEntry.getValue().entrySet().stream()
                            .map(semesterEntry -> {
                                SemesterData semesterData = new SemesterData();
                                semesterData.setSemester(semesterEntry.getKey());
                                semesterData.setCourses(semesterEntry.getValue());
                                return semesterData;
                            })
                            .collect(Collectors.toList()));
                    return yearData;
                })
                .collect(Collectors.toList());

        return Result.success(resultList);
    }

    @Data
    static class YearData {
        private String year;
        private List<SemesterData> semesters;
    }

    @Data
    static class SemesterData {
        private Integer semester;
        private List<Courses> courses;
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String name) {
        QueryWrapper<Courses> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(Strings.isNotEmpty(name), "name", name);
        String role = AuthContext.currentRole();
        if (!"admin".equals(role)) {
            queryWrapper.and(w -> w.eq("status", Courses.STATUS_APPROVED).or().isNull("status"));
        }
        queryWrapper.orderByDesc("id");
        return Result.success(coursesService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }
}

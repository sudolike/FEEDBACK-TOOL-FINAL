package com.cen.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Result;
import com.cen.entity.*;
import com.cen.mapper.*;
import com.cen.service.IUserService;
import com.cen.utils.AuthContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员中心：
 *  - 全局仪表盘（用户/课程/问卷/反馈数）
 *  - 用户管理（分页 / 启用 / 停用 / 重置密码 / 删除）
 *  - 反馈与问卷的全局只读视图
 *
 * 拦截器 JwtInterceptor 已为 /admin/** 强制要求 admin 角色。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final String DEFAULT_RESET_PASSWORD = "Welcome2026!";

    @Resource private IUserService userService;
    @Resource private UserMapper userMapper;
    @Resource private CoursesMapper coursesMapper;
    @Resource private QuestionnairesMapper questionnairesMapper;
    @Resource private QuestionnaireResponsesMapper questionnaireResponsesMapper;
    @Resource private CourseFeedbackMapper courseFeedbackMapper;
    @Resource private TeacherRatingMapper teacherRatingMapper;

    /* ------------------------- 仪表盘 ------------------------- */

    @GetMapping("/dashboard")
    public Result dashboard() {
        AuthContext.requireRole("admin");

        Map<String, Object> ret = new HashMap<>();

        Long totalUsers   = userMapper.selectCount(new QueryWrapper<>());
        Long totalTeachers = userMapper.selectCount(new QueryWrapper<User>().eq("role", "teacher"));
        Long totalStudents = userMapper.selectCount(new QueryWrapper<User>().eq("role", "student"));
        Long totalAdmins   = userMapper.selectCount(new QueryWrapper<User>().eq("role", "admin"));
        Long disabledUsers = userMapper.selectCount(new QueryWrapper<User>().eq("status", 0));

        Long totalCourses   = coursesMapper.selectCount(new QueryWrapper<>());
        Long pendingCourses = coursesMapper.selectCount(
                new QueryWrapper<Courses>().eq("status", Courses.STATUS_PENDING));
        Long approvedCourses = coursesMapper.selectCount(
                new QueryWrapper<Courses>().eq("status", Courses.STATUS_APPROVED));
        Long rejectedCourses = coursesMapper.selectCount(
                new QueryWrapper<Courses>().eq("status", Courses.STATUS_REJECTED));

        Long totalQuestionnaires = questionnairesMapper.selectCount(new QueryWrapper<>());
        Long totalResponses      = questionnaireResponsesMapper.selectCount(new QueryWrapper<>());
        Long totalFeedbacks      = courseFeedbackMapper.selectCount(new QueryWrapper<>());
        Long totalTeacherRatings = teacherRatingMapper.selectCount(new QueryWrapper<>());

        LocalDateTime today = LocalDate.now().atStartOfDay();
        Long todayResponses = questionnaireResponsesMapper.selectCount(
                new QueryWrapper<QuestionnaireResponses>().ge("submitted_at", today));
        Long todayFeedbacks = courseFeedbackMapper.selectCount(
                new QueryWrapper<CourseFeedback>().ge("created_at", today));

        ret.put("totalUsers", totalUsers);
        ret.put("totalTeachers", totalTeachers);
        ret.put("totalStudents", totalStudents);
        ret.put("totalAdmins", totalAdmins);
        ret.put("disabledUsers", disabledUsers);

        ret.put("totalCourses", totalCourses);
        ret.put("pendingCourses", pendingCourses);
        ret.put("approvedCourses", approvedCourses);
        ret.put("rejectedCourses", rejectedCourses);

        ret.put("totalQuestionnaires", totalQuestionnaires);
        ret.put("totalResponses", totalResponses);
        ret.put("totalFeedbacks", totalFeedbacks);
        ret.put("totalTeacherRatings", totalTeacherRatings);

        ret.put("todayResponses", todayResponses);
        ret.put("todayFeedbacks", todayFeedbacks);
        return Result.success(ret);
    }

    /* ------------------------- 用户管理 ------------------------- */

    @GetMapping("/users/page")
    public Result usersPage(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(defaultValue = "") String username,
                            @RequestParam(defaultValue = "") String role,
                            @RequestParam(required = false) Integer status) {
        AuthContext.requireRole("admin");
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.like(Strings.isNotEmpty(username), "username", username);
        qw.eq(Strings.isNotEmpty(role), "role", role);
        qw.eq(status != null, "status", status);
        qw.orderByDesc("id");
        return Result.success(userService.page(new Page<>(pageNum, pageSize), qw));
    }

    @PostMapping("/users/{id}/disable")
    public Result disableUser(@PathVariable Long id) {
        Long actor = AuthContext.requireRole("admin");
        if (id.equals(actor)) {
            return Result.error(400, "不能停用自己");
        }
        User u = userService.getById(id);
        if (u == null) return Result.error(404, "用户不存在");
        if ("admin".equals(u.getRole())) {
            return Result.error(403, "管理员账号受保护，请通过数据库手动处理");
        }
        u.setStatus(0);
        userService.updateById(u);
        return Result.success();
    }

    @PostMapping("/users/{id}/enable")
    public Result enableUser(@PathVariable Long id) {
        AuthContext.requireRole("admin");
        User u = userService.getById(id);
        if (u == null) return Result.error(404, "用户不存在");
        u.setStatus(1);
        userService.updateById(u);
        return Result.success();
    }

    @PostMapping("/users/{id}/reset-password")
    public Result resetUserPassword(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, String> body) {
        AuthContext.requireRole("admin");
        User u = userService.getById(id);
        if (u == null) return Result.error(404, "用户不存在");
        if ("admin".equals(u.getRole())) {
            return Result.error(403, "管理员账号密码请联系数据库管理员重置");
        }
        String newPwd = body != null ? body.getOrDefault("password", "") : "";
        if (newPwd == null || newPwd.isEmpty()) newPwd = DEFAULT_RESET_PASSWORD;
        u.setPassword(newPwd);
        userService.updateById(u);
        Map<String, String> ret = new HashMap<>();
        ret.put("password", newPwd);
        return Result.success(ret);
    }

    @PostMapping("/users/{id}/delete")
    public Result deleteUser(@PathVariable Long id) {
        Long actor = AuthContext.requireRole("admin");
        if (id.equals(actor)) {
            return Result.error(400, "不能删除自己");
        }
        User u = userService.getById(id);
        if (u == null) return Result.error(404, "用户不存在");
        if ("admin".equals(u.getRole())) {
            return Result.error(403, "管理员账号受保护，无法在前端删除");
        }
        userService.removeById(id);
        return Result.success();
    }

    /* ------------------------- 反馈 / 问卷概览 ------------------------- */

    @GetMapping("/feedbacks/page")
    public Result feedbackPage(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthContext.requireRole("admin");
        QueryWrapper<CourseFeedback> qw = new QueryWrapper<>();
        qw.orderByDesc("created_at");
        return Result.success(courseFeedbackMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    @GetMapping("/questionnaires/page")
    public Result questionnairesPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthContext.requireRole("admin");
        QueryWrapper<Questionnaires> qw = new QueryWrapper<>();
        qw.orderByDesc("id");
        return Result.success(questionnairesMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }
}

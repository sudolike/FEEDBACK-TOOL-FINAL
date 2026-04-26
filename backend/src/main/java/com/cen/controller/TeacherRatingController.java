package com.cen.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import com.cen.entity.TeacherRating;
import com.cen.service.ITeacherRatingService;
import com.cen.utils.AnonymizeUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师评分：学生提交 & 教师/管理员查看（匿名化）。
 */
@RestController
@RequestMapping("/teacherRating")
public class TeacherRatingController {

    @Resource private ITeacherRatingService teacherRatingService;

    @PostMapping("/save")
    public Result save(@RequestBody TeacherRating rating) {
        return Result.success(teacherRatingService.saveOrUpdate(rating));
    }

    @GetMapping("/teacher/{teacherId}")
    public Result listByTeacher(@PathVariable Long teacherId) {
        QueryWrapper<TeacherRating> qw = new QueryWrapper<>();
        qw.eq("teacher_id", teacherId).orderByDesc("created_at");
        List<TeacherRating> list = teacherRatingService.list(qw);
        // 匿名化输出
        List<Map<String, Object>> ret = list.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("courseId", r.getCourseId());
            m.put("teacherId", r.getTeacherId());
            m.put("rating", r.getRating());
            m.put("teachingScore", r.getTeachingScore());
            m.put("attitudeScore", r.getAttitudeScore());
            m.put("contentScore", r.getContentScore());
            m.put("comment", r.getComment());
            m.put("createdAt", r.getCreatedAt());
            m.put("anonymousId", AnonymizeUtils.anonymize(r.getStudentId(), r.getTeacherId()));
            return m;
        }).collect(Collectors.toList());
        return Result.success(ret);
    }

    @GetMapping("/stats/{teacherId}")
    public Result stats(@PathVariable Long teacherId) {
        return Result.success(teacherRatingService.statsByTeacher(teacherId));
    }

    @GetMapping("/my")
    public Result my(@RequestParam Long studentId,
                     @RequestParam Long courseId,
                     @RequestParam Long teacherId) {
        QueryWrapper<TeacherRating> qw = new QueryWrapper<>();
        qw.eq("student_id", studentId)
          .eq("course_id", courseId)
          .eq("teacher_id", teacherId);
        return Result.success(teacherRatingService.getOne(qw));
    }
}

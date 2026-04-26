package com.cen.controller;

import com.cen.common.Result;
import com.cen.entity.Assignment;
import com.cen.entity.AssignmentSubmission;
import com.cen.service.IAssignmentService;
import com.cen.service.IAssignmentSubmissionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/assignment")
public class AssignmentController {

    @Resource private IAssignmentService assignmentService;
    @Resource private IAssignmentSubmissionService submissionService;

    /* ------ 教师 ------ */

    @PostMapping("/save")
    public Result save(@RequestBody Assignment assignment) {
        return Result.success(assignmentService.saveOrUpdate(assignment));
    }

    @PostMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return Result.success(assignmentService.removeById(id));
    }

    @GetMapping("/course/{courseId}")
    public Result listByCourse(@PathVariable Long courseId) {
        return Result.success(assignmentService.listByCourse(courseId));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Long id) {
        return Result.success(assignmentService.getById(id));
    }

    @GetMapping("/student/{studentId}")
    public Result listByStudent(@PathVariable Long studentId) {
        return Result.success(assignmentService.listByStudent(studentId));
    }

    /* ------ 学生提交 ------ */

    @PostMapping("/submission/save")
    public Result submit(@RequestBody AssignmentSubmission submission) {
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(LocalDateTime.now());
        }
        return Result.success(submissionService.saveOrUpdate(submission));
    }

    @GetMapping("/submission/list/{assignmentId}")
    public Result submissionList(@PathVariable Long assignmentId) {
        return Result.success(submissionService.listByAssignment(assignmentId));
    }

    @GetMapping("/submission/my")
    public Result mySubmission(@RequestParam Long assignmentId, @RequestParam Long studentId) {
        return Result.success(submissionService.getOne(assignmentId, studentId));
    }

    /** 教师批改 */
    @PostMapping("/submission/grade")
    public Result grade(@RequestBody AssignmentSubmission submission) {
        submission.setGradedAt(LocalDateTime.now());
        return Result.success(submissionService.updateById(submission));
    }
}

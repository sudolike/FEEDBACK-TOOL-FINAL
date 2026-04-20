package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import java.util.List;

import com.cen.service.ICourseQuestionnaireService;
import com.cen.entity.CourseQuestionnaire;
import com.cen.controller.dto.CourseQuestionnaireDTO;
import com.cen.service.IQuestionnaireResponsesService;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-12
 */
@RestController
@RequestMapping("/courseQuestionnaire")
public class CourseQuestionnaireController {

    @Resource
    private ICourseQuestionnaireService courseQuestionnaireService;

    @Resource
    private IQuestionnaireResponsesService questionnaireResponsesService;

    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody CourseQuestionnaire courseQuestionnaire) {
        return Result.success(courseQuestionnaireService.saveOrUpdate(courseQuestionnaire));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody CourseQuestionnaire courseQuestionnaire){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(courseQuestionnaireService.removeById(courseQuestionnaire.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(courseQuestionnaireService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable CourseQuestionnaire courseQuestionnaire) {
        return Result.success(courseQuestionnaireService.getById(courseQuestionnaire.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(courseQuestionnaireService.page(new Page<>(pageNum, pageSize)));
    }

    // 查询课程问卷关联关系
    @GetMapping("/course/{courseId}")
    public Result getQuestionnaireByCourseId(@PathVariable Long courseId) {
        return Result.success(courseQuestionnaireService.getQuestionnaireByCourseId(courseId));
    }

    // 批量关联课程和问卷
    @PostMapping("/bind/{courseId}")
    public Result bindQuestionnaires(
            @PathVariable Long courseId,
            @RequestParam String questionnaireIds) {
        return Result.success(courseQuestionnaireService.bindQuestionnaires(courseId, questionnaireIds));
    }

    // 删除课程问卷关联关系
    @PostMapping("/unbind")
    public Result unbindQuestionnaire(@RequestBody CourseQuestionnaireDTO dto) {
        return Result.success(courseQuestionnaireService.unbindQuestionnaire(dto.getCourseId(), dto.getQuestionnaireId()));
    }

    // 更新问卷状态
    @PostMapping("/updateStatus")
    public Result updateStatus(@RequestBody CourseQuestionnaireDTO dto) {
        return Result.success(courseQuestionnaireService.updateStatus(dto.getCourseId(), 
                                                                    dto.getQuestionnaireId(), 
                                                                    dto.getStatus()));
    }

    // 发布问卷
    @PostMapping("/publish")
    public Result publishQuestionnaire(@RequestBody CourseQuestionnaireDTO dto) {
        return Result.success(courseQuestionnaireService.publishQuestionnaire(dto.getCourseId(), dto.getQuestionnaireId()));
    }

    // 结束问卷
    @PostMapping("/end")
    public Result completeQuestionnaire(@RequestBody CourseQuestionnaireDTO dto) {
        return Result.success(courseQuestionnaireService.completeQuestionnaire(dto.getCourseId(), dto.getQuestionnaireId()));
    }

    // 撤回问卷
    @PostMapping("/revoke")
    public Result recallQuestionnaire(@RequestBody CourseQuestionnaireDTO dto) {
        return Result.success(courseQuestionnaireService.recallQuestionnaire(dto.getCourseId(), dto.getQuestionnaireId()));
    }

    // 查询课程的进行中和已结束问卷
    @GetMapping("/status/{courseId}")
    public Result getQuestionnairesByStatus(
            @PathVariable Long courseId,
            @RequestParam Long studentId) {
        return Result.success(courseQuestionnaireService.getQuestionnairesByStatus(courseId, studentId));
    }

    // 查询学生的问卷答案
    @GetMapping("/response")
    public Result getStudentResponse(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId,
            @RequestParam Long studentId) {
        return Result.success(courseQuestionnaireService.getStudentResponse(courseId, questionnaireId, studentId));
    }

    // 查询学生的所有课程问卷
    @GetMapping("/student/questionnaires")
    public Result getStudentQuestionnaires(@RequestParam Long studentId) {
        return Result.success(courseQuestionnaireService.getStudentQuestionnaires(studentId));
    }

    // 获取课程问卷的填写统计信息
    @GetMapping("/stats/{courseId}")
    public Result getQuestionnaireSubmissionStats(@PathVariable Long courseId) {
        return Result.success(courseQuestionnaireService.getQuestionnaireSubmissionStats(courseId));
    }

    // 查询课程问卷的填写情况汇总
    @GetMapping("/responses")
    public Result getQuestionnaireResponses(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) {
        return Result.success(questionnaireResponsesService.getQuestionnaireResponseSummary(courseId, questionnaireId));
    }
}


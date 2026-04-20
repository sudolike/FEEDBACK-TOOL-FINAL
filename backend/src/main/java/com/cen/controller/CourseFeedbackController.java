package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import java.util.List;

import com.cen.service.ICourseFeedbackService;
import com.cen.entity.CourseFeedback;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 课程反馈表 前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-12
 */
@RestController
@RequestMapping("/courseFeedback")
public class CourseFeedbackController {

    @Resource
    private ICourseFeedbackService courseFeedbackService;
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody CourseFeedback courseFeedback) {
        return Result.success(courseFeedbackService.saveOrUpdate(courseFeedback));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody CourseFeedback courseFeedback){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(courseFeedbackService.removeById(courseFeedback.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(courseFeedbackService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable CourseFeedback courseFeedback) {
        return Result.success(courseFeedbackService.getById(courseFeedback.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<CourseFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(courseFeedbackService.page(new Page<>(pageNum, pageSize)));
    }

    // 查询课程反馈
    @GetMapping("/course/{courseId}")
    public Result getCourseFeedbacks(@PathVariable Long courseId) {
        return Result.success(courseFeedbackService.getCourseFeedbacksWithUser(courseId));
    }
}


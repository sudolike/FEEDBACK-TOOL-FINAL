package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import java.util.List;

import com.cen.service.IResponsesService;
import com.cen.entity.Responses;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-14
 */
@RestController
@RequestMapping("/responses")
public class ResponsesController {

    @Resource
    private IResponsesService responsesService;
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody Responses responses) {
        return Result.success(responsesService.saveOrUpdate(responses));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody Responses responses){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(responsesService.removeById(responses.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(responsesService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable Responses responses) {
        return Result.success(responsesService.getById(responses.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<Responses> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(responsesService.page(new Page<>(pageNum, pageSize)));
    }
}


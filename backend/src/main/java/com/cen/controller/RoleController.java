package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;

import com.cen.service.IRoleService;
import com.cen.entity.Role;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-02-17
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    @Resource
    private IRoleService roleService;
    //角色绑定菜单
    @GetMapping("/roleList")
    public Result roleList(){
        return Result.success(roleService.list());
    }
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody Role role) {
        return Result.success(roleService.saveOrUpdate(role));
    }
    //删除
    @PostMapping("/delete")
    public Result userDelete(@RequestBody Role role){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(roleService.removeById(role.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result batch(@RequestBody List<Integer> ids){
        return Result.success(roleService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable Role role) {
        return Result.success(roleService.getById(role.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String name) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(Strings.isNotEmpty(name),"name",name);
        queryWrapper.orderByAsc("id"); //设置id倒序
        return Result.success(roleService.page(new Page<>(pageNum, pageSize),queryWrapper));
    }
}


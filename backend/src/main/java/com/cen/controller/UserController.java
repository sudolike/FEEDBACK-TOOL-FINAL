package com.cen.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Constants;
import com.cen.common.lang.Const;
import com.cen.controller.dto.UserDTO;
import com.cen.entity.FileD;
import com.cen.mapper.UserMapper;
import com.cen.utils.RedisUtil;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.cen.service.IUserService;
import com.cen.entity.User;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
@RequestMapping("/user")
public class UserController {

    @Value("${files.upload.path}")
    private  String fileUploadPath;
    @Resource
    private IUserService userService;
    @Resource
    UserMapper userMapper;
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody User user) {
//        return Result.success(userService.saveOrUpdate(user));
        return Result.success(userService.saveUser(user));
    }
    //修改密码
    @PostMapping("/edit/pow")
    public Result editPow(@RequestBody User user) {
        return Result.success(userService.editPow(user));
    }
    //上传头像
    @PostMapping("/upload/avatar")
    public Result uploadAvatar(@RequestBody User user) throws IOException {

        return Result.success(userService.saveOrUpdate(user));
    }
    //删除
    @PostMapping("/delete")
    public Result userDelete(@RequestBody User user){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(userService.removeById(user.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result batch(@RequestBody List<Integer> ids){
        return Result.success(userService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@RequestParam Long id) {
        return Result.success(userService.getById(id));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String nickname,
                           @RequestParam(defaultValue = "") String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(Strings.isNotEmpty(nickname),"nickname",nickname);
        queryWrapper.like(Strings.isNotEmpty(email),"email",email);
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(userService.page(new Page<>(pageNum, pageSize),queryWrapper));
    }
    private User getFlieMd5(String md5){
        // 查询文件的md5是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5",md5);
        List<User> fileslist = userMapper.selectList(queryWrapper);
        return fileslist.size()==0?null:fileslist.get(0);
    }
    // 查询学生列表
    @GetMapping("/student/list")
    public Result getStudentList() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "student");  // 根据role字段筛选学生
        queryWrapper.orderByDesc("id");
        return Result.success(userService.list(queryWrapper));
    }
}


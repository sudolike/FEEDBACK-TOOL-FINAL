package com.cen.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.cen.common.Constants;
import com.cen.common.Result;
import com.cen.common.lang.Const;
import com.cen.controller.dto.UserDTO;
import com.cen.service.IUserService;
import com.cen.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

@RestController
public class LoginController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IUserService userService;

    //获取验证码
    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        String key = UUID.randomUUID().toString();
        // 利用 hutool 工具，生成验证码图片资源
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 100, 4, 5);
        // 获得生成的验证码字符
        String code = captcha.getCode();
        // 利用 redis 来存储验证码
        redisUtil.set(Const.CAPTCHA_KEY,code);
        // 将验证码图片的二进制数据写入【响应体 response 】
        captcha.write(response.getOutputStream());
    }
    /*登录接口*/
    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO){
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if(StrUtil.isBlank(username) || StrUtil.isBlank(password)){
            return Result.error(Constants.CODE_400,"参数错误");
        }
        return Result.success(userService.login(userDTO));
    }
    /*
     * 公共注册接口：
     *  - 仅允许 student / teacher 注册
     *  - 不接受任何 admin 注册请求（即使前端绕过校验，后端 service 层也会拒绝）
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error(Constants.CODE_400, "参数错误");
        }
        if (userDTO.getRole() != null && "admin".equalsIgnoreCase(userDTO.getRole().trim())) {
            return Result.error(Constants.CODE_403,
                    "管理员账号不开放注册，请使用学生或教师身份");
        }
        return Result.success(userService.register(userDTO));
    }
}

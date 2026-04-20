package com.cen.controller;

import com.cen.common.Result;
import com.cen.service.IEmailService;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/email")
public class EmailController {

    @Resource
    private IEmailService emailService;

    @PostMapping("/send")
    public Result sendEmail(@RequestBody Map<String, String> params) {
        String toEmail = params.get("toEmail");
        log.info("收到发送邮件请求，目标邮箱: {}", toEmail);
        try {
            emailService.sendNotification(toEmail);
            return Result.success();
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            return Result.error(500, "邮件发送失败: " + e.getMessage());
        }
    }
} 
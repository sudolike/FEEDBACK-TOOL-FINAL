package com.cen.service.impl;

import com.cen.service.IEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import com.cen.exception.ServiceException;
import com.cen.common.Constants;

import javax.annotation.Resource;

@Slf4j
@Service
public class EmailServiceImpl implements IEmailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendNotification(String toEmail) {
        try {
            log.info("开始发送邮件到: {}", toEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Feedback Report Notification");
            message.setText("Dear supervisor. A new feedback report has been generated. Please pay attention to it and check it promptly!");
            
            mailSender.send(message);
            log.info("邮件发送成功");
        } catch (MailException e) {
            log.error("邮件发送失败: {}", e.getMessage(), e);
            throw new ServiceException(Constants.CODE_500, "邮件发送失败: " + e.getMessage());
        }
    }
} 
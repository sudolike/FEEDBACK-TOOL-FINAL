package com.cen.utils;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.cen.entity.User;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT Token工具类
 * 用于生成和管理JWT (JSON Web Token)
 */
public class TokenUtils {

    /**
     * 生成JWT token
     * @param user 用户对象
     * @return JWT token字符串
     * 
     * Token的组成部分：
     * 1. Payload (载荷) - 存储用户ID
     * 2. Expiration (过期时间) - 5小时后过期
     * 3. Signature (签名) - 使用用户密码作为密钥进行HMAC-SHA256加密
     */
    public static String getToken(User user){
        return JWT.create().withAudience(user.getId()+"") // 将 user id 保存到 token 里面,作为载荷
                .withExpiresAt(DateUtil.offsetHour(new Date(), 5)) // 5小时后token过期
                .sign(Algorithm.HMAC256(user.getPassword())); // 以 password 作为 token 的密钥
    }
}
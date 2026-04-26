package com.cen.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.cen.common.Constants;
import com.cen.entity.User;
import com.cen.exception.ServiceException;
import com.cen.service.IUserService;
import com.cen.utils.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局 JWT 拦截器：
 *  1. 解析 Token → 写入 AuthContext（线程本地的 userId/role/username）
 *  2. 校验路径前缀的角色要求：
 *      - /**\/admin/**             仅 admin
 *      - /courses/teacher/**       仅 teacher / admin
 *  3. 用户被禁用 (status=0) 直接返回 401
 *  4. afterCompletion 清空 AuthContext，避免线程池污染
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "登录失效，请重新登录");
        }

        String userId;
        try {
            userId = JWT.decode(token).getAudience().get(0);
        } catch (Exception e) {
            throw new ServiceException(Constants.CODE_401, "验证失败，请重新登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException(Constants.CODE_403, "账号已被管理员停用");
        }

        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new ServiceException(Constants.CODE_401, "验证失败，请重新登录");
        }

        AuthContext.set(user.getId(), user.getRole(), user.getUsername());

        String uri = request.getRequestURI();
        if (uri.contains("/admin/") && !"admin".equals(user.getRole())) {
            throw new ServiceException(Constants.CODE_403, "需要管理员权限");
        }
        if (uri.startsWith("/courses/teacher/")
                && !("teacher".equals(user.getRole()) || "admin".equals(user.getRole()))) {
            throw new ServiceException(Constants.CODE_403, "需要教师权限");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res,
                           Object handler, ModelAndView mv) {
    }
}

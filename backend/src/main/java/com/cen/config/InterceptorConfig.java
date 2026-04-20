package com.cen.config;

import com.cen.config.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")//拦截的所有请求，通过判断token是否合法来觉得是否需要登录
                .excludePathPatterns("/login","/register","/captcha","/file/**","/webuser/list", "/ai/aliTyqw");//排除不拦截的请求
    }
}

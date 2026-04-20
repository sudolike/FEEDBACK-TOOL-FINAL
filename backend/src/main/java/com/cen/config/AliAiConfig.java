package com.cen.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云通义千问AI配置类
 * 该配置类负责初始化阿里云AI服务所需的组件
 */
@Configuration // 标记为Spring配置类，会被自动扫描并加载
public class AliAiConfig {
    /**
     * 创建通义千问AI的Generation实例Bean
     * Generation是阿里云通义千问SDK的核心类，用于生成AI文本
     * 通过@Bean注解将其注册到Spring容器中，使其可以在其他组件中通过@Resource注入使用
     * 
     * @return Generation实例，用于调用通义千问AI接口
     */
    @Bean
    public Generation generation() {
        return new Generation(); // 创建并返回Generation实例，无需额外配置，API密钥会在调用时通过参数传入
    }
}

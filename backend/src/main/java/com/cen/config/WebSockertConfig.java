package com.cen.config;

import com.cen.config.interceptor.MessageHanderInterceptors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSockertConfig implements WebSocketConfigurer {

    @Autowired
    private MessageeHander messageeHander;
    @Autowired
    private MessageHanderInterceptors messageHanderInterceptors;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageeHander,"/mushan/{name}")
                .setAllowedOrigins("*")
                .addInterceptors(messageHanderInterceptors);
    }
}

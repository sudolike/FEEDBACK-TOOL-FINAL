package com.cen.config;

import com.alibaba.fastjson.JSON;
import com.cen.pojo.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MessageeHander extends TextWebSocketHandler {

    public static final Map<String,WebSocketSession> SESSION_MAP = new HashMap<>();

    //进入websocket的时候
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String name = session.getAttributes().get("name").toString();
        SESSION_MAP.put(name,session);
        allSend(name);
    }

    private void allSend(String name) {
        Set<String> users = SESSION_MAP.keySet();
        if(!CollectionUtils.isEmpty(users)){

            users.forEach(res->{
                if(StringUtils.isNotEmpty(res) && !res.equals(name)){
                    WebSocketSession webSocketSession = SESSION_MAP.get(res);
                    if (webSocketSession != null){
                        try {
                            Message message = new Message();
                            message.setType(0);
                            message.setMsg("你的好友"+name+"已上线！");
                            webSocketSession.sendMessage(new TextMessage(JSON.toJSONString(message)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
    }

    //关闭
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        SESSION_MAP.remove(session.getAttributes().get("name").toString());
    }

    //发送信息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Message msg = JSON.parseObject(payload, Message.class);
        String to = msg.getTo();
        WebSocketSession webSocketSession = SESSION_MAP.get(to);
        if (webSocketSession != null){
            webSocketSession.sendMessage(new TextMessage(JSON.toJSONString(msg)));
        }
    }
}

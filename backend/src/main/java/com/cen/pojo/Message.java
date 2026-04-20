package com.cen.pojo;

import lombok.Data;

@Data
public class Message {
    private String to;  //给谁
    private String source;  //给谁
    private Integer type;  //类型 0给所有人发消息 1 单个人发送消息  2 发送视频通话消息 3确定进行视频通话
    private String msg;  //消息内容
}

package com.cen.controller.dto;

import lombok.Data;

import java.util.List;

/*
*
* 接受前端登录请求的参数
* */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String token;
    private String role;
    private Integer roleId;

    private String code;
}

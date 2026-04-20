package com.cen.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author wyt
 * @since 2025-02-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // id
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 用户名
    private String username;

    // 密码
    private String password;
    // 角色
    private String role;

    // 昵称
    private String nickname;

    // 头像
    private String avatarUrl;
    // 邮箱
    private String email;

    // 角色  0超级管理员  1 管理员 2普通账号
    private Integer roleId;

    // 是否有效 1有效 0无效
    private Integer status;
}

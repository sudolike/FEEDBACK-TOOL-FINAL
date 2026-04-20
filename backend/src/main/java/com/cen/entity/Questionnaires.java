package com.cen.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author volcano
 * @since 2025-03-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_questionnaires")
public class Questionnaires implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 问卷标题
    private String title;

    // 问卷描述
    private String description;

    // 创建者ID（通常是教师）
    private Long createdBy;

    // 问题列表（JSON数组）
    private String questions;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}

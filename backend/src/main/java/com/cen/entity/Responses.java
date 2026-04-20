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
 * @author wyt
 * @since 2025-03-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_responses")
public class Responses implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 问卷ID
    private Long questionnaireId;

    // 提交学生ID
    private Long studentId;

    // 学生反馈（JSON数组）
    private String answers;

    // 回答时间
    private LocalDateTime createdAt;
}

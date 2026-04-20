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
 * 课程反馈表
 * </p>
 *
 * @author wyt
 * @since 2025-03-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_course_feedback")
public class CourseFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键ID
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 课程ID
    private Long courseId;

    // 学生ID
    private Long studentId;

    // 反馈内容
    private String content;

    // 课程评分(1-5)
    private Integer rating;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}

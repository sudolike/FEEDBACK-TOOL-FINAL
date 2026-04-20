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
@TableName("sys_course_questionnaire")
public class CourseQuestionnaire implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 课程ID
    private Long courseId;

    // 问卷ID
    private Long questionnaireId;

    // 问卷状态：0-待发布，1-进行中，2-已完成
    private Integer status;

    // 关联时间
    private LocalDateTime createdAt;
}

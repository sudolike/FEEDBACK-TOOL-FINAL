package com.cen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教师评分（学生对老师打分，对外接口匿名化）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_teacher_rating")
public class TeacherRating implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long teacherId;
    private Long studentId;

    private Integer rating;
    private Integer teachingScore;
    private Integer attitudeScore;
    private Integer contentScore;

    private String comment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

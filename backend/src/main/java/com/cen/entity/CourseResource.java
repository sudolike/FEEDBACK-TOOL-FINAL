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
 * 课程资料（PPT / PDF / 视频 / 代码 等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_course_resource")
public class CourseResource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long uploaderId;
    private String uploaderRole;

    private String title;
    private String description;

    private Long fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;

    /** lecture / recording / code / other */
    private String category;

    private Integer downloadCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Integer isDeleted;
}

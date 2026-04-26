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
 * 课程实体（含管理员审批字段）。
 *
 * status 取值：
 *   - pending  教师刚提交，等待管理员审批
 *   - approved 已通过，对学生端可见
 *   - rejected 已驳回，仅教师本人可见，可修改后重新提交
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_courses")
public class Courses implements Serializable {

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Long teacherId;

    private String description;

    private String coverUrl;

    private String academicYear;

    private Integer semester;

    private String courseTime;

    private String location;

    /** 审批状态：pending/approved/rejected */
    private String status;

    /** 驳回原因（仅 rejected 时有值） */
    private String rejectReason;

    /** 审批人（管理员）ID */
    private Long reviewedBy;

    /** 审批时间 */
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

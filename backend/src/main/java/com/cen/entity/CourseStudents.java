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
 * 选课关系（含申请/邀请工作流）。
 *
 * 状态机：
 *   学生主动申请：source=student_apply, status=pending → 教师批准 approved / 驳回 rejected
 *   教师主动邀请：source=teacher_invite, status=pending → 学生接受 approved / 拒绝则删除
 *
 * 仅当 status=approved 才视为已加入课程，其它状态不进入"我的课程"。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_course_students")
public class CourseStudents implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    public static final String SOURCE_STUDENT_APPLY = "student_apply";
    public static final String SOURCE_TEACHER_INVITE = "teacher_invite";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 学生ID */
    private Long studentId;

    /** 关系状态：pending / approved / rejected */
    private String status;

    /** 来源：student_apply / teacher_invite */
    private String source;

    /** 学生申请时附带的留言 */
    private String applyMessage;

    /** 教师驳回时的原因 */
    private String rejectReason;

    /** 审核/接受/拒绝的时间 */
    private LocalDateTime reviewedAt;

    /** 选课/申请创建时间 */
    private LocalDateTime createdAt;
}

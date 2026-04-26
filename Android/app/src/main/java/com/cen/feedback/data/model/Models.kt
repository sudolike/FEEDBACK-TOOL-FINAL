package com.cen.feedback.data.model

import com.squareup.moshi.JsonClass

/* ===========  通用响应  =========== */

@JsonClass(generateAdapter = true)
data class ApiResult<T>(
    val code: Int,
    val msg: String?,
    val data: T?,
)

/* ===========  用户与登录  =========== */

@JsonClass(generateAdapter = true)
data class UserDTO(
    val id: Long? = null,
    val username: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val token: String? = null,
    val role: String? = null,
    val roleId: Int? = null,
)

@JsonClass(generateAdapter = true)
data class User(
    val id: Long,
    val username: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val role: String? = null,
    val roleId: Int? = null,
    val status: Int? = null,
)

/* ===========  课程  =========== */

@JsonClass(generateAdapter = true)
data class Courses(
    val id: Long,
    val name: String? = null,
    val code: String? = null,
    val teacherId: Long? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val academicYear: String? = null,
    val semester: Int? = null,
    val courseTime: String? = null,
    val location: String? = null,
    /** pending / approved / rejected */
    val status: String? = null,
    val rejectReason: String? = null,
    val reviewedBy: Long? = null,
    val reviewedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** 用于教师端"申请新课程"的请求体（id 留空 → 新建） */
@JsonClass(generateAdapter = true)
data class CourseProposalRequest(
    val id: Long? = null,
    val name: String,
    val code: String,
    val teacherId: Long? = null,
    val description: String? = null,
    val academicYear: String? = null,
    val semester: Int? = null,
    val courseTime: String? = null,
    val location: String? = null,
)

/** 管理员审批列表的一行：课程 + 教师 */
@JsonClass(generateAdapter = true)
data class PendingCourseRow(
    val course: Courses? = null,
    val teacher: User? = null,
)

/** 通用分页结果 */
@JsonClass(generateAdapter = true)
data class PageResult<T>(
    val records: List<T>? = null,
    val total: Long? = null,
    val size: Long? = null,
    val current: Long? = null,
    val pages: Long? = null,
)

/** 管理员 Dashboard 数据 */
@JsonClass(generateAdapter = true)
data class AdminDashboard(
    val totalUsers: Long? = null,
    val totalTeachers: Long? = null,
    val totalStudents: Long? = null,
    val totalAdmins: Long? = null,
    val disabledUsers: Long? = null,
    val totalCourses: Long? = null,
    val pendingCourses: Long? = null,
    val approvedCourses: Long? = null,
    val rejectedCourses: Long? = null,
    val totalQuestionnaires: Long? = null,
    val totalResponses: Long? = null,
    val totalFeedbacks: Long? = null,
    val totalTeacherRatings: Long? = null,
    val todayResponses: Long? = null,
    val todayFeedbacks: Long? = null,
)

/* ===========  课程反馈  =========== */

@JsonClass(generateAdapter = true)
data class CourseFeedback(
    val id: Long? = null,
    val courseId: Long,
    val studentId: Long,
    val content: String,
    val rating: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class CourseFeedbackDTO(
    val feedback: CourseFeedback? = null,
    val student: User? = null,
)

/* ===========  问卷  =========== */

@JsonClass(generateAdapter = true)
data class Questionnaires(
    val id: Long,
    val title: String? = null,
    val description: String? = null,
    val createdBy: Long? = null,
    val questions: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireWithStatusDTO(
    val questionnaire: Questionnaires? = null,
    val status: Int? = null,
    val statusDescription: String? = null,
    val hasSubmitted: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireFullInfoDTO(
    val questionnaire: Questionnaires? = null,
    val course: Courses? = null,
    val teacher: User? = null,
    val status: Int? = null,
    val statusDescription: String? = null,
    val hasSubmitted: Boolean? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireResponseDTO(
    val id: Long? = null,
    val questionnaire: Questionnaires? = null,
    val answers: String? = null,
    val submittedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireResponses(
    val id: Long? = null,
    val courseId: Long,
    val questionnaireId: Long,
    val studentId: Long,
    val answers: String,
    val submittedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireSubmissionStatsDTO(
    val questionnaire: Questionnaires? = null,
    val status: Int? = null,
    val statusDescription: String? = null,
    val totalStudents: Long? = null,
    val submittedCount: Long? = null,
    val submissionRate: Double? = null,
)

@JsonClass(generateAdapter = true)
data class QuestionnaireResponseSummaryDTO(
    val questionnaire: Questionnaires? = null,
    val totalQuestions: Int? = null,
    val totalResponses: Int? = null,
    val completionRate: Double? = null,
    val answers: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class CourseQuestionnaireDTO(
    val courseId: Long,
    val questionnaireId: Long,
    val status: Int? = null,
)

/* ===========  课程资料  =========== */

@JsonClass(generateAdapter = true)
data class CourseResource(
    val id: Long? = null,
    val courseId: Long,
    val uploaderId: Long? = null,
    val uploaderRole: String? = null,
    val title: String? = null,
    val description: String? = null,
    val fileId: Long? = null,
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileType: String? = null,
    val fileSize: Long? = null,
    val category: String? = null,
    val downloadCount: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/* ===========  作业  =========== */

@JsonClass(generateAdapter = true)
data class Assignment(
    val id: Long? = null,
    val courseId: Long,
    val teacherId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val deadline: String? = null,
    val totalScore: Int? = null,
    val status: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssignmentSubmission(
    val id: Long? = null,
    val assignmentId: Long,
    val studentId: Long,
    val content: String? = null,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val score: Int? = null,
    val comment: String? = null,
    val submittedAt: String? = null,
    val gradedAt: String? = null,
)

/* ===========  教师评分  =========== */

@JsonClass(generateAdapter = true)
data class TeacherRating(
    val id: Long? = null,
    val courseId: Long,
    val teacherId: Long,
    val studentId: Long? = null,
    val rating: Int? = null,
    val teachingScore: Int? = null,
    val attitudeScore: Int? = null,
    val contentScore: Int? = null,
    val comment: String? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class TeacherRatingItem(
    val id: Long? = null,
    val courseId: Long? = null,
    val teacherId: Long? = null,
    val rating: Int? = null,
    val teachingScore: Int? = null,
    val attitudeScore: Int? = null,
    val contentScore: Int? = null,
    val comment: String? = null,
    val createdAt: String? = null,
    val anonymousId: String? = null,
)

@JsonClass(generateAdapter = true)
data class TeacherRatingStats(
    val teacherId: Long? = null,
    val avgRating: Double? = null,
    val avgTeaching: Double? = null,
    val avgAttitude: Double? = null,
    val avgContent: Double? = null,
    val total: Long? = null,
    val distribution: List<Map<String, Any?>>? = null,
)

/* ===========  问答区  =========== */

@JsonClass(generateAdapter = true)
data class QaPost(
    val id: Long? = null,
    val courseId: Long,
    val authorId: Long? = null,
    val authorRole: String? = null,
    val title: String? = null,
    val content: String? = null,
    val viewCount: Int? = null,
    val replyCount: Int? = null,
    val isResolved: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QaReply(
    val id: Long? = null,
    val postId: Long,
    val authorId: Long? = null,
    val authorRole: String? = null,
    val content: String? = null,
    val parentId: Long? = null,
    val isAccepted: Int? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class QaPostDetail(
    val post: QaPost? = null,
    val replies: List<QaReply>? = null,
)

/* ===========  AI 助手  =========== */

@JsonClass(generateAdapter = true)
data class ChatTurn(
    val role: String,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class ChatRequestDTO(
    val userId: Long? = null,
    val role: String? = null,
    val sessionId: String? = null,
    val message: String,
    val courseId: Long? = null,
    val scene: String? = null,
    val history: List<ChatTurn>? = null,
)

@JsonClass(generateAdapter = true)
data class KbChunk(
    val id: Long? = null,
    val sourceType: String? = null,
    val sourceId: Long? = null,
    val courseId: Long? = null,
    val title: String? = null,
    val content: String? = null,
    val keywords: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatResponseDTO(
    val sessionId: String? = null,
    val reply: String? = null,
    val citations: List<KbChunk>? = null,
    val latencyMs: Long? = null,
)

/* ===========  数据分析  =========== */

@JsonClass(generateAdapter = true)
data class CourseDashboard(
    val totalFeedback: Int? = null,
    val avgRating: Double? = null,
    val ratingHistogram: List<Int>? = null,
    val totalStudents: Long? = null,
    val totalQuestionnaireSubmissions: Long? = null,
)

/* ===========  选课工作流  =========== */

/**
 * 选课关系：sys_course_students 表
 *  status: pending / approved / rejected
 *  source: student_apply / teacher_invite
 */
@JsonClass(generateAdapter = true)
data class Enrollment(
    val id: Long? = null,
    val courseId: Long? = null,
    val studentId: Long? = null,
    val status: String? = null,
    val source: String? = null,
    val applyMessage: String? = null,
    val rejectReason: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String? = null,
)

/** 学生我的选课关系页一行：关系 + 课程详情 */
@JsonClass(generateAdapter = true)
data class StudentEnrollmentRow(
    val enrollment: Enrollment? = null,
    val course: Courses? = null,
)

/** 教师课程学生 Tab 一行：关系 + 学生详情 */
@JsonClass(generateAdapter = true)
data class TeacherEnrollmentRow(
    val enrollment: Enrollment? = null,
    val student: User? = null,
)

@JsonClass(generateAdapter = true)
data class StudentApplyRequest(
    val courseId: Long,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class TeacherInviteRequest(
    val courseId: Long,
    val studentId: Long? = null,
    val username: String? = null,
)

@JsonClass(generateAdapter = true)
data class RejectReasonRequest(
    val reason: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val email: String? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
)

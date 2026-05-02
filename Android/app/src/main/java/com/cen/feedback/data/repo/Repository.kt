package com.cen.feedback.data.repo

import com.cen.feedback.data.api.ApiService
import com.cen.feedback.data.local.TokenStore
import com.cen.feedback.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val codeNum: Int, message: String) : RuntimeException(message)

private fun <T> ApiResult<T>.unwrap(): T {
    if (code != 200) throw ApiException(code, msg ?: "请求失败")
    @Suppress("UNCHECKED_CAST")
    return data as T
}

private fun <T> ApiResult<T>.unwrapOrNull(): T? {
    if (code != 200) throw ApiException(code, msg ?: "请求失败")
    return data
}

@Singleton
class FeedbackRepository @Inject constructor(
    private val api: ApiService,
    val tokenStore: TokenStore,
) {

    /* ========== 登录 / 注册 ========== */

    suspend fun login(username: String, password: String, role: String): UserDTO {
        val res = api.login(UserDTO(username = username, password = password, role = role)).unwrap()
        if (res.token.isNullOrBlank() || res.id == null) throw ApiException(500, "登录响应缺失字段")
        tokenStore.save(
            token = res.token,
            userId = res.id,
            username = res.username ?: username,
            nickname = res.nickname,
            role = res.role ?: role,
            avatar = res.avatarUrl,
        )
        return res
    }

    suspend fun register(username: String, password: String, role: String, nickname: String?, email: String?): UserDTO =
        api.register(UserDTO(username = username, password = password, role = role,
            nickname = nickname, email = email, roleId = if (role == "teacher") 3 else 2)).unwrap()

    suspend fun logout() = tokenStore.clear()

    /* ========== 用户 ========== */

    suspend fun getUserById(id: Long) = api.getUserById(id).unwrap()
    suspend fun listStudents() = api.listStudents().unwrap()
    suspend fun saveUser(user: User) = api.saveUser(user).unwrap()
    suspend fun editPassword(user: User) = api.editPassword(user).unwrap()

    /* ========== 课程 ========== */

    suspend fun studentCourses(studentId: Long) = api.studentCourseList(studentId).unwrap()
    /** 兼容老调用：教师视角看自己已通过的课程（默认仅 approved） */
    suspend fun teacherCourses(teacherId: Long) = api.listCourses(teacherId).unwrap()
    /** 教师查看自己提交过的所有课程（含 pending / rejected / approved） */
    suspend fun teacherAllCourses(teacherId: Long? = null) =
        api.teacherMyCourses(teacherId).unwrap()
    /** 教师提交课程申请（新建或重新提交） */
    suspend fun submitCourseProposal(req: CourseProposalRequest) =
        api.teacherSubmitCourse(req).unwrap()
    suspend fun saveCourse(c: Courses) = api.saveCourse(c).unwrap()
    suspend fun deleteCourse(c: Courses) = api.deleteCourse(c).unwrap()

    /* ========== 管理员：课程审批 ========== */
    suspend fun adminPendingCourses() = api.adminPendingCourses().unwrap()
    suspend fun adminApproveCourse(id: Long) = api.adminApproveCourse(id).unwrap()
    suspend fun adminRejectCourse(id: Long, reason: String) =
        api.adminRejectCourse(id, mapOf("reason" to reason)).unwrap()
    suspend fun adminDeleteCourse(id: Long) = api.adminDeleteCourse(id).unwrap()
    suspend fun adminAllCourses(status: String? = null) = api.listCourses(status = status).unwrap()

    /* ========== 管理员：用户与仪表盘 ========== */
    suspend fun adminDashboard() = api.adminDashboard().unwrap()
    suspend fun adminUsersPage(
        pageNum: Int = 1,
        pageSize: Int = 20,
        username: String = "",
        role: String = "",
        status: Int? = null,
    ) = api.adminUsersPage(pageNum, pageSize, username, role, status).unwrap()
    suspend fun adminDisableUser(id: Long) = api.adminDisableUser(id).unwrap()
    suspend fun adminEnableUser(id: Long) = api.adminEnableUser(id).unwrap()
    suspend fun adminResetUserPassword(id: Long, password: String? = null) =
        api.adminResetUserPassword(
            id,
            if (password != null) mapOf("password" to password) else emptyMap(),
        ).unwrap()
    suspend fun adminDeleteUser(id: Long) = api.adminDeleteUser(id).unwrap()

    /* ========== 课程反馈 ========== */

    suspend fun courseFeedbackList(courseId: Long) = api.courseFeedbackList(courseId).unwrap()
    suspend fun saveCourseFeedback(fb: CourseFeedback) = api.saveCourseFeedback(fb).unwrap()

    /* ========== 教师评分 ========== */

    suspend fun saveTeacherRating(r: TeacherRating) = api.saveTeacherRating(r).unwrap()
    suspend fun teacherRatings(teacherId: Long) = api.teacherRatings(teacherId).unwrap()
    suspend fun teacherRatingStats(teacherId: Long) = api.teacherRatingStats(teacherId).unwrap()
    suspend fun myTeacherRating(studentId: Long, courseId: Long, teacherId: Long) =
        api.myTeacherRating(studentId, courseId, teacherId).unwrapOrNull()

    /* ========== 课程问卷关联 ========== */

    suspend fun courseQuestionnaires(courseId: Long) = api.courseQuestionnaireList(courseId).unwrap()

    suspend fun questionnairesByStatus(courseId: Long, studentId: Long) =
        api.questionnairesByStatus(courseId, studentId).unwrap()

    suspend fun studentResponseDetail(courseId: Long, qId: Long, studentId: Long) =
        api.studentResponseDetail(courseId, qId, studentId).unwrapOrNull()

    suspend fun studentAllQuestionnaires(studentId: Long) =
        api.studentAllQuestionnaires(studentId).unwrap()

    suspend fun courseQuestionnaireStats(courseId: Long) =
        api.courseQuestionnaireStats(courseId).unwrap()

    suspend fun bindQuestionnaires(courseId: Long, ids: String) =
        api.bindQuestionnaires(courseId, ids).unwrap()

    suspend fun unbindCourseQuestionnaire(courseId: Long, qId: Long) =
        api.unbindCourseQuestionnaire(CourseQuestionnaireDTO(courseId, qId)).unwrap()

    suspend fun publishQuestionnaire(courseId: Long, qId: Long) =
        api.publishQuestionnaire(CourseQuestionnaireDTO(courseId, qId)).unwrap()

    suspend fun endQuestionnaire(courseId: Long, qId: Long) =
        api.endQuestionnaire(CourseQuestionnaireDTO(courseId, qId)).unwrap()

    suspend fun revokeQuestionnaire(courseId: Long, qId: Long) =
        api.revokeQuestionnaire(CourseQuestionnaireDTO(courseId, qId)).unwrap()

    /* ========== 问卷 ========== */

    suspend fun teacherQuestionnaires(teacherId: Long) =
        api.listQuestionnaires(teacherId).unwrap()

    /**
     * 兼容两种后端返回：
     *  1) 新版：data 是完整 Questionnaires 对象（Moshi 解析为 Map<String,Any?>）
     *  2) 旧版：data 是 boolean（true=成功）
     * 旧版返回 boolean 时无法拿到自增 id，调用方应判断 result.id > 0 才做后续绑定。
     */
    suspend fun saveQuestionnaire(q: Questionnaires): Questionnaires {
        val raw = api.saveQuestionnaire(q).unwrap()
        return when (raw) {
            is Map<*, *> -> {
                val m = raw as Map<*, *>
                Questionnaires(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    title = m["title"] as? String,
                    description = m["description"] as? String,
                    createdBy = (m["createdBy"] as? Number)?.toLong(),
                    questions = m["questions"] as? String,
                    createdAt = m["createdAt"] as? String,
                    updatedAt = m["updatedAt"] as? String,
                )
            }
            is Boolean -> {
                if (!raw) throw ApiException(500, "保存失败")
                q.copy(id = if (q.id > 0L) q.id else 0L)
            }
            else -> q.copy(id = if (q.id > 0L) q.id else 0L)
        }
    }

    suspend fun deleteQuestionnaire(q: Questionnaires) = api.deleteQuestionnaire(q).unwrap()

    /* ========== 问卷答案 ========== */

    suspend fun saveQuestionnaireResponse(r: QuestionnaireResponses) =
        api.saveQuestionnaireResponse(r).unwrap()

    suspend fun questionnaireAiAnalysis(courseId: Long, qId: Long) =
        api.questionnaireAiAnalysis(courseId, qId).unwrap()

    /* ========== 课程资料 ========== */

    suspend fun listResources(courseId: Long, category: String? = null) =
        api.listResources(courseId, category).unwrap()

    suspend fun saveResource(r: CourseResource) = api.saveResource(r).unwrap()

    suspend fun deleteResource(id: Long) = api.deleteResource(id).unwrap()

    suspend fun resourceDownloadHit(id: Long) = api.resourceDownloadHit(id).unwrap()

    /* ========== 作业 ========== */

    suspend fun assignmentsByCourse(courseId: Long) = api.assignmentsByCourse(courseId).unwrap()
    suspend fun assignmentsByStudent(studentId: Long) = api.assignmentsByStudent(studentId).unwrap()
    suspend fun saveAssignment(a: Assignment) = api.saveAssignment(a).unwrap()
    suspend fun deleteAssignment(id: Long) = api.deleteAssignment(id).unwrap()
    suspend fun submitAssignment(s: AssignmentSubmission) = api.submitAssignment(s).unwrap()
    suspend fun submissionsOf(assignmentId: Long) = api.submissionsOf(assignmentId).unwrap()
    suspend fun mySubmission(assignmentId: Long, studentId: Long) =
        api.mySubmission(assignmentId, studentId).unwrapOrNull()
    suspend fun gradeSubmission(s: AssignmentSubmission) = api.gradeSubmission(s).unwrap()

    /* ========== 问答区 ========== */

    suspend fun listPosts(courseId: Long) = api.listPosts(courseId).unwrap()
    suspend fun postDetail(postId: Long) = api.postDetail(postId).unwrap()
    suspend fun createPost(p: QaPost) = api.createPost(p).unwrap()
    suspend fun createReply(r: QaReply) = api.createReply(r).unwrap()
    suspend fun acceptReply(id: Long) = api.acceptReply(id).unwrap()

    /* ========== AI 助手 ========== */

    suspend fun chat(req: ChatRequestDTO) = api.chat(req).unwrap()

    suspend fun summarize(courseId: Long, qId: Long) =
        api.summarize(courseId, qId).unwrap()

    /* ========== 选课工作流 ========== */

    suspend fun discoverCourses(pageNum: Int = 1, pageSize: Int = 20, keyword: String = "") =
        api.discoverCourses(pageNum, pageSize, keyword).unwrap()

    suspend fun studentApplyEnroll(courseId: Long, message: String? = null) =
        api.studentApplyEnroll(StudentApplyRequest(courseId, message)).unwrap()

    suspend fun studentCancelEnrollment(id: Long) = api.studentCancelEnrollment(id).unwrap()

    suspend fun studentAcceptInvite(id: Long) = api.studentAcceptInvite(id).unwrap()

    suspend fun studentLeaveCourse(id: Long) = api.studentLeaveCourse(id).unwrap()

    suspend fun studentMyEnrollments(status: String? = null) =
        api.studentMyEnrollments(status).unwrap()

    suspend fun teacherCourseEnrollments(courseId: Long, status: String? = null) =
        api.teacherCourseEnrollments(courseId, status).unwrap()

    suspend fun teacherInviteStudent(courseId: Long, studentId: Long? = null, username: String? = null) =
        api.teacherInviteStudent(TeacherInviteRequest(courseId, studentId, username)).unwrap()

    suspend fun teacherApproveEnrollment(id: Long) = api.teacherApproveEnrollment(id).unwrap()

    suspend fun teacherRejectEnrollment(id: Long, reason: String? = null) =
        api.teacherRejectEnrollment(id, RejectReasonRequest(reason)).unwrap()

    suspend fun teacherRemoveEnrollment(id: Long) = api.teacherRemoveEnrollment(id).unwrap()

    /* ========== 账户安全 ========== */

    suspend fun me() = api.me().unwrap()

    suspend fun changeMyPassword(oldPwd: String, newPwd: String) =
        api.changeMyPassword(ChangePasswordRequest(oldPwd, newPwd)).unwrap()

    suspend fun updateMyProfile(email: String? = null, nickname: String? = null, avatarUrl: String? = null) =
        api.updateMyProfile(UpdateProfileRequest(email, nickname, avatarUrl)).unwrap()

    /* ========== 分析 ========== */

    suspend fun courseDashboard(courseId: Long) = api.courseDashboard(courseId).unwrap()

    /* ========== 文件上传 ========== */

    /** 上传文件，返回服务器存储的 url */
    suspend fun uploadFile(file: File, mime: String? = null): String {
        val type = (mime ?: guessMime(file.extension)).toMediaTypeOrNull()
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(type))
        val resp = api.uploadFile(part)
        if (!resp.isSuccessful) throw ApiException(resp.code(), "文件上传失败")
        return resp.body()?.string()?.trim() ?: throw ApiException(500, "文件上传无响应")
    }

    private fun guessMime(ext: String) = when (ext.lowercase()) {
        "pdf" -> "application/pdf"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "doc", "docx" -> "application/msword"
        "mp4", "mov" -> "video/mp4"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "zip", "rar" -> "application/zip"
        else -> "application/octet-stream"
    }
}

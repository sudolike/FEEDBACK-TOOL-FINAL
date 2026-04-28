package com.cen.feedback.data.api

import com.cen.feedback.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * 后端 REST API 接口。
 * 服务器返回结构：{ code, msg, data }，由 ApiResult<T> 统一包装。
 */
interface ApiService {

    /* ========== 登录 / 注册 / 用户 ========== */

    @POST("login")
    suspend fun login(@Body body: UserDTO): ApiResult<UserDTO>

    @POST("register")
    suspend fun register(@Body body: UserDTO): ApiResult<UserDTO>

    @GET("user/getById")
    suspend fun getUserById(@Query("id") id: Long): ApiResult<User>

    @POST("user/save")
    suspend fun saveUser(@Body user: User): ApiResult<Any>

    @POST("user/edit/pow")
    suspend fun editPassword(@Body user: User): ApiResult<Any>

    @GET("user/student/list")
    suspend fun listStudents(): ApiResult<List<User>>

    /* ========== 课程 ========== */

    @GET("courses/list")
    suspend fun listCourses(
        @Query("teacherId") teacherId: Long? = null,
        @Query("status") status: String? = null,
    ): ApiResult<List<Courses>>

    @GET("courses/studentList")
    suspend fun studentCourseList(@Query("studentId") studentId: Long): ApiResult<List<Courses>>

    @GET("courses/student/{id}")
    suspend fun coursesByStudent(@Path("id") studentId: Long): ApiResult<List<Courses>>

    /** 教师端：查看自己提交过的所有课程（含 pending / rejected） */
    @GET("courses/teacher/my")
    suspend fun teacherMyCourses(
        @Query("teacherId") teacherId: Long? = null,
    ): ApiResult<List<Courses>>

    /** 教师端：申请新建 / 重新提交课程（必走审批） */
    @POST("courses/teacher/submit")
    suspend fun teacherSubmitCourse(@Body req: CourseProposalRequest): ApiResult<Courses>

    @POST("courses/save")
    suspend fun saveCourse(@Body course: Courses): ApiResult<Any>

    @POST("courses/delete")
    suspend fun deleteCourse(@Body course: Courses): ApiResult<Any>

    /* ========== 管理员：课程审批 ========== */

    @GET("courses/admin/pending")
    suspend fun adminPendingCourses(): ApiResult<List<PendingCourseRow>>

    @POST("courses/admin/approve/{id}")
    suspend fun adminApproveCourse(@Path("id") id: Long): ApiResult<Courses>

    @POST("courses/admin/reject/{id}")
    suspend fun adminRejectCourse(
        @Path("id") id: Long,
        @Body body: Map<String, String>,
    ): ApiResult<Courses>

    @POST("courses/admin/delete/{id}")
    suspend fun adminDeleteCourse(@Path("id") id: Long): ApiResult<Any>

    /* ========== 管理员：用户与仪表盘 ========== */

    @GET("admin/dashboard")
    suspend fun adminDashboard(): ApiResult<AdminDashboard>

    @GET("admin/users/page")
    suspend fun adminUsersPage(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("username") username: String = "",
        @Query("role") role: String = "",
        @Query("status") status: Int? = null,
    ): ApiResult<PageResult<User>>

    @POST("admin/users/{id}/disable")
    suspend fun adminDisableUser(@Path("id") id: Long): ApiResult<Any>

    @POST("admin/users/{id}/enable")
    suspend fun adminEnableUser(@Path("id") id: Long): ApiResult<Any>

    @POST("admin/users/{id}/reset-password")
    suspend fun adminResetUserPassword(
        @Path("id") id: Long,
        @Body body: Map<String, String> = emptyMap(),
    ): ApiResult<Map<String, String>>

    @POST("admin/users/{id}/delete")
    suspend fun adminDeleteUser(@Path("id") id: Long): ApiResult<Any>

    /* ========== 课程反馈 ========== */

    @POST("courseFeedback/save")
    suspend fun saveCourseFeedback(@Body fb: CourseFeedback): ApiResult<Any>

    @GET("courseFeedback/course/{id}")
    suspend fun courseFeedbackList(@Path("id") courseId: Long): ApiResult<List<CourseFeedbackDTO>>

    /* ========== 教师评分 ========== */

    @POST("teacherRating/save")
    suspend fun saveTeacherRating(@Body r: TeacherRating): ApiResult<Any>

    @GET("teacherRating/teacher/{id}")
    suspend fun teacherRatings(@Path("id") teacherId: Long): ApiResult<List<TeacherRatingItem>>

    @GET("teacherRating/stats/{id}")
    suspend fun teacherRatingStats(@Path("id") teacherId: Long): ApiResult<TeacherRatingStats>

    @GET("teacherRating/my")
    suspend fun myTeacherRating(
        @Query("studentId") studentId: Long,
        @Query("courseId") courseId: Long,
        @Query("teacherId") teacherId: Long,
    ): ApiResult<TeacherRating?>

    /* ========== 课程问卷关联 ========== */

    @GET("courseQuestionnaire/course/{courseId}")
    suspend fun courseQuestionnaireList(
        @Path("courseId") courseId: Long
    ): ApiResult<List<QuestionnaireWithStatusDTO>>

    @POST("courseQuestionnaire/bind/{courseId}")
    suspend fun bindQuestionnaires(
        @Path("courseId") courseId: Long,
        @Query("questionnaireIds") ids: String,
    ): ApiResult<Any>

    @POST("courseQuestionnaire/unbind")
    suspend fun unbindCourseQuestionnaire(@Body dto: CourseQuestionnaireDTO): ApiResult<Any>

    @POST("courseQuestionnaire/publish")
    suspend fun publishQuestionnaire(@Body dto: CourseQuestionnaireDTO): ApiResult<Any>

    @POST("courseQuestionnaire/end")
    suspend fun endQuestionnaire(@Body dto: CourseQuestionnaireDTO): ApiResult<Any>

    @POST("courseQuestionnaire/revoke")
    suspend fun revokeQuestionnaire(@Body dto: CourseQuestionnaireDTO): ApiResult<Any>

    @GET("courseQuestionnaire/status/{courseId}")
    suspend fun questionnairesByStatus(
        @Path("courseId") courseId: Long,
        @Query("studentId") studentId: Long,
    ): ApiResult<Map<String, List<QuestionnaireWithStatusDTO>>>

    @GET("courseQuestionnaire/response")
    suspend fun studentResponseDetail(
        @Query("courseId") courseId: Long,
        @Query("questionnaireId") qId: Long,
        @Query("studentId") studentId: Long,
    ): ApiResult<QuestionnaireResponseDTO?>

    @GET("courseQuestionnaire/student/questionnaires")
    suspend fun studentAllQuestionnaires(
        @Query("studentId") studentId: Long
    ): ApiResult<List<QuestionnaireFullInfoDTO>>

    @GET("courseQuestionnaire/stats/{courseId}")
    suspend fun courseQuestionnaireStats(
        @Path("courseId") courseId: Long
    ): ApiResult<List<QuestionnaireSubmissionStatsDTO>>

    /* ========== 问卷 ========== */

    @GET("questionnaires/list")
    suspend fun listQuestionnaires(
        @Query("teacherId") teacherId: Long? = null
    ): ApiResult<List<Questionnaires>>

    @POST("questionnaires/save")
    suspend fun saveQuestionnaire(@Body q: Questionnaires): ApiResult<Questionnaires>

    @POST("questionnaires/delete")
    suspend fun deleteQuestionnaire(@Body q: Questionnaires): ApiResult<Any>

    /* ========== 问卷答案 ========== */

    @POST("questionnaireResponses/save")
    suspend fun saveQuestionnaireResponse(@Body r: QuestionnaireResponses): ApiResult<Any>

    @GET("questionnaireResponses/analysis")
    suspend fun questionnaireAiAnalysis(
        @Query("courseId") courseId: Long,
        @Query("questionnaireId") qId: Long,
    ): ApiResult<Map<String, Any?>>

    @GET("questionnaireResponses/FillinDetails")
    suspend fun questionnaireFillinDetails(
        @Query("courseId") courseId: Long,
        @Query("questionnaireId") qId: Long,
    ): ApiResult<Any>

    /* ========== 课程资料 ========== */

    @POST("courseResource/save")
    suspend fun saveResource(@Body r: CourseResource): ApiResult<Any>

    @POST("courseResource/delete/{id}")
    suspend fun deleteResource(@Path("id") id: Long): ApiResult<Any>

    @GET("courseResource/course/{id}")
    suspend fun listResources(
        @Path("id") courseId: Long,
        @Query("category") category: String? = null,
    ): ApiResult<List<CourseResource>>

    @POST("courseResource/download/{id}")
    suspend fun resourceDownloadHit(@Path("id") id: Long): ApiResult<Any>

    /* ========== 作业 ========== */

    @POST("assignment/save")
    suspend fun saveAssignment(@Body a: Assignment): ApiResult<Any>

    @POST("assignment/delete/{id}")
    suspend fun deleteAssignment(@Path("id") id: Long): ApiResult<Any>

    @GET("assignment/course/{id}")
    suspend fun assignmentsByCourse(@Path("id") courseId: Long): ApiResult<List<Assignment>>

    @GET("assignment/student/{id}")
    suspend fun assignmentsByStudent(@Path("id") studentId: Long): ApiResult<List<Assignment>>

    @POST("assignment/submission/save")
    suspend fun submitAssignment(@Body s: AssignmentSubmission): ApiResult<Any>

    @GET("assignment/submission/list/{id}")
    suspend fun submissionsOf(@Path("id") assignmentId: Long): ApiResult<List<AssignmentSubmission>>

    @GET("assignment/submission/my")
    suspend fun mySubmission(
        @Query("assignmentId") assignmentId: Long,
        @Query("studentId") studentId: Long,
    ): ApiResult<AssignmentSubmission?>

    @POST("assignment/submission/grade")
    suspend fun gradeSubmission(@Body s: AssignmentSubmission): ApiResult<Any>

    /* ========== 问答区 ========== */

    @POST("qa/post")
    suspend fun createPost(@Body post: QaPost): ApiResult<Any>

    @POST("qa/reply")
    suspend fun createReply(@Body reply: QaReply): ApiResult<Any>

    @GET("qa/posts/{id}")
    suspend fun listPosts(@Path("id") courseId: Long): ApiResult<List<QaPost>>

    @GET("qa/post/{id}")
    suspend fun postDetail(@Path("id") postId: Long): ApiResult<QaPostDetail>

    @POST("qa/accept/{id}")
    suspend fun acceptReply(@Path("id") replyId: Long): ApiResult<Any>

    /* ========== AI 助手 ========== */

    @POST("assistant/chat")
    suspend fun chat(@Body req: ChatRequestDTO): ApiResult<ChatResponseDTO>

    @POST("assistant/recommend")
    suspend fun recommend(@Body body: Map<String, Any?>): ApiResult<Any>

    @GET("assistant/summarize")
    suspend fun summarize(
        @Query("courseId") courseId: Long,
        @Query("questionnaireId") qId: Long,
    ): ApiResult<Map<String, Any?>>

    /* ========== 选课工作流（学生申请 / 教师邀请） ========== */

    /** 学生发现课程：分页 + 关键词 */
    @GET("courses/discover")
    suspend fun discoverCourses(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("keyword") keyword: String = "",
    ): ApiResult<PageResult<Courses>>

    @POST("enrollments/student/apply")
    suspend fun studentApplyEnroll(@Body req: StudentApplyRequest): ApiResult<Enrollment>

    @POST("enrollments/student/cancel/{id}")
    suspend fun studentCancelEnrollment(@Path("id") id: Long): ApiResult<Any>

    @POST("enrollments/student/accept/{id}")
    suspend fun studentAcceptInvite(@Path("id") id: Long): ApiResult<Enrollment>

    @POST("enrollments/student/leave/{id}")
    suspend fun studentLeaveCourse(@Path("id") id: Long): ApiResult<Any>

    @GET("enrollments/student/my")
    suspend fun studentMyEnrollments(
        @Query("status") status: String? = null
    ): ApiResult<List<StudentEnrollmentRow>>

    @GET("enrollments/teacher/course/{courseId}")
    suspend fun teacherCourseEnrollments(
        @Path("courseId") courseId: Long,
        @Query("status") status: String? = null,
    ): ApiResult<List<TeacherEnrollmentRow>>

    @POST("enrollments/teacher/invite")
    suspend fun teacherInviteStudent(@Body req: TeacherInviteRequest): ApiResult<Enrollment>

    @POST("enrollments/teacher/approve/{id}")
    suspend fun teacherApproveEnrollment(@Path("id") id: Long): ApiResult<Enrollment>

    @POST("enrollments/teacher/reject/{id}")
    suspend fun teacherRejectEnrollment(
        @Path("id") id: Long,
        @Body body: RejectReasonRequest,
    ): ApiResult<Enrollment>

    @POST("enrollments/teacher/remove/{id}")
    suspend fun teacherRemoveEnrollment(@Path("id") id: Long): ApiResult<Any>

    /* ========== 账户安全 ========== */

    @GET("user/me")
    suspend fun me(): ApiResult<User>

    @POST("user/me/password")
    suspend fun changeMyPassword(@Body req: ChangePasswordRequest): ApiResult<Any>

    @POST("user/me/profile")
    suspend fun updateMyProfile(@Body req: UpdateProfileRequest): ApiResult<User>

    /* ========== 数据分析 ========== */

    @GET("analytics/course/{id}/dashboard")
    suspend fun courseDashboard(@Path("id") courseId: Long): ApiResult<CourseDashboard>

    /* ========== 文件 ========== */

    @Multipart
    @POST("file/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): retrofit2.Response<okhttp3.ResponseBody>
}

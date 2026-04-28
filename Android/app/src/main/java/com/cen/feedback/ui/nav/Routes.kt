package com.cen.feedback.ui.nav

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    /* === 学生端 === */
    const val STUDENT_HOME = "student/home"
    const val STUDENT_COURSES = "student/courses"
    const val STUDENT_QUESTIONNAIRES = "student/questionnaires"
    const val STUDENT_CALENDAR = "student/calendar"
    const val STUDENT_PROFILE = "student/profile"

    fun courseDetail(courseId: Long) = "student/course/$courseId"
    const val COURSE_DETAIL_PATTERN = "student/course/{courseId}"

    fun questionnaireFill(courseId: Long, qId: Long) = "student/q/$courseId/$qId"
    const val QUESTIONNAIRE_FILL_PATTERN = "student/q/{courseId}/{qId}"

    fun rateTeacher(courseId: Long, teacherId: Long) = "student/rate/$courseId/$teacherId"
    const val RATE_TEACHER_PATTERN = "student/rate/{courseId}/{teacherId}"

    fun courseQa(courseId: Long) = "student/qa/$courseId"
    const val COURSE_QA_PATTERN = "student/qa/{courseId}"

    fun postDetail(postId: Long) = "qa/post/$postId"
    const val POST_DETAIL_PATTERN = "qa/post/{postId}"

    fun assignmentDetail(assignmentId: Long) = "assignment/$assignmentId"
    const val ASSIGNMENT_DETAIL_PATTERN = "assignment/{assignmentId}"

    /* === 教师端 === */
    const val TEACHER_HOME = "teacher/home"
    const val TEACHER_COURSES = "teacher/courses"
    const val TEACHER_QUESTIONNAIRES = "teacher/questionnaires"
    const val TEACHER_PROFILE = "teacher/profile"

    fun teacherCourseDetail(courseId: Long) = "teacher/course/$courseId"
    const val TEACHER_COURSE_DETAIL_PATTERN = "teacher/course/{courseId}"

    fun questionnaireEditor(qId: Long?) = if (qId == null) "teacher/q/editor/-1" else "teacher/q/editor/$qId"
    const val QUESTIONNAIRE_EDITOR_PATTERN = "teacher/q/editor/{qId}"

    fun teacherAnalysis(courseId: Long, qId: Long) = "teacher/analysis/$courseId/$qId"
    const val TEACHER_ANALYSIS_PATTERN = "teacher/analysis/{courseId}/{qId}"

    fun assignmentEditor(courseId: Long, assignmentId: Long?) =
        "teacher/assignment/editor/$courseId/${assignmentId ?: -1}"
    const val ASSIGNMENT_EDITOR_PATTERN = "teacher/assignment/editor/{courseId}/{assignmentId}"

    /** 教师申请新建/重新提交课程的表单 */
    fun teacherCoursePropose(editingId: Long?) =
        "teacher/course/propose/${editingId ?: -1}"
    const val TEACHER_COURSE_PROPOSE_PATTERN = "teacher/course/propose/{editingId}"

    /* === 学生：发现 / 申请加入课程 === */
    const val STUDENT_DISCOVER = "student/discover"
    const val STUDENT_ENROLLMENTS = "student/enrollments"

    /* === 通用 / 我的菜单 === */
    const val ACCOUNT_SECURITY = "common/account-security"
    const val NOTIFICATIONS = "common/notifications"
    const val BOOKMARKS = "common/bookmarks"
    const val ASSISTANT_SETTINGS = "common/assistant-settings"
    const val HELP_FEEDBACK = "common/help-feedback"
    const val TEACHER_ASSISTANT = "teacher/assistant"

    /* === 媒体预览（需求 13 · 不改后端，仅 UI 壳） === */
    fun mediaPlayer(url: String, title: String? = null): String {
        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
        val encodedTitle = java.net.URLEncoder.encode(title.orEmpty(), "UTF-8")
        return "common/media?url=$encodedUrl&title=$encodedTitle"
    }
    const val MEDIA_PLAYER_PATTERN = "common/media?url={url}&title={title}"

    /* === 管理员端 === */
    const val ADMIN_HOME = "admin/home"
}

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

    /**
     * 问卷编辑器路由：可选 bindCourseId（保存后自动绑定到该课程并发布）。
     * - 仅编辑模板：questionnaireEditor(qId, null)
     * - 课内新建并发布：questionnaireEditor(null, courseId)
     */
    fun questionnaireEditor(qId: Long?, bindCourseId: Long? = null) =
        "teacher/q/editor/${qId ?: -1}/${bindCourseId ?: -1}"
    const val QUESTIONNAIRE_EDITOR_PATTERN = "teacher/q/editor/{qId}/{bindCourseId}"

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

    /* === 管理员端 === */
    const val ADMIN_HOME = "admin/home"
}

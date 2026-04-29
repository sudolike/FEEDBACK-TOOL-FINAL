package com.cen.feedback.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cen.feedback.ui.admin.AdminMainScaffold
import com.cen.feedback.ui.auth.LoginScreen
import com.cen.feedback.ui.auth.RegisterScreen
import com.cen.feedback.ui.common.AccountSecurityScreen
import com.cen.feedback.ui.common.AssistantSettingsScreen
import com.cen.feedback.ui.common.BookmarksScreen
import com.cen.feedback.ui.common.HelpFeedbackScreen
import com.cen.feedback.ui.common.NotificationsScreen
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.student.StudentMainScaffold
import com.cen.feedback.ui.student.calendar.CalendarScreen
import com.cen.feedback.ui.student.course.CourseDetailScreen
import com.cen.feedback.ui.student.course.StudentDiscoverCoursesScreen
import com.cen.feedback.ui.student.qa.PostDetailScreen
import com.cen.feedback.ui.student.questionnaire.QuestionnaireFillScreen
import com.cen.feedback.ui.student.qa.CourseQaScreen
import com.cen.feedback.ui.student.assignment.AssignmentDetailScreen
import com.cen.feedback.ui.student.rate.RateTeacherScreen
import com.cen.feedback.ui.teacher.TeacherMainScaffold
import com.cen.feedback.ui.teacher.analysis.TeacherAnalysisScreen
import com.cen.feedback.ui.teacher.assignment.AssignmentEditorScreen
import com.cen.feedback.ui.teacher.assistant.TeacherAssistantScreen
import com.cen.feedback.ui.teacher.course.TeacherCourseDetailScreen
import com.cen.feedback.ui.teacher.course.TeacherCourseProposeScreen
import com.cen.feedback.ui.teacher.questionnaire.QuestionnaireEditorScreen

/**
 * 顶层路由：
 *  - 未登录 → 登录/注册
 *  - 登录后角色 = student → 学生导航
 *  - 登录后角色 = teacher → 教师导航
 */
@Composable
fun AppRoot(session: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val loginState by session.uiState.collectAsStateWithLifecycle()

    val startDestination = remember(loginState.role, loginState.token) {
        when {
            loginState.token.isNullOrBlank() -> Routes.LOGIN
            loginState.role == "admin"   -> Routes.ADMIN_HOME
            loginState.role == "teacher" -> Routes.TEACHER_HOME
            else -> Routes.STUDENT_HOME
        }
    }

    LaunchedEffect(loginState.token, loginState.role) {
        val target = when {
            loginState.token.isNullOrBlank() -> Routes.LOGIN
            loginState.role == "admin"   -> Routes.ADMIN_HOME
            loginState.role == "teacher" -> Routes.TEACHER_HOME
            else -> Routes.STUDENT_HOME
        }
        if (navController.currentBackStackEntry != null) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 6 } },
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onRegister = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onBack = { navController.popBackStack() })
        }

        /* === 学生端 === */
        composable(Routes.STUDENT_HOME) { StudentMainScaffold(navController) }
        composable(
            Routes.COURSE_DETAIL_PATTERN,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("courseId") ?: 0L
            CourseDetailScreen(courseId = id, navController = navController)
        }
        composable(
            Routes.QUESTIONNAIRE_FILL_PATTERN,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("qId") { type = NavType.LongType },
            ),
        ) { entry ->
            val cId = entry.arguments?.getLong("courseId") ?: 0L
            val qId = entry.arguments?.getLong("qId") ?: 0L
            QuestionnaireFillScreen(courseId = cId, questionnaireId = qId, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RATE_TEACHER_PATTERN,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("teacherId") { type = NavType.LongType },
            ),
        ) { entry ->
            val cId = entry.arguments?.getLong("courseId") ?: 0L
            val tId = entry.arguments?.getLong("teacherId") ?: 0L
            RateTeacherScreen(courseId = cId, teacherId = tId, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.COURSE_QA_PATTERN,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType }),
        ) { entry ->
            CourseQaScreen(
                courseId = entry.arguments?.getLong("courseId") ?: 0L,
                navController = navController,
            )
        }
        composable(
            Routes.POST_DETAIL_PATTERN,
            arguments = listOf(navArgument("postId") { type = NavType.LongType }),
        ) { entry ->
            PostDetailScreen(
                postId = entry.arguments?.getLong("postId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.ASSIGNMENT_DETAIL_PATTERN,
            arguments = listOf(navArgument("assignmentId") { type = NavType.LongType }),
        ) { entry ->
            AssignmentDetailScreen(
                assignmentId = entry.arguments?.getLong("assignmentId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }

        /* === 教师端 === */
        composable(Routes.TEACHER_HOME) { TeacherMainScaffold(navController) }
        composable(
            Routes.TEACHER_COURSE_DETAIL_PATTERN,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType }),
        ) { entry ->
            TeacherCourseDetailScreen(
                courseId = entry.arguments?.getLong("courseId") ?: 0L,
                navController = navController,
            )
        }
        composable(
            Routes.QUESTIONNAIRE_EDITOR_PATTERN,
            arguments = listOf(
                navArgument("qId") { type = NavType.LongType },
                navArgument("bindCourseId") { type = NavType.LongType },
            ),
        ) { entry ->
            val qId = entry.arguments?.getLong("qId") ?: -1L
            val bindCid = entry.arguments?.getLong("bindCourseId") ?: -1L
            QuestionnaireEditorScreen(
                editingId = if (qId > 0) qId else null,
                bindCourseId = if (bindCid > 0) bindCid else null,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.TEACHER_ANALYSIS_PATTERN,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("qId") { type = NavType.LongType },
            ),
        ) { entry ->
            TeacherAnalysisScreen(
                courseId = entry.arguments?.getLong("courseId") ?: 0L,
                qId = entry.arguments?.getLong("qId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.ASSIGNMENT_EDITOR_PATTERN,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("assignmentId") { type = NavType.LongType },
            ),
        ) { entry ->
            val cId = entry.arguments?.getLong("courseId") ?: 0L
            val aId = entry.arguments?.getLong("assignmentId") ?: -1L
            AssignmentEditorScreen(
                courseId = cId,
                editingId = if (aId > 0) aId else null,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.TEACHER_COURSE_PROPOSE_PATTERN,
            arguments = listOf(navArgument("editingId") { type = NavType.LongType }),
        ) { entry ->
            val eid = entry.arguments?.getLong("editingId") ?: -1L
            TeacherCourseProposeScreen(
                editingId = if (eid > 0) eid else null,
                onBack = { navController.popBackStack() },
            )
        }

        /* === 学生：发现 / 选课 === */
        composable(Routes.STUDENT_DISCOVER) { StudentDiscoverCoursesScreen(navController) }

        /* === 通用「我的」入口 === */
        composable(Routes.ACCOUNT_SECURITY) {
            AccountSecurityScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ASSISTANT_SETTINGS) {
            AssistantSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP_FEEDBACK) {
            HelpFeedbackScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_ASSISTANT) {
            TeacherAssistantScreen(onBack = { navController.popBackStack() })
        }

        /* === 媒体播放器（需求 13） === */
        composable(
            Routes.MEDIA_PLAYER_PATTERN,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val encoded = entry.arguments?.getString("url").orEmpty()
            val encodedTitle = entry.arguments?.getString("title").orEmpty()
            val url = java.net.URLDecoder.decode(encoded, "UTF-8")
            val title = java.net.URLDecoder.decode(encodedTitle, "UTF-8")
            com.cen.feedback.ui.common.MediaPlayerScreen(
                url = url,
                title = title.takeIf { it.isNotBlank() },
                onBack = { navController.popBackStack() },
            )
        }

        /* === 管理员端 === */
        composable(Routes.ADMIN_HOME) { AdminMainScaffold(navController) }
    }
}

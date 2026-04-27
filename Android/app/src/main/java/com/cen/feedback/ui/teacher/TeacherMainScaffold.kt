package com.cen.feedback.ui.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.R
import com.cen.feedback.ui.components.AiAssistantFab
import com.cen.feedback.ui.components.AppBottomTab
import com.cen.feedback.ui.components.RoleMainScaffold
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.student.ai.AiViewModel
import com.cen.feedback.ui.teacher.course.TeacherCourseListScreen
import com.cen.feedback.ui.teacher.dashboard.TeacherHomeScreen
import com.cen.feedback.ui.teacher.profile.TeacherProfileScreen
import com.cen.feedback.ui.teacher.questionnaire.TeacherQuestionnaireListScreen
import com.cen.feedback.ui.teacher.analysis.TeacherAnalysisHubScreen

enum class TeacherTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    Dashboard(R.string.tab_dashboard, Icons.Rounded.Dashboard),
    Courses(R.string.tab_courses, Icons.Rounded.Class),
    Questionnaires(R.string.tab_questionnaires, Icons.Rounded.Quiz),
    Analysis(R.string.tab_analysis, Icons.Rounded.Analytics),
    Profile(R.string.tab_profile, Icons.Rounded.Person),
}

@Composable
fun TeacherMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(TeacherTab.Dashboard) }
    val aiVm: AiViewModel = hiltViewModel()
    val aiState by aiVm.state.collectAsStateWithLifecycle()
    val tabs = remember {
        TeacherTab.values().map { AppBottomTab(titleRes = it.titleRes, icon = it.icon) }
    }
    val teacherQuickActions = listOf(
        R.string.ai_teacher_q1_label to R.string.ai_teacher_q1_prompt,
        R.string.ai_teacher_q2_label to R.string.ai_teacher_q2_prompt,
        R.string.ai_teacher_q3_label to R.string.ai_teacher_q3_prompt,
    ).map { (a, b) -> stringResource(a) to stringResource(b) }

    RoleMainScaffold(
        tabs = tabs,
        selectedIndex = tab.ordinal,
        onSelectTab = { tab = TeacherTab.values()[it] },
        floatingContent = {
            AiAssistantFab(
                messages = aiState.messages,
                sending = aiState.sending,
                onSend = aiVm::send,
                quickActions = teacherQuickActions,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "teacher-tab",
            ) { current ->
                when (current) {
                    TeacherTab.Dashboard -> TeacherHomeScreen(
                        navController = navController,
                        onJumpTab = { tab = it },
                    )
                    TeacherTab.Courses -> TeacherCourseListScreen(navController)
                    TeacherTab.Questionnaires -> TeacherQuestionnaireListScreen(navController)
                    TeacherTab.Analysis -> TeacherAnalysisHubScreen(navController)
                    TeacherTab.Profile -> TeacherProfileScreen(
                        onLogout = session::signOut,
                        onSwitchTab = { tab = it },
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
            }
        }
    }
}

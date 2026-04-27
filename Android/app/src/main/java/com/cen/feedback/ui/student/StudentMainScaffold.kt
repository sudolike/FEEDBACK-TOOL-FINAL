package com.cen.feedback.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionAnswer
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
import com.cen.feedback.ui.student.calendar.CalendarScreen
import com.cen.feedback.ui.student.home.StudentHomeScreen
import com.cen.feedback.ui.student.course.StudentCourseListScreen
import com.cen.feedback.ui.student.profile.StudentProfileScreen
import com.cen.feedback.ui.student.questionnaire.StudentQuestionnaireListScreen

enum class StudentTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    Home(R.string.tab_home, Icons.Rounded.Home),
    Courses(R.string.tab_courses, Icons.Rounded.LibraryBooks),
    Questionnaires(R.string.tab_questionnaires, Icons.Rounded.QuestionAnswer),
    Calendar(R.string.tab_calendar, Icons.Rounded.CalendarMonth),
    Profile(R.string.tab_profile, Icons.Rounded.Person),
}

@Composable
fun StudentMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(StudentTab.Home) }
    val aiVm: com.cen.feedback.ui.student.ai.AiViewModel = hiltViewModel()
    val aiState by aiVm.state.collectAsStateWithLifecycle()
    val tabs = remember {
        StudentTab.values().map { AppBottomTab(titleRes = it.titleRes, icon = it.icon) }
    }
    val studentQuickActions = listOf(
        R.string.ai_student_q1_label to R.string.ai_student_q1_prompt,
        R.string.ai_student_q2_label to R.string.ai_student_q2_prompt,
        R.string.ai_student_q3_label to R.string.ai_student_q3_prompt,
    ).map { (a, b) -> stringResource(a) to stringResource(b) }

    RoleMainScaffold(
        tabs = tabs,
        selectedIndex = tab.ordinal,
        onSelectTab = { tab = StudentTab.values()[it] },
        floatingContent = {
            AiAssistantFab(
                messages = aiState.messages,
                sending = aiState.sending,
                onSend = aiVm::send,
                quickActions = studentQuickActions,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "stu-tab",
            ) { current ->
                when (current) {
                    StudentTab.Home -> StudentHomeScreen(
                        navController = navController,
                        onJumpTab = { tab = it },
                    )
                    StudentTab.Courses -> StudentCourseListScreen(navController)
                    StudentTab.Questionnaires -> StudentQuestionnaireListScreen(navController)
                    StudentTab.Calendar -> CalendarScreen()
                    StudentTab.Profile -> StudentProfileScreen(
                        onLogout = session::signOut,
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
            }
        }
    }
}

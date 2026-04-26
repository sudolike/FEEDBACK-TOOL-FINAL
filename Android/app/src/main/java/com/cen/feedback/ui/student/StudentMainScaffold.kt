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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.ui.components.AiAssistantFab
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.student.calendar.CalendarScreen
import com.cen.feedback.ui.student.home.StudentHomeScreen
import com.cen.feedback.ui.student.course.StudentCourseListScreen
import com.cen.feedback.ui.student.profile.StudentProfileScreen
import com.cen.feedback.ui.student.questionnaire.StudentQuestionnaireListScreen

enum class StudentTab(val title: String, val icon: ImageVector) {
    Home("首页", Icons.Rounded.Home),
    Courses("课程", Icons.Rounded.LibraryBooks),
    Questionnaires("问卷", Icons.Rounded.QuestionAnswer),
    Calendar("日历", Icons.Rounded.CalendarMonth),
    Profile("我的", Icons.Rounded.Person),
}

@Composable
fun StudentMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(StudentTab.Home) }
    val sessionState by session.uiState.collectAsStateWithLifecycle()
    val aiVm: com.cen.feedback.ui.student.ai.AiViewModel = hiltViewModel()
    val aiState by aiVm.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
            ) {
                StudentTab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
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

            // 浮动 AI 助手
            Box(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)) {
                AiAssistantFab(
                    messages = aiState.messages,
                    sending = aiState.sending,
                    onSend = aiVm::send,
                )
            }
        }
    }
}

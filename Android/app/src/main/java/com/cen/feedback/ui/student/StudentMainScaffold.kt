package com.cen.feedback.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.R
import com.cen.feedback.ui.components.AiAssistantFab
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.student.calendar.CalendarScreen
import com.cen.feedback.ui.student.home.StudentHomeScreen
import com.cen.feedback.ui.student.course.StudentCourseListScreen
import com.cen.feedback.ui.student.profile.StudentProfileScreen
import com.cen.feedback.ui.student.questionnaire.StudentQuestionnaireListScreen

enum class StudentTab(
    val titleRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Home(R.string.tab_home, Icons.Rounded.Home, Icons.Filled.Home),
    Courses(R.string.tab_courses, Icons.Rounded.LibraryBooks, Icons.Filled.LibraryBooks),
    Questionnaires(R.string.tab_questionnaires, Icons.Rounded.QuestionAnswer, Icons.Filled.QuestionAnswer),
    Calendar(R.string.tab_calendar, Icons.Rounded.CalendarMonth, Icons.Filled.CalendarMonth),
    Profile(R.string.tab_profile, Icons.Rounded.Person, Icons.Filled.Person);

    /** 兼容旧代码访问 `.title` 的地方 */
    @Composable
    fun title(): String = stringResource(titleRes)
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
    val haptic = LocalHapticFeedback.current

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                StudentTab.values().forEach { item ->
                    val selected = tab == item
                    val scale by animateFloatAsState(
                        if (selected) 1f else 0.92f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab-scale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != item) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            tab = item
                        },
                        icon = {
                            Icon(
                                if (selected) item.iconSelected else item.icon,
                                null,
                                modifier = Modifier.scale(scale),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
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
                transitionSpec = {
                    // 水平滑入 12dp + fade
                    (slideInHorizontally { 40 } + fadeIn()) togetherWith fadeOut()
                },
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

            // 浮动 AI 助手 —— 需求 2 验收点 5：navigationBarsPadding
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
            ) {
                AiAssistantFab(
                    messages = aiState.messages,
                    sending = aiState.sending,
                    onSend = aiVm::send,
                )
            }
        }
    }
}

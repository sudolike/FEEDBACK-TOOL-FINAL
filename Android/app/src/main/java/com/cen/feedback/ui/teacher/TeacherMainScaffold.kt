package com.cen.feedback.ui.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
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
import com.cen.feedback.ui.student.ai.AiViewModel
import com.cen.feedback.ui.teacher.course.TeacherCourseListScreen
import com.cen.feedback.ui.teacher.dashboard.TeacherHomeScreen
import com.cen.feedback.ui.teacher.profile.TeacherProfileScreen
import com.cen.feedback.ui.teacher.questionnaire.TeacherQuestionnaireListScreen
import com.cen.feedback.ui.teacher.analysis.TeacherAnalysisHubScreen

enum class TeacherTab(
    val titleRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Dashboard(R.string.tab_dashboard, Icons.Rounded.Dashboard, Icons.Filled.Dashboard),
    Courses(R.string.tab_courses, Icons.Rounded.Class, Icons.Filled.Class),
    Questionnaires(R.string.tab_questionnaires, Icons.Rounded.Quiz, Icons.Filled.Quiz),
    Analysis(R.string.tab_analysis, Icons.Rounded.Analytics, Icons.Filled.Analytics),
    Profile(R.string.tab_profile, Icons.Rounded.Person, Icons.Filled.Person),
}

@Composable
fun TeacherMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(TeacherTab.Dashboard) }
    val aiVm: AiViewModel = hiltViewModel()
    val aiState by aiVm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                TeacherTab.values().forEach { item ->
                    val selected = tab == item
                    val scale by animateFloatAsState(
                        if (selected) 1f else 0.92f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab-scale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != item) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tab = item
                        },
                        icon = {
                            Icon(
                                if (selected) item.iconSelected else item.icon, null,
                                modifier = Modifier.scale(scale),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (slideInHorizontally { 40 } + fadeIn()) togetherWith fadeOut()
                },
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp)
            ) {
                AiAssistantFab(
                    messages = aiState.messages,
                    sending = aiState.sending,
                    onSend = aiVm::send,
                    quickActions = listOf(
                        "总结这次问卷反馈" to "请基于我最近发布的问卷反馈，总结学生评价并给出改进建议。",
                        "教学改进建议" to "结合最近的课评数据，给我教学改进建议。",
                        "起草问卷题目" to "请帮我针对刚结束的章节起草一份课程反馈问卷。",
                    ),
                )
            }
        }
    }
}

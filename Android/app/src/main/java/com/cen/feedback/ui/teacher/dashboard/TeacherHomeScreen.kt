package com.cen.feedback.ui.teacher.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.R
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.Questionnaires
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.teacher.TeacherTab
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUi(
    val loading: Boolean = false,
    val nickname: String = "",
    val courses: List<Courses> = emptyList(),
    val questionnaires: List<Questionnaires> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherHomeUi())
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            val nick = repo.tokenStore.username().orEmpty()
            val courses = runCatching { repo.teacherCourses(tid) }.getOrDefault(emptyList())
            val qs = runCatching { repo.teacherQuestionnaires(tid) }.getOrDefault(emptyList())
            Triple(nick, courses, qs)
        }.onSuccess { v ->
            _state.update { it.copy(loading = false, nickname = v.first,
                courses = v.second, questionnaires = v.third) }
        }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun TeacherHomeScreen(
    navController: NavController,
    onJumpTab: (TeacherTab) -> Unit,
    vm: TeacherHomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val heroTitle = if (s.nickname.isBlank()) {
        stringResource(R.string.teacher_home_title_fallback)
    } else {
        stringResource(R.string.teacher_home_title, s.nickname)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            DashboardHero(
                title = heroTitle,
                subtitle = stringResource(
                    R.string.teacher_home_subtitle,
                    s.courses.size,
                    s.questionnaires.size,
                ),
                hint = stringResource(R.string.teacher_home_hint_ai),
                icon = Icons.Rounded.Person,
                colors = listOf(Accent600, Primary600, Primary400),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.pagePadding, vertical = AppDimens.sectionVertical),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Class,
                    label = stringResource(R.string.teacher_home_metric_courses),
                    value = s.courses.size.toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = stringResource(R.string.teacher_home_metric_templates),
                    value = s.questionnaires.size.toString(),
                    accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionTitle(stringResource(R.string.teacher_home_section_courses), trailing = {
                TextButton(onClick = { onJumpTab(TeacherTab.Courses) }) {
                    Text(stringResource(R.string.action_manage))
                }
            })
        }
        if (s.courses.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.teacher_home_empty_courses_title),
                    subtitle = stringResource(R.string.teacher_home_empty_courses_subtitle),
                    icon = Icons.Rounded.Class,
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppDimens.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
                ) {
                    items(s.courses) { c ->
                        TeacherCourseTile(c) {
                            navController.navigate(Routes.teacherCourseDetail(c.id))
                        }
                    }
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.teacher_home_section_templates), trailing = {
                TextButton(onClick = { onJumpTab(TeacherTab.Questionnaires) }) {
                    Text(stringResource(R.string.action_view))
                }
            })
        }
        if (s.questionnaires.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.teacher_home_empty_templates_title),
                    subtitle = stringResource(R.string.teacher_home_empty_templates_subtitle),
                    icon = Icons.Rounded.Quiz,
                )
            }
        } else {
            items(s.questionnaires.take(5)) { q ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.pagePadding, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    onClick = { navController.navigate(Routes.questionnaireEditor(q.id)) },
                ) {
                    Row(modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Quiz, null, tint = Pink500)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                q.title ?: stringResource(R.string.placeholder_dash),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                q.description?.take(50).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherCourseTile(c: Courses, onClick: () -> Unit) {
    val dash = stringResource(R.string.placeholder_dash)
    val sem = c.semester?.let { stringResource(R.string.format_semester, it) }
    val fallback = stringResource(R.string.course_fallback_initial)
    Surface(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 300.dp)
            .heightIn(min = 130.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                contentAlignment = Alignment.Center,
            ) {
                Text((c.name ?: fallback).take(1), color = Color.White,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                c.name ?: dash,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(c.code, c.academicYear, sem).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

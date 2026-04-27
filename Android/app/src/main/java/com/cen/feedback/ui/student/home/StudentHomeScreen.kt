package com.cen.feedback.ui.student.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.R
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.QuestionnaireFullInfoDTO
import com.cen.feedback.ui.components.AppDimens
import com.cen.feedback.ui.components.DashboardHero
import com.cen.feedback.ui.components.EmptyState
import com.cen.feedback.ui.components.GlassCard
import com.cen.feedback.ui.components.MetricCard
import com.cen.feedback.ui.components.SectionTitle
import com.cen.feedback.ui.components.StatusChip
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.Accent500
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Slate900
import com.cen.feedback.ui.theme.Success500
import com.cen.feedback.ui.theme.Warning500

/**
 * 学生首页：渐变标题区 + 数据指标 + 当前进行中问卷 + 我的课程。
 */
@Composable
fun StudentHomeScreen(
    navController: NavController,
    onJumpTab: (com.cen.feedback.ui.student.StudentTab) -> Unit = {},
    vm: StudentHomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val pendingQ = s.questionnaires.count { it.status == 1 && it.hasSubmitted != true }
    val nick = s.nickname.ifBlank { stringResource(R.string.default_nickname_student) }
    val sub = if (pendingQ > 0) {
        stringResource(R.string.student_home_subtitle_pending, s.courses.size, pendingQ)
    } else {
        stringResource(R.string.student_home_subtitle_none, s.courses.size)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            DashboardHero(
                title = stringResource(R.string.student_home_title, nick),
                subtitle = sub,
                hint = stringResource(R.string.student_home_hint_ai),
                icon = Icons.Rounded.Person,
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
                    icon = Icons.Rounded.LibraryBooks,
                    label = stringResource(R.string.student_home_metric_courses),
                    value = s.courses.size.toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = stringResource(R.string.student_home_metric_pending_q),
                    value = pendingQ.toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle(stringResource(R.string.student_home_section_active_q)) }
        if (s.questionnaires.none { it.status == 1 }) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Quiz,
                    title = stringResource(R.string.student_home_empty_active_q_title),
                    subtitle = stringResource(R.string.student_home_empty_active_q_subtitle),
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppDimens.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
                ) {
                    items(s.questionnaires.filter { it.status == 1 }) { q ->
                        QuestionnaireCard(q) {
                            val cId = q.course?.id ?: return@QuestionnaireCard
                            val qId = q.questionnaire?.id ?: return@QuestionnaireCard
                            navController.navigate(Routes.questionnaireFill(cId, qId))
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.student_home_section_my_courses), trailing = {
                TextButton(onClick = { onJumpTab(com.cen.feedback.ui.student.StudentTab.Courses) }) {
                    Text(stringResource(R.string.action_view_all))
                }
            })
        }
        if (s.courses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryBooks,
                    title = stringResource(R.string.student_home_empty_courses_title),
                    subtitle = stringResource(R.string.student_home_empty_courses_subtitle),
                )
            }
        } else {
            items(s.courses.take(6)) { c ->
                CourseRow(c) {
                    navController.navigate(Routes.courseDetail(c.id))
                }
            }
        }
    }
}

@Composable
fun QuestionnaireCard(q: QuestionnaireFullInfoDTO, onClick: () -> Unit) {
    val inProgress = stringResource(R.string.questionnaire_status_in_progress)
    val submitted = stringResource(R.string.questionnaire_status_submitted)
    val untitled = stringResource(R.string.questionnaire_untitled)
    Surface(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 340.dp)
            .heightIn(min = 140.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = q.statusDescription ?: inProgress,
                    color = if (q.status == 1) Success500 else Slate900.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(8.dp))
                if (q.hasSubmitted == true) {
                    StatusChip(text = submitted, color = Accent500)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                q.questionnaire?.title ?: untitled,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${q.course?.name ?: ""} · ${q.teacher?.nickname ?: q.teacher?.username ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CourseRow(c: Courses, onClick: () -> Unit) {
    val dash = stringResource(R.string.placeholder_dash)
    val sem = c.semester?.let { stringResource(R.string.format_semester, it) }
    val fallbackInitial = stringResource(R.string.course_fallback_initial)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    c.name?.firstOrNull()?.toString() ?: fallbackInitial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name ?: dash, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(c.code, c.academicYear, sem)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

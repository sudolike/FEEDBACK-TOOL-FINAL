package com.cen.feedback.ui.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.R
import com.cen.feedback.data.model.AdminDashboard
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.admin.AdminTab
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUi(
    val loading: Boolean = false,
    val data: AdminDashboard? = null,
    val nickname: String = "",
    val error: String? = null,
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminDashboardUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val nick = repo.tokenStore.username().orEmpty()
            val data = repo.adminDashboard()
            nick to data
        }.onSuccess { (nick, data) ->
            _state.update { it.copy(loading = false, nickname = nick, data = data) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}

@Composable
fun AdminDashboardScreen(
    onJumpTab: (AdminTab) -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val nick = s.nickname.ifBlank { stringResource(R.string.default_nickname_admin) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            DashboardHero(
                title = stringResource(R.string.admin_dashboard_title, nick),
                subtitle = stringResource(
                    R.string.admin_dashboard_subtitle,
                    s.data?.totalUsers ?: 0,
                    s.data?.totalCourses ?: 0,
                ),
                hint = stringResource(R.string.admin_dashboard_hint_security),
                icon = Icons.Rounded.AdminPanelSettings,
                colors = listOf(Primary800, Primary600, Pink500.copy(alpha = 0.6f)),
            )
        }

        item { SectionTitle(stringResource(R.string.admin_section_user_mix)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = AppDimens.pagePadding),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                val d = s.data
                item {
                    StatTile(
                        icon = Icons.Rounded.School,
                        label = stringResource(R.string.admin_label_students),
                        value = (d?.totalStudents ?: 0L).toString(),
                        accent = Primary600,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.Person,
                        label = stringResource(R.string.admin_label_teachers),
                        value = (d?.totalTeachers ?: 0L).toString(),
                        accent = Accent600,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.AdminPanelSettings,
                        label = stringResource(R.string.admin_label_admins),
                        value = (d?.totalAdmins ?: 0L).toString(),
                        accent = Pink500,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.Block,
                        label = stringResource(R.string.admin_label_disabled),
                        value = (d?.disabledUsers ?: 0L).toString(),
                        accent = Danger500,
                    )
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.admin_section_approval), trailing = {
                TextButton(onClick = { onJumpTab(AdminTab.Approval) }) {
                    Text(stringResource(R.string.action_manage))
                }
            })
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.pagePadding),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                MetricCard(
                    icon = Icons.Rounded.HourglassTop,
                    label = stringResource(R.string.admin_metric_pending),
                    value = (s.data?.pendingCourses ?: 0L).toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.CheckCircle,
                    label = stringResource(R.string.admin_metric_approved),
                    value = (s.data?.approvedCourses ?: 0L).toString(),
                    accent = Success500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Cancel,
                    label = stringResource(R.string.admin_metric_rejected),
                    value = (s.data?.rejectedCourses ?: 0L).toString(),
                    accent = Danger500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle(stringResource(R.string.admin_section_feedback)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.pagePadding),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = stringResource(R.string.admin_metric_q_templates),
                    value = (s.data?.totalQuestionnaires ?: 0L).toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Insights,
                    label = stringResource(R.string.admin_metric_q_responses),
                    value = (s.data?.totalResponses ?: 0L).toString(),
                    accent = Accent600,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.pagePadding, vertical = AppDimens.sectionVertical),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Reviews,
                    label = stringResource(R.string.admin_metric_feedbacks),
                    value = (s.data?.totalFeedbacks ?: 0L).toString(),
                    accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Star,
                    label = stringResource(R.string.admin_metric_teacher_ratings),
                    value = (s.data?.totalTeacherRatings ?: 0L).toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle(stringResource(R.string.admin_section_today)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.pagePadding),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.itemSpacing),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Today,
                    label = stringResource(R.string.admin_metric_today_responses),
                    value = (s.data?.todayResponses ?: 0L).toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Forum,
                    label = stringResource(R.string.admin_metric_today_feedbacks),
                    value = (s.data?.todayFeedbacks ?: 0L).toString(),
                    accent = Accent600,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (s.error != null) {
            item { InlineError(s.error) }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.action_retry),
                        onClick = { vm.refresh() },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    Surface(
        modifier = Modifier.widthIn(min = 132.dp, max = 180.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent) }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

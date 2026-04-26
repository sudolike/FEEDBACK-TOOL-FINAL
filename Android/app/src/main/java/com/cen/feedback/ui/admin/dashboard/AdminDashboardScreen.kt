package com.cen.feedback.ui.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
            val nick = repo.tokenStore.username() ?: "Admin"
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { HeaderHero(s.nickname, s.data) }

        item { SectionTitle("用户构成") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val d = s.data
                item {
                    StatTile(
                        icon = Icons.Rounded.School,
                        label = "学生",
                        value = (d?.totalStudents ?: 0L).toString(),
                        accent = Primary600,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.Person,
                        label = "教师",
                        value = (d?.totalTeachers ?: 0L).toString(),
                        accent = Accent600,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.AdminPanelSettings,
                        label = "管理员",
                        value = (d?.totalAdmins ?: 0L).toString(),
                        accent = Pink500,
                    )
                }
                item {
                    StatTile(
                        icon = Icons.Rounded.Block,
                        label = "已停用",
                        value = (d?.disabledUsers ?: 0L).toString(),
                        accent = Danger500,
                    )
                }
            }
        }

        item {
            SectionTitle("课程审批", trailing = {
                TextButton(onClick = { onJumpTab(AdminTab.Approval) }) { Text("管理") }
            })
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.HourglassTop,
                    label = "待审批",
                    value = (s.data?.pendingCourses ?: 0L).toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.CheckCircle,
                    label = "已通过",
                    value = (s.data?.approvedCourses ?: 0L).toString(),
                    accent = Success500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Cancel,
                    label = "已驳回",
                    value = (s.data?.rejectedCourses ?: 0L).toString(),
                    accent = Danger500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle("反馈与问卷") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = "问卷模板",
                    value = (s.data?.totalQuestionnaires ?: 0L).toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Insights,
                    label = "问卷答复",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Reviews,
                    label = "课程评价",
                    value = (s.data?.totalFeedbacks ?: 0L).toString(),
                    accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Star,
                    label = "教师评分",
                    value = (s.data?.totalTeacherRatings ?: 0L).toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle("今日活跃") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Today,
                    label = "今日新增问卷",
                    value = (s.data?.todayResponses ?: 0L).toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Forum,
                    label = "今日新增课评",
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
                    SecondaryButton(text = "重试", onClick = { vm.refresh() })
                }
            }
        }
    }
}

@Composable
private fun HeaderHero(nickname: String, d: AdminDashboard?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Primary800, Primary600, Pink500.copy(alpha = 0.6f))))
    ) {
        Column(modifier = Modifier.statusBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AdminPanelSettings, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "你好，$nickname",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "正在维护 ${d?.totalUsers ?: 0} 位用户 · ${d?.totalCourses ?: 0} 门课程",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "管理员账号不开放注册，请妥善保管账号密码并定期轮换",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
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
        modifier = Modifier.width(140.dp),
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

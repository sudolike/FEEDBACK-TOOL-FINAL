package com.cen.feedback.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.EmptyState
import com.cen.feedback.ui.components.GradientTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 学生端"消息通知"页：
 *   汇总 待填问卷 + 教师邀请 + 课程问答提醒
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    data class Notification(
        val icon: ImageVector,
        val title: String,
        val subtitle: String,
        val timestamp: String? = null,
    )

    data class State(
        val loading: Boolean = false,
        val items: List<Notification> = emptyList(),
        val error: String? = null,
    )

    val state = MutableStateFlow(State())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        state.update { it.copy(loading = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: return@runCatching emptyList<Notification>()
            val list = mutableListOf<Notification>()
            // 邀请
            runCatching { repo.studentMyEnrollments("pending") }.getOrNull()?.forEach { row ->
                val src = row.enrollment?.source
                val courseName = row.course?.name ?: "课程"
                if (src == "teacher_invite") {
                    list += Notification(
                        Icons.Rounded.Campaign,
                        "课程邀请：$courseName",
                        "教师邀请你加入该课程，请到\"课程\"页处理",
                        row.enrollment.createdAt,
                    )
                } else {
                    list += Notification(
                        Icons.Rounded.Inbox,
                        "申请待审批：$courseName",
                        "你提交的申请正在等待教师审批",
                        row.enrollment?.createdAt,
                    )
                }
            }
            // 待填问卷
            runCatching { repo.studentAllQuestionnaires(sid) }.getOrNull()
                ?.filter { it.hasSubmitted == false && it.status == 1 }
                ?.forEach { q ->
                    list += Notification(
                        Icons.Rounded.Quiz,
                        "问卷待填：${q.questionnaire?.title ?: "未命名问卷"}",
                        "${q.course?.name ?: "课程"} · ${q.statusDescription ?: "进行中"}",
                        q.createdAt,
                    )
                }
            list
        }
            .onSuccess { state.update { s -> s.copy(loading = false, items = it) } }
            .onFailure { e -> state.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: NotificationsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        GradientTopBar(title = "消息通知", onBack = onBack)
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            s.error != null -> EmptyState(
                icon = Icons.Rounded.Inbox,
                title = "加载失败",
                subtitle = s.error,
            )
            s.items.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Inbox,
                title = "暂无新消息",
                subtitle = "当有新的问卷推送或课程邀请时，会在这里出现。",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(s.items) { n ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(n.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    n.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

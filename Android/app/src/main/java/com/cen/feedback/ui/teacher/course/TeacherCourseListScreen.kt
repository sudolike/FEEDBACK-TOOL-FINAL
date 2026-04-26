package com.cen.feedback.ui.teacher.course

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val LIST_FILTERS = listOf(
    null      to "全部",
    "approved" to "已通过",
    "pending"  to "待审批",
    "rejected" to "已驳回",
)

data class TeacherCourseListUi(
    val loading: Boolean = false,
    val all: List<Courses> = emptyList(),
    val filter: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TeacherCourseListViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherCourseListUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun setFilter(f: String?) = _state.update { it.copy(filter = f) }
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            repo.teacherAllCourses(tid)
        }.onSuccess { l -> _state.update { it.copy(loading = false, all = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun TeacherCourseListScreen(
    navController: NavController,
    vm: TeacherCourseListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val visible = remember(s.all, s.filter) {
        if (s.filter == null) s.all
        else s.all.filter { (it.status ?: "approved") == s.filter }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.teacherCoursePropose(null)) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("申请新课程") },
                containerColor = Primary600,
                contentColor = Color.White,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GradientTopBar(
                title = "我的课程",
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Rounded.Refresh, null, tint = Color.White)
                    }
                },
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LIST_FILTERS) { (value, label) ->
                    val total = if (value == null) s.all.size
                                else s.all.count { (it.status ?: "approved") == value }
                    FilterChip(
                        selected = s.filter == value,
                        onClick = { vm.setFilter(value) },
                        label = { Text("$label ($total)") },
                    )
                }
            }

            InlineError(s.error)
            when {
                s.loading && visible.isEmpty() ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
                visible.isEmpty() ->
                    EmptyState(
                        title = if (s.all.isEmpty()) "还没有课程" else "该分类下暂无课程",
                        subtitle = if (s.all.isEmpty())
                            "点击右下角「申请新课程」提交一份课程信息，等待管理员审批" else null,
                        icon = Icons.Rounded.Class,
                        modifier = Modifier.padding(top = 48.dp),
                    )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                ) {
                    items(visible) { c ->
                        TeacherCourseRow(
                            c = c,
                            onClick = {
                                when (c.status) {
                                    "approved" ->
                                        navController.navigate(Routes.teacherCourseDetail(c.id))
                                    "rejected" ->
                                        navController.navigate(Routes.teacherCoursePropose(c.id))
                                    else ->
                                        navController.navigate(Routes.teacherCoursePropose(c.id))
                                }
                            },
                            onResubmit = if (c.status == "rejected")
                                ({ navController.navigate(Routes.teacherCoursePropose(c.id)) }) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherCourseRow(
    c: Courses,
    onClick: () -> Unit,
    onResubmit: (() -> Unit)?,
) {
    val (statusLabel, statusColor) = when (c.status) {
        "pending"  -> "待审批" to Warning500
        "rejected" -> "已驳回" to Danger500
        "approved", null -> "已通过" to Success500
        else -> (c.status ?: "—") to Slate500
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (c.name ?: "课").take(1),
                        color = Color.White, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name ?: "—", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        StatusChip(statusLabel, color = statusColor)
                    }
                    Text(
                        listOfNotNull(
                            c.code, c.academicYear,
                            c.semester?.let { "第 $it 学期" }, c.location
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (c.status == "rejected") {
                if (!c.rejectReason.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Danger500.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            "驳回理由：${c.rejectReason}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger500,
                        )
                    }
                }
                if (onResubmit != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onResubmit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("重新提交")
                    }
                }
            }
            if (c.status == "pending") {
                Spacer(Modifier.height(6.dp))
                Text(
                    "提交后等待管理员审批，通过后学生才会看到。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                )
            }
        }
    }
}

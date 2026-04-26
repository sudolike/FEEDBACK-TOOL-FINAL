package com.cen.feedback.ui.admin.courses

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
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val ALL_FILTERS = listOf(
    null     to "全部",
    "pending"  to "待审批",
    "approved" to "已通过",
    "rejected" to "已驳回",
)

data class AdminCourseListUi(
    val loading: Boolean = false,
    val filter: String? = null,
    val list: List<Courses> = emptyList(),
    val error: String? = null,
    val deleting: Long? = null,
)

@HiltViewModel
class AdminCourseListViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminCourseListUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun setFilter(f: String?) {
        _state.update { it.copy(filter = f) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repo.adminAllCourses(_state.value.filter) }
            .onSuccess { l -> _state.update { it.copy(loading = false, list = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun delete(id: Long) = viewModelScope.launch {
        _state.update { it.copy(deleting = id) }
        runCatching { repo.adminDeleteCourse(id) }
            .onSuccess { _state.update { it.copy(deleting = null) }; refresh() }
            .onFailure { e -> _state.update { it.copy(deleting = null, error = e.message) } }
    }
}

@Composable
fun AdminCourseListScreen(
    vm: AdminCourseListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Courses?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(title = "全平台课程",
            actions = {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Rounded.Refresh, "刷新", tint = Color.White)
                }
            })

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ALL_FILTERS) { (value, label) ->
                FilterChip(
                    selected = s.filter == value,
                    onClick = { vm.setFilter(value) },
                    label = { Text(label) },
                )
            }
        }

        InlineError(s.error)
        when {
            s.loading && s.list.isEmpty() ->
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
            s.list.isEmpty() ->
                EmptyState(
                    title = "暂无课程",
                    icon = Icons.Rounded.Class,
                    modifier = Modifier.padding(top = 64.dp),
                )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            ) {
                items(s.list) { c ->
                    CourseAdminRow(
                        c = c,
                        deleting = s.deleting == c.id,
                        onDelete = { deleting = c },
                    )
                }
            }
        }
    }

    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除课程") },
            text = {
                Text("确认删除「${deleting?.name ?: ""}」吗？此操作不可恢复，关联的反馈/问卷记录会一并保留为孤立数据。")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting?.id?.let(vm::delete); deleting = null
                }) { Text("删除", color = Danger500) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CourseAdminRow(
    c: Courses,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    val (statusLabel, statusColor) = when (c.status) {
        "pending"  -> "待审批" to Warning500
        "rejected" -> "已驳回" to Danger500
        "approved" -> "已通过" to Success500
        else        -> (c.status ?: "—") to Slate500
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Primary600, Accent600))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (c.name ?: "课").take(1),
                        color = Color.White, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name ?: "—", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        StatusChip(statusLabel, color = statusColor)
                    }
                    Text(
                        listOfNotNull(c.code, c.academicYear, c.location).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                    )
                }
                IconButton(onClick = onDelete, enabled = !deleting) {
                    if (deleting) {
                        CircularProgressIndicator(
                            color = Danger500, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else Icon(Icons.Rounded.Delete, "删除", tint = Danger500)
                }
            }
            if (c.status == "rejected" && !c.rejectReason.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
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
        }
    }
}

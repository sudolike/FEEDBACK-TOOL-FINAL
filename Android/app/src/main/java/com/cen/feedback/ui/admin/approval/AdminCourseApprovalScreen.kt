package com.cen.feedback.ui.admin.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cen.feedback.data.model.PendingCourseRow
import com.cen.feedback.data.model.User
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminCourseApprovalUi(
    val loading: Boolean = false,
    val rows: List<PendingCourseRow> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val acting: Long? = null,
)

@HiltViewModel
class AdminCourseApprovalViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminCourseApprovalUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repo.adminPendingCourses() }
            .onSuccess { rows -> _state.update { it.copy(loading = false, rows = rows) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun approve(id: Long) = viewModelScope.launch {
        _state.update { it.copy(acting = id) }
        runCatching { repo.adminApproveCourse(id) }
            .onSuccess { _state.update { it.copy(acting = null, message = "已通过") }; refresh() }
            .onFailure { e -> _state.update { it.copy(acting = null, error = e.message) } }
    }

    fun reject(id: Long, reason: String) = viewModelScope.launch {
        _state.update { it.copy(acting = id) }
        runCatching { repo.adminRejectCourse(id, reason) }
            .onSuccess { _state.update { it.copy(acting = null, message = "已驳回") }; refresh() }
            .onFailure { e -> _state.update { it.copy(acting = null, error = e.message) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null, error = null) }
}

@Composable
fun AdminCourseApprovalScreen(
    vm: AdminCourseApprovalViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var rejectingFor by remember { mutableStateOf<PendingCourseRow?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "课程审批 (${s.rows.size})",
            actions = {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Rounded.Refresh, "刷新", tint = Color.White)
                }
            }
        )

        if (s.message != null || s.error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (s.error != null) MaterialTheme.colorScheme.errorContainer
                        else Success500.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (s.error != null) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                        null,
                        tint = if (s.error != null) MaterialTheme.colorScheme.onErrorContainer else Success500,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.error ?: s.message ?: "",
                        modifier = Modifier.weight(1f),
                        color = if (s.error != null) MaterialTheme.colorScheme.onErrorContainer else Slate800,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = vm::consumeMessage) { Text("好") }
                }
            }
        }

        when {
            s.loading && s.rows.isEmpty() ->
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
            s.rows.isEmpty() ->
                EmptyState(
                    title = "暂无待审批课程",
                    subtitle = "教师端提交后会出现在这里",
                    icon = Icons.Rounded.Inbox,
                    modifier = Modifier.padding(top = 64.dp),
                )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(s.rows) { row ->
                    PendingRow(
                        row = row,
                        loading = s.acting == row.course?.id,
                        onApprove = { row.course?.id?.let(vm::approve) },
                        onReject = { rejectingFor = row },
                    )
                }
            }
        }
    }

    if (rejectingFor != null) {
        RejectDialog(
            target = rejectingFor!!,
            onDismiss = { rejectingFor = null },
            onConfirm = { reason ->
                rejectingFor!!.course?.id?.let { vm.reject(it, reason) }
                rejectingFor = null
            },
        )
    }
}

@Composable
private fun PendingRow(
    row: PendingCourseRow,
    loading: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val c: Courses = row.course ?: return
    val t: User? = row.teacher
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Warning500, Pink500))),
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
                        Text(c.name ?: "未命名", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        StatusChip("待审批", color = Warning500)
                    }
                    Text(
                        listOfNotNull(c.code, c.academicYear, c.semester?.let { "学期 $it" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!c.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(c.description, style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Person, null, tint = Slate500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "申请人：${t?.nickname ?: t?.username ?: "教师#${c.teacherId}"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                )
            }
            if (!c.courseTime.isNullOrBlank() || !c.location.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = Slate500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        listOfNotNull(c.courseTime, c.location).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("驳回")
                }
                Button(
                    onClick = onApprove,
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White, strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("通过")
                    }
                }
            }
        }
    }
}

@Composable
private fun RejectDialog(
    target: PendingCourseRow,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("驳回课程：${target.course?.name ?: ""}") },
        text = {
            Column {
                Text(
                    "请填写驳回理由（教师端会看到）",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("驳回理由") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.ifBlank { "课程信息不规范" }) },
            ) { Text("确认驳回") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

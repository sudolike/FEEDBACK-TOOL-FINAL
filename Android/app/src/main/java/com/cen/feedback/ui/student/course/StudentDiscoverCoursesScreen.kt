package com.cen.feedback.ui.student.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.School
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
import com.cen.feedback.ui.components.EmptyState
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.InlineError
import com.cen.feedback.ui.components.PrimaryButton
import com.cen.feedback.ui.components.StatusChip
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUi(
    val loading: Boolean = false,
    val keyword: String = "",
    val courses: List<Courses> = emptyList(),
    /** 已申请的课程 ID（pending） */
    val pendingIds: Set<Long> = emptySet(),
    /** 已加入（approved）的课程 ID */
    val approvedIds: Set<Long> = emptySet(),
    val opMsg: String? = null,
    val error: String? = null,
)

@HiltViewModel
class StudentDiscoverViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUi())
    val state = _state.asStateFlow()

    init { search("") }

    fun onKeyword(s: String) = _state.update { it.copy(keyword = s) }

    fun search(keyword: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, keyword = keyword) }
        runCatching {
            val page = repo.discoverCourses(1, 50, keyword)
            val mine = runCatching { repo.studentMyEnrollments(null) }.getOrDefault(emptyList())
            val pending = mine
                .filter { it.enrollment?.status == "pending" }
                .mapNotNull { it.enrollment?.courseId }
                .toSet()
            val approved = mine
                .filter { it.enrollment?.status == "approved" }
                .mapNotNull { it.enrollment?.courseId }
                .toSet()
            Triple(page.records ?: emptyList(), pending, approved)
        }
            .onSuccess { (list, pending, approved) ->
                _state.update {
                    it.copy(
                        loading = false,
                        courses = list,
                        pendingIds = pending,
                        approvedIds = approved,
                    )
                }
            }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun apply(courseId: Long, message: String?) = viewModelScope.launch {
        runCatching { repo.studentApplyEnroll(courseId, message?.ifBlank { null }) }
            .onSuccess {
                _state.update { it.copy(opMsg = "申请已提交，等待教师审批") }
                search(_state.value.keyword)
            }
            .onFailure { e -> _state.update { it.copy(opMsg = e.message ?: "申请失败") } }
    }

    fun consumeMsg() = _state.update { it.copy(opMsg = null) }
}

@Composable
fun StudentDiscoverCoursesScreen(
    navController: NavController,
    vm: StudentDiscoverViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var applyTarget by remember { mutableStateOf<Courses?>(null) }

    LaunchedEffect(s.opMsg) {
        s.opMsg?.let {
            snackbar.showSnackbar(it)
            vm.consumeMsg()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GradientTopBar(
                title = "发现课程",
                onBack = { navController.popBackStack() },
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = s.keyword,
                        onValueChange = vm::onKeyword,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索课程名 / 课程编号") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    )
                    TextButton(onClick = { vm.search(s.keyword) }) { Text("搜索") }
                }
            }

            InlineError(s.error)

            if (s.courses.isEmpty() && !s.loading) {
                EmptyState(
                    icon = Icons.Rounded.School,
                    title = "暂无可申请的课程",
                    subtitle = "换个关键词，或者稍后再试。",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(s.courses) { c ->
                        DiscoverCourseRow(
                            course = c,
                            isApproved = c.id in s.approvedIds,
                            isPending = c.id in s.pendingIds,
                            onApply = { applyTarget = c },
                        )
                    }
                }
            }
        }
    }

    applyTarget?.let { course ->
        ApplyDialog(
            course = course,
            onDismiss = { applyTarget = null },
            onConfirm = { msg ->
                vm.apply(course.id, msg)
                applyTarget = null
            },
        )
    }
}

@Composable
private fun DiscoverCourseRow(
    course: Courses,
    isApproved: Boolean,
    isPending: Boolean,
    onApply: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
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
                    course.name?.firstOrNull()?.toString() ?: "课",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    course.name ?: "未命名",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                val sub = listOfNotNull(course.code, course.location)
                    .joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                isApproved -> StatusChip("已加入")
                isPending -> StatusChip("审批中")
                else -> FilledTonalButton(onClick = onApply) { Text("申请加入") }
            }
        }
    }
}

@Composable
private fun ApplyDialog(
    course: Courses,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var msg by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("申请加入：${course.name ?: ""}") },
        text = {
            Column {
                Text("可向教师附上一条留言（选填）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = msg,
                    onValueChange = { if (it.length <= 200) msg = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("例如：希望选修这门课") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(msg) }) { Text("提交申请") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

package com.cen.feedback.ui.teacher.assignment

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.Assignment
import com.cen.feedback.data.model.AssignmentSubmission
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class AssignmentEditorUi(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val totalScore: String = "100",
    val status: Int = 1,
    val attachUrl: String? = null,
    val attachName: String? = null,
    val submissions: List<AssignmentSubmission> = emptyList(),
    val sending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AssignmentEditorViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AssignmentEditorUi())
    val state = _state.asStateFlow()
    private var courseIdInternal = 0L
    private var editingId: Long? = null

    fun load(courseId: Long, id: Long?) {
        courseIdInternal = courseId
        editingId = id
        if (id == null) {
            _state.value = AssignmentEditorUi()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val list = repo.assignmentsByCourse(courseId)
                val a = list.firstOrNull { it.id == id }
                val subs = runCatching { repo.submissionsOf(id) }.getOrDefault(emptyList())
                a to subs
            }.onSuccess { (a, subs) ->
                if (a == null) _state.update { it.copy(loading = false, error = "作业不存在") }
                else _state.update {
                    it.copy(
                        loading = false,
                        title = a.title.orEmpty(),
                        description = a.description.orEmpty(),
                        deadline = a.deadline.orEmpty(),
                        totalScore = (a.totalScore ?: 100).toString(),
                        status = a.status ?: 1,
                        attachUrl = a.attachmentUrl,
                        attachName = a.attachmentName,
                        submissions = subs,
                    )
                }
            }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setDeadline(v: String) = _state.update { it.copy(deadline = v) }
    fun setScore(v: String) = _state.update { it.copy(totalScore = v.filter(Char::isDigit)) }
    fun setStatus(v: Int) = _state.update { it.copy(status = v) }

    fun upload(file: File) = viewModelScope.launch {
        _state.update { it.copy(sending = true) }
        runCatching { repo.uploadFile(file) }
            .onSuccess { url ->
                _state.update { it.copy(sending = false, attachUrl = url, attachName = file.name) }
            }.onFailure { e -> _state.update { it.copy(sending = false, error = e.message) } }
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "请填写作业标题") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            repo.saveAssignment(
                Assignment(
                    id = editingId,
                    courseId = courseIdInternal,
                    teacherId = tid,
                    title = s.title,
                    description = s.description,
                    attachmentUrl = s.attachUrl,
                    attachmentName = s.attachName,
                    deadline = s.deadline,
                    totalScore = s.totalScore.toIntOrNull() ?: 100,
                    status = s.status,
                )
            )
        }.onSuccess { _state.update { it.copy(sending = false, saved = true) } }
         .onFailure { e -> _state.update { it.copy(sending = false, error = e.message ?: "保存失败") } }
    }

    fun grade(sub: AssignmentSubmission, score: Int, comment: String) = viewModelScope.launch {
        runCatching {
            repo.gradeSubmission(sub.copy(score = score, comment = comment))
        }.onSuccess { editingId?.let { load(courseIdInternal, it) } }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }
}

@Composable
fun AssignmentEditorScreen(
    courseId: Long,
    editingId: Long?,
    onBack: () -> Unit,
    vm: AssignmentEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId, editingId) { vm.load(courseId, editingId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(s.saved) { if (s.saved) onBack() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = queryDisplayName(ctx, uri) ?: "attach-${System.currentTimeMillis()}"
            val tmp = File(ctx.cacheDir, name)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            vm.upload(tmp)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {
            GradientTopBar(
                title = if (editingId == null) "新建作业" else "编辑作业",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = vm::save,
                        colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.White),
                    ) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("保存")
                    }
                }
            )
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = s.title, onValueChange = vm::setTitle,
                    label = { Text("作业标题") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = s.description, onValueChange = vm::setDescription,
                    label = { Text("作业要求 / 描述") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = s.deadline, onValueChange = vm::setDeadline,
                    label = { Text("截止时间（YYYY-MM-DD HH:mm）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = s.totalScore, onValueChange = vm::setScore,
                        label = { Text("总分") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("状态", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SegmentedButtons(s.status, vm::setStatus)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SecondaryButton("上传附件",
                        icon = Icons.Rounded.UploadFile,
                        onClick = { launcher.launch("*/*") })
                    Spacer(Modifier.width(8.dp))
                    if (!s.attachName.isNullOrBlank()) {
                        Text(s.attachName!!, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                InlineError(s.error)

                if (editingId != null) {
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("学生提交（${s.submissions.size}）")
                    if (s.submissions.isEmpty()) {
                        EmptyState(title = "暂无提交")
                    } else {
                        s.submissions.forEach { sub ->
                            SubmissionCard(sub, onGrade = { score, comment ->
                                vm.grade(sub, score, comment)
                            })
                        }
                    }
                }
            }
        }
        LoadingOverlay(visible = s.sending)
    }
}

@Composable
private fun SegmentedButtons(value: Int, onChange: (Int) -> Unit) {
    val options = listOf(0 to "未发布", 1 to "进行中", 2 to "已结束")
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)) {
        options.forEachIndexed { i, (v, label) ->
            val selected = value == v
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                shape = when (i) {
                    0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    options.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                color = if (selected) Primary600 else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onChange(v) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label,
                        color = if (selected) androidx.compose.ui.graphics.Color.White
                                else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    sub: AssignmentSubmission,
    onGrade: (Int, String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var score by remember(sub.id) { mutableStateOf((sub.score ?: 0).toString()) }
    var comment by remember(sub.id) { mutableStateOf(sub.comment.orEmpty()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = { open = !open },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("学生 #${sub.studentId}", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (sub.score != null) StatusChip("${sub.score} 分", color = Success500)
                else StatusChip("待批阅", color = Warning500)
            }
            Spacer(Modifier.height(4.dp))
            Text(sub.content?.take(120).orEmpty(),
                style = MaterialTheme.typography.bodyMedium)
            if (open) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it.filter(Char::isDigit) },
                        label = { Text("分数") },
                        modifier = Modifier.width(120.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("评语") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    "提交批阅",
                    icon = Icons.Rounded.Done,
                    onClick = { onGrade(score.toIntOrNull() ?: 0, comment) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun queryDisplayName(ctx: Context, uri: Uri): String? {
    val p = ctx.contentResolver.query(uri, null, null, null, null) ?: return null
    return p.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
    }
}

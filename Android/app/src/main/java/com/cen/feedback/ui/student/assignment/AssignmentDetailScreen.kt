package com.cen.feedback.ui.student.assignment

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

data class AssignmentDetailUi(
    val loading: Boolean = false,
    val assignment: Assignment? = null,
    val mySubmission: AssignmentSubmission? = null,
    val content: String = "",
    val attachUrl: String? = null,
    val attachName: String? = null,
    val sending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AssignmentDetailViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AssignmentDetailUi())
    val state = _state.asStateFlow()
    private var assignIdInternal = 0L

    fun load(id: Long) {
        assignIdInternal = id
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            val all = repo.assignmentsByStudent(sid)
            val a = all.firstOrNull { it.id == assignIdInternal }
            val mine = runCatching { repo.mySubmission(assignIdInternal, sid) }.getOrNull()
            a to mine
        }.onSuccess { (a, mine) ->
            _state.update {
                it.copy(
                    loading = false,
                    assignment = a,
                    mySubmission = mine,
                    content = mine?.content.orEmpty(),
                    attachUrl = mine?.attachmentUrl,
                    attachName = mine?.attachmentName,
                )
            }
        }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun setContent(v: String) = _state.update { it.copy(content = v) }

    fun upload(ctx: Context, file: File) = viewModelScope.launch {
        _state.update { it.copy(sending = true, error = null) }
        runCatching { repo.uploadFile(file) }
            .onSuccess { url ->
                _state.update {
                    it.copy(sending = false, attachUrl = url, attachName = file.name,
                        message = "附件已上传")
                }
            }.onFailure { e ->
                _state.update { it.copy(sending = false, error = e.message ?: "上传失败") }
            }
    }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        if (s.content.isBlank() && s.attachUrl.isNullOrBlank()) {
            _state.update { it.copy(error = "请填写内容或上传附件") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            repo.submitAssignment(
                AssignmentSubmission(
                    assignmentId = assignIdInternal,
                    studentId = sid,
                    content = s.content,
                    attachmentUrl = s.attachUrl,
                    attachmentName = s.attachName,
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, message = "已提交") }
            refresh()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "提交失败") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun AssignmentDetailScreen(
    assignmentId: Long,
    onBack: () -> Unit,
    vm: AssignmentDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(assignmentId) { vm.load(assignmentId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = queryDisplayName(ctx, uri) ?: "submission-${System.currentTimeMillis()}"
            val tmp = File(ctx.cacheDir, name)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            vm.upload(ctx, tmp)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {
            GradientTopBar(title = "作业详情", onBack = onBack)
            val a = s.assignment
            if (a == null && s.loading) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (a == null) {
                EmptyState(title = "未找到该作业", subtitle = "可能已被删除")
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(a.title ?: "未命名作业",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold)
                                StatusChip(
                                    when (a.status) {
                                        0 -> "未发布"; 1 -> "进行中"; 2 -> "已结束"
                                        else -> "—"
                                    },
                                    color = when (a.status) {
                                        1 -> Success500; 2 -> Slate600; else -> Warning500
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("截止：${a.deadline ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium)
                            if (a.totalScore != null) {
                                Text("总分：${a.totalScore}",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(a.description.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    val mine = s.mySubmission
                    val graded = mine?.score != null
                    Text(
                        if (graded) "本次作业已批阅" else if (mine != null) "已提交，等待批阅" else "我的提交",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (graded) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Success500.copy(alpha = 0.1f),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("得分：${mine?.score}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Success500)
                                if (!mine?.comment.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("评语：${mine?.comment}",
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = s.content,
                            onValueChange = vm::setContent,
                            placeholder = { Text("写下你的解答 / 思路…") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SecondaryButton(
                                "上传附件",
                                icon = Icons.Rounded.UploadFile,
                                onClick = { launcher.launch("*/*") },
                            )
                            Spacer(Modifier.width(8.dp))
                            if (!s.attachName.isNullOrBlank()) {
                                Text(
                                    s.attachName!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        InlineError(s.error)
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(
                            if (mine == null) "提交作业" else "重新提交",
                            icon = Icons.Rounded.Send,
                            onClick = vm::submit,
                            loading = s.sending,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        LoadingOverlay(visible = s.sending)
    }

    if (s.message != null) {
        LaunchedEffect(s.message) {
            android.widget.Toast.makeText(ctx, s.message, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
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

package com.cen.feedback.ui.student.qa

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.QaPost
import com.cen.feedback.data.model.QaPostDetail
import com.cen.feedback.data.model.QaReply
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostDetailUi(
    val loading: Boolean = false,
    val detail: QaPostDetail? = null,
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PostDetailUi())
    val state = _state.asStateFlow()
    private var postIdInternal = 0L

    fun load(id: Long) {
        postIdInternal = id
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { repo.postDetail(postIdInternal) }
            .onSuccess { d -> _state.update { it.copy(loading = false, detail = d) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun setInput(v: String) = _state.update { it.copy(input = v) }

    fun reply() = viewModelScope.launch {
        val text = _state.value.input.trim()
        if (text.isBlank()) {
            _state.update { it.copy(error = "请输入回答内容") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            val role = repo.tokenStore.role() ?: "student"
            repo.createReply(
                QaReply(
                    postId = postIdInternal,
                    authorId = sid,
                    authorRole = role,
                    content = text,
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, input = "") }
            refresh()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "回复失败") }
        }
    }

    fun accept(replyId: Long) = viewModelScope.launch {
        runCatching { repo.acceptReply(replyId) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }
}

@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    vm: PostDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(postId) { vm.load(postId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(title = "问题详情", onBack = onBack)
            if (s.loading && s.detail == null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    item { PostHeader(s.detail?.post) }
                    item { SectionTitle("全部回答（${s.detail?.replies?.size ?: 0}）") }
                    if (s.detail?.replies.isNullOrEmpty()) {
                        item { EmptyState(title = "暂无回答", subtitle = "成为第一个回答的人吧") }
                    } else {
                        items(s.detail!!.replies!!) { r ->
                            ReplyCard(r) { rid ->
                                vm.accept(rid)
                            }
                        }
                    }
                }
                ReplyBar(
                    value = s.input,
                    sending = s.sending,
                    onValueChange = vm::setInput,
                    onSend = vm::reply,
                )
                InlineError(s.error)
            }
        }
    }
}

@Composable
private fun PostHeader(p: QaPost?) {
    if (p == null) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    if (p.authorRole == "teacher") "教师" else "学生",
                    color = if (p.authorRole == "teacher") Accent500 else Primary600,
                )
                Spacer(Modifier.width(6.dp))
                if (p.isResolved == 1) StatusChip("已解决", color = Success500)
            }
            Spacer(Modifier.height(8.dp))
            Text(p.title ?: "—", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(p.content.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(p.createdAt.orEmpty(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReplyCard(r: QaReply, onAccept: (Long) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    if (r.authorRole == "teacher") "教师" else "同学",
                    color = if (r.authorRole == "teacher") Accent500 else Primary600,
                )
                Spacer(Modifier.width(8.dp))
                if (r.isAccepted == 1) StatusChip("最佳答案", color = Success500)
                Spacer(Modifier.weight(1f))
                if (r.isAccepted != 1) {
                    TextButton(onClick = { r.id?.let(onAccept) }) {
                        Icon(Icons.Rounded.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("采纳")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(r.content.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(r.createdAt.orEmpty(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReplyBar(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("写下你的回答…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = !sending && value.isNotBlank()) {
                Icon(Icons.Rounded.Send, null)
            }
        }
    }
}

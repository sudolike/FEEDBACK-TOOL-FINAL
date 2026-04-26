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
import androidx.navigation.NavController
import com.cen.feedback.data.model.QaPost
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

data class CourseQaUi(
    val loading: Boolean = false,
    val posts: List<QaPost> = emptyList(),
    val showCompose: Boolean = false,
    val title: String = "",
    val content: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CourseQaViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseQaUi())
    val state = _state.asStateFlow()
    private var courseIdInternal = 0L

    fun load(cid: Long) {
        courseIdInternal = cid
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { repo.listPosts(courseIdInternal) }
            .onSuccess { l -> _state.update { it.copy(loading = false, posts = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun toggleCompose(open: Boolean) = _state.update {
        it.copy(showCompose = open, title = if (open) it.title else "",
            content = if (open) it.content else "")
    }
    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setContent(v: String) = _state.update { it.copy(content = v) }

    fun publish() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank() || s.content.isBlank()) {
            _state.update { it.copy(error = "请输入标题与内容") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            val role = repo.tokenStore.role() ?: "student"
            repo.createPost(
                QaPost(
                    courseId = courseIdInternal,
                    authorId = sid,
                    authorRole = role,
                    title = s.title.trim(),
                    content = s.content.trim(),
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, showCompose = false, title = "", content = "") }
            refresh()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "发布失败") }
        }
    }
}

@Composable
fun CourseQaScreen(
    courseId: Long,
    navController: NavController,
    vm: CourseQaViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId) { vm.load(courseId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(
                title = "课程问答",
                onBack = { navController.popBackStack() },
                actions = {
                    TextButton(
                        onClick = { vm.toggleCompose(true) },
                        colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.White),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("提问")
                    }
                }
            )
            if (s.loading && s.posts.isEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
            } else if (s.posts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.QuestionAnswer,
                    title = "还没有提问",
                    subtitle = "成为第一个提问的同学吧～",
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                ) {
                    items(s.posts) { p ->
                        PostRow(p) {
                            navController.navigate(Routes.postDetail(p.id ?: 0L))
                        }
                    }
                }
            }
        }

        if (s.showCompose) {
            ModalBottomSheet(
                onDismissRequest = { vm.toggleCompose(false) },
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("提出问题", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = s.title,
                        onValueChange = vm::setTitle,
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = s.content,
                        onValueChange = vm::setContent,
                        label = { Text("详细描述（含上下文、错误信息会更易得到答复）") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    InlineError(s.error)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        "发布",
                        icon = Icons.Rounded.Send,
                        onClick = vm::publish,
                        loading = s.sending,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        LoadingOverlay(visible = s.sending)
    }
}

@Composable
private fun PostRow(p: QaPost, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.title ?: "—", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (p.isResolved == 1) StatusChip("已解决", color = Success500)
                else StatusChip("待解答", color = Warning500)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                p.content?.take(80).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${p.viewCount ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.Comment, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${p.replyCount ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    p.createdAt.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

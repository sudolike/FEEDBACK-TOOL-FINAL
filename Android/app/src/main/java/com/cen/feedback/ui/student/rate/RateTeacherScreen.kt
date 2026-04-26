package com.cen.feedback.ui.student.rate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RateReview
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
import com.cen.feedback.data.model.TeacherRating
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RateTeacherUi(
    val loading: Boolean = false,
    val sending: Boolean = false,
    val rating: Int = 0,
    val teaching: Int = 0,
    val attitude: Int = 0,
    val content: Int = 0,
    val comment: String = "",
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class RateTeacherViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RateTeacherUi())
    val state = _state.asStateFlow()
    private var courseId = 0L
    private var teacherId = 0L

    fun load(c: Long, t: Long) {
        courseId = c; teacherId = t
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val sid = repo.tokenStore.userId() ?: 0L
                repo.myTeacherRating(sid, c, t)
            }.onSuccess { existing ->
                _state.update {
                    it.copy(
                        loading = false,
                        rating = existing?.rating ?: 0,
                        teaching = existing?.teachingScore ?: 0,
                        attitude = existing?.attitudeScore ?: 0,
                        content = existing?.contentScore ?: 0,
                        comment = existing?.comment ?: "",
                    )
                }
            }.onFailure { _state.update { it.copy(loading = false) } }
        }
    }

    fun setRating(v: Int) = _state.update { it.copy(rating = v) }
    fun setTeaching(v: Int) = _state.update { it.copy(teaching = v) }
    fun setAttitude(v: Int) = _state.update { it.copy(attitude = v) }
    fun setContent(v: Int) = _state.update { it.copy(content = v) }
    fun setComment(v: String) = _state.update { it.copy(comment = v) }

    fun submit(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        if (s.rating == 0) {
            _state.update { it.copy(error = "请先给出整体评分") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            repo.saveTeacherRating(
                TeacherRating(
                    courseId = courseId,
                    teacherId = teacherId,
                    studentId = sid,
                    rating = s.rating,
                    teachingScore = s.teaching,
                    attitudeScore = s.attitude,
                    contentScore = s.content,
                    comment = s.comment.trim(),
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, message = "感谢你的匿名评价") }
            onDone()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "提交失败") }
        }
    }
}

@Composable
fun RateTeacherScreen(
    courseId: Long,
    teacherId: Long,
    onBack: () -> Unit,
    vm: RateTeacherViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId, teacherId) { vm.load(courseId, teacherId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(title = "评价老师", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("整体评分", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    StarRatingBar(rating = s.rating, onChange = vm::setRating)
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("教学水平", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    StarRatingBar(rating = s.teaching, onChange = vm::setTeaching)
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("教学态度", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    StarRatingBar(rating = s.attitude, onChange = vm::setAttitude)
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("课程内容", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    StarRatingBar(rating = s.content, onChange = vm::setContent)
                }
                OutlinedTextField(
                    value = s.comment,
                    onValueChange = vm::setComment,
                    placeholder = { Text("说说你对老师授课的真实感受…") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                InlineError(s.error)
                PrimaryButton(
                    "匿名提交评价",
                    icon = Icons.Rounded.RateReview,
                    onClick = { vm.submit(onBack) },
                    loading = s.sending,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("评价对老师匿名展示，仅以匿名编号呈现。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LoadingOverlay(visible = s.sending)
    }
}

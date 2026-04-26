package com.cen.feedback.ui.student.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.ChatRequestDTO
import com.cen.feedback.data.model.ChatTurn
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.AiMsg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AiUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val messages: List<AiMsg> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
)

/**
 * AI 助手会话 VM。可被任意页面注入，session id 在内存维持，账号切换/进程重启会重置。
 */
@HiltViewModel
class AiViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiUiState())
    val state = _state.asStateFlow()

    fun send(text: String, courseId: Long? = null, scene: String? = null) {
        if (text.isBlank()) return
        val now = _state.value
        val withUser = now.messages + AiMsg("user", text)
        _state.update { it.copy(messages = withUser, sending = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val role = repo.tokenStore.role()
                val userId = repo.tokenStore.userId()
                repo.chat(
                    ChatRequestDTO(
                        userId = userId,
                        role = role,
                        sessionId = now.sessionId,
                        message = text,
                        courseId = courseId,
                        scene = scene,
                        history = now.messages.takeLast(10).map { ChatTurn(it.role, it.text) },
                    )
                )
            }.onSuccess { resp ->
                val reply = resp.reply ?: "（助手未返回内容）"
                _state.update {
                    it.copy(
                        messages = it.messages + AiMsg("assistant", reply),
                        sending = false,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        sending = false,
                        error = e.message,
                        messages = it.messages + AiMsg(
                            "assistant",
                            "网络似乎不太稳定～请稍后再试。\n${e.message ?: ""}",
                        ),
                    )
                }
            }
        }
    }
}

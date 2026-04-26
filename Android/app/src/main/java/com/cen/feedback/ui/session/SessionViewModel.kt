package com.cen.feedback.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.local.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionState(
    val token: String? = null,
    val userId: Long? = null,
    val nickname: String? = null,
    val role: String? = null,
    val avatar: String? = null,
)

/**
 * 全局会话状态：包含 token、用户 ID、角色、昵称等。
 * 任何画面都可通过 hiltViewModel<SessionViewModel>() 获取。
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionState())
    val uiState: StateFlow<SessionState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                tokenStore.tokenFlow,
                tokenStore.userIdFlow,
                tokenStore.roleFlow,
                tokenStore.nicknameFlow,
                tokenStore.avatarFlow,
            ) { token, id, role, nick, avatar ->
                SessionState(token, id, nick, role, avatar)
            }.collect { _uiState.value = it }
        }
    }

    fun signOut() = viewModelScope.launch { tokenStore.clear() }
}

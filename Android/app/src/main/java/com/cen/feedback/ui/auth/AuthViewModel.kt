package com.cen.feedback.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.repo.ApiException
import com.cen.feedback.data.repo.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val email: String = "",
    val role: String = "student",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun setUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun setNickname(v: String) = _state.update { it.copy(nickname = v) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setRole(v: String) = _state.update { it.copy(role = v) }
    fun consumeError() = _state.update { it.copy(error = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun login(onSuccess: (role: String) -> Unit) {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "请输入用户名和密码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repo.login(s.username.trim(), s.password.trim(), s.role) }
                .onSuccess { user ->
                    _state.update { it.copy(loading = false) }
                    onSuccess(user.role ?: s.role)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = (e as? ApiException)?.message ?: e.message ?: "登录失败",
                        )
                    }
                }
        }
    }

    fun register(onDone: () -> Unit) {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "请输入用户名和密码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                repo.register(s.username.trim(), s.password.trim(), s.role,
                    s.nickname.ifBlank { s.username }, s.email.ifBlank { null })
            }
                .onSuccess {
                    _state.update {
                        it.copy(loading = false, message = "注册成功，请返回登录")
                    }
                    onDone()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = (e as? ApiException)?.message ?: e.message ?: "注册失败"
                        )
                    }
                }
        }
    }
}

package com.cen.feedback.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.User
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.InlineError
import com.cen.feedback.ui.components.PrimaryButton
import com.cen.feedback.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountSecurityState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val me: User? = null,
    val email: String = "",
    val nickname: String = "",
    val oldPwd: String = "",
    val newPwd: String = "",
    val confirmPwd: String = "",
    val showOldPwd: Boolean = false,
    val showNewPwd: Boolean = false,
    val infoMsg: String? = null,
    val pwdMsg: String? = null,
    val infoOk: Boolean = false,
    val pwdOk: Boolean = false,
)

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountSecurityState())
    val state: StateFlow<AccountSecurityState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { repo.me() }
            .onSuccess { me ->
                _state.update { it.copy(
                    loading = false,
                    me = me,
                    email = me.email.orEmpty(),
                    nickname = me.nickname.orEmpty(),
                ) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false, infoMsg = e.message ?: "加载失败") }
            }
    }

    fun onEmail(v: String) = _state.update { it.copy(email = v, infoMsg = null, infoOk = false) }
    fun onNickname(v: String) = _state.update { it.copy(nickname = v, infoMsg = null, infoOk = false) }
    fun onOldPwd(v: String) = _state.update { it.copy(oldPwd = v, pwdMsg = null, pwdOk = false) }
    fun onNewPwd(v: String) = _state.update { it.copy(newPwd = v, pwdMsg = null, pwdOk = false) }
    fun onConfirmPwd(v: String) = _state.update { it.copy(confirmPwd = v, pwdMsg = null, pwdOk = false) }
    fun toggleShowOld() = _state.update { it.copy(showOldPwd = !it.showOldPwd) }
    fun toggleShowNew() = _state.update { it.copy(showNewPwd = !it.showNewPwd) }

    fun saveProfile() = viewModelScope.launch {
        val s = _state.value
        if (s.email.isNotBlank() && !s.email.contains("@")) {
            _state.update { it.copy(infoMsg = "邮箱格式不正确", infoOk = false) }
            return@launch
        }
        _state.update { it.copy(saving = true, infoMsg = null) }
        runCatching { repo.updateMyProfile(email = s.email.trim(), nickname = s.nickname.trim()) }
            .onSuccess { u ->
                _state.update { it.copy(saving = false, me = u, infoMsg = "资料已更新", infoOk = true) }
            }
            .onFailure { e ->
                _state.update { it.copy(saving = false, infoMsg = e.message ?: "保存失败", infoOk = false) }
            }
    }

    fun changePassword() = viewModelScope.launch {
        val s = _state.value
        when {
            s.oldPwd.isBlank() -> {
                _state.update { it.copy(pwdMsg = "请输入旧密码") }; return@launch
            }
            s.newPwd.length < 6 -> {
                _state.update { it.copy(pwdMsg = "新密码至少 6 位") }; return@launch
            }
            s.newPwd != s.confirmPwd -> {
                _state.update { it.copy(pwdMsg = "两次输入的新密码不一致") }; return@launch
            }
            s.newPwd == s.oldPwd -> {
                _state.update { it.copy(pwdMsg = "新密码不能与旧密码相同") }; return@launch
            }
        }
        _state.update { it.copy(saving = true) }
        runCatching { repo.changeMyPassword(s.oldPwd, s.newPwd) }
            .onSuccess {
                _state.update { it.copy(
                    saving = false,
                    pwdMsg = "密码已更新，下次登录请使用新密码",
                    pwdOk = true,
                    oldPwd = "", newPwd = "", confirmPwd = "",
                ) }
            }
            .onFailure { e ->
                _state.update { it.copy(saving = false, pwdMsg = e.message ?: "修改失败", pwdOk = false) }
            }
    }
}

@Composable
fun AccountSecurityScreen(
    onBack: () -> Unit,
    vm: AccountSecurityViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GradientTopBar(title = "账户安全", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        SectionTitle("基本资料")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = s.me?.username.orEmpty(),
                    onValueChange = {},
                    enabled = false,
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    label = { Text("用户名（不可修改）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = s.nickname,
                    onValueChange = vm::onNickname,
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = s.email,
                    onValueChange = vm::onEmail,
                    leadingIcon = { Icon(Icons.Rounded.Email, null) },
                    label = { Text("邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (s.infoMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.infoMsg!!,
                        color = if (s.infoOk) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = if (s.saving) "保存中..." else "保存资料",
                    onClick = vm::saveProfile,
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("修改密码")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = s.oldPwd,
                    onValueChange = vm::onOldPwd,
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = vm::toggleShowOld) {
                            Icon(
                                if (s.showOldPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null,
                            )
                        }
                    },
                    visualTransformation = if (s.showOldPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("旧密码") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = s.newPwd,
                    onValueChange = vm::onNewPwd,
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = vm::toggleShowNew) {
                            Icon(
                                if (s.showNewPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null,
                            )
                        }
                    },
                    visualTransformation = if (s.showNewPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("新密码（至少 6 位）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = s.confirmPwd,
                    onValueChange = vm::onConfirmPwd,
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    visualTransformation = if (s.showNewPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("再次输入新密码") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (s.pwdMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.pwdMsg!!,
                        color = if (s.pwdOk) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = if (s.saving) "提交中..." else "更新密码",
                    onClick = vm::changePassword,
                    enabled = !s.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

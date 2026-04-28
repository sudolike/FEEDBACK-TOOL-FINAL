package com.cen.feedback.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.R
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.InlineError
import com.cen.feedback.ui.components.PrimaryButton
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary500
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Primary700

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val nicknameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // 进入注册页后，强制重置角色为 student，避免误用 admin
        if (s.role !in listOf("student", "teacher")) vm.setRole("student")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Primary700, Primary500, Pink500.copy(alpha = 0.85f)))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // 复用 GradientTopBar 保持与登录页视觉语言统一（需求 3 验收点 5）
            GradientTopBar(
                title = stringResource(R.string.auth_create_account),
                onBack = onBack,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.auth_register_sub),
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // 注册仅允许学生 / 教师身份；管理员账号由系统内置，不开放注册
                    RegisterRoleSwitcher(role = s.role, onRoleChange = vm::setRole)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.auth_admin_closed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = s.username, onValueChange = vm::setUsername,
                        label = { Text(stringResource(R.string.auth_username)) },
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            nicknameFocus.requestFocus()
                        }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.nickname, onValueChange = vm::setNickname,
                        label = { Text(stringResource(R.string.auth_nickname)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            emailFocus.requestFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nicknameFocus),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.email, onValueChange = vm::setEmail,
                        label = { Text(stringResource(R.string.auth_email)) },
                        leadingIcon = { Icon(Icons.Rounded.Email, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            passwordFocus.requestFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocus),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.password, onValueChange = vm::setPassword,
                        label = { Text(stringResource(R.string.auth_password)) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            vm.register { }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus),
                    )
                    InlineError(s.error)
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = stringResource(R.string.btn_register_cta),
                        onClick = {
                            focusManager.clearFocus()
                            vm.register { }
                        },
                        loading = s.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (s.message != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.message!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 注册专用的角色切换器：仅允许 student / teacher，没有 admin 选项 */
@Composable
private fun RegisterRoleSwitcher(role: String, onRoleChange: (String) -> Unit) {
    val options = listOf(
        "student" to stringResource(R.string.auth_role_student),
        "teacher" to stringResource(R.string.auth_role_teacher),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val selected = role == value
            val container = if (selected)
                Brush.horizontalGradient(listOf(Primary600, Primary400))
            else
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(container)
                    .clickable { onRoleChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

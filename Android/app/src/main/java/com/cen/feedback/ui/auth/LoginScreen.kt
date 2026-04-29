package com.cen.feedback.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.R
import com.cen.feedback.ui.components.InlineError
import com.cen.feedback.ui.components.PrimaryButton
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary500
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Primary700
import kotlin.math.cos
import kotlin.math.sin

/**
 * 登录页：渐变背景 + 玻璃卡片 + 角色切换 Tab + 流畅动画。
 */
@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Primary700, Primary500, Pink500.copy(alpha = 0.85f))
                )
            )
    ) {
        AnimatedBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            // Logo & 标题
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White,
                    modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.app_brand),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.app_brand_sub),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(32.dp))

            // 卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        stringResource(R.string.auth_welcome_back),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))

                    RoleSwitcher(
                        role = state.role,
                        onRoleChange = vm::setRole,
                    )
                    Spacer(Modifier.height(20.dp))

                    var userFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = vm::setUsername,
                        label = { Text(stringResource(R.string.auth_username)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Person, null,
                                tint = if (userFocused) Primary600
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { userFocused = it.isFocused },
                    )
                    Spacer(Modifier.height(12.dp))
                    var pwdFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = vm::setPassword,
                        label = { Text(stringResource(R.string.auth_password)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Lock, null,
                                tint = if (pwdFocused) Primary600
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility, null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus)
                            .onFocusChanged { pwdFocused = it.isFocused },
                    )
                    InlineError(state.error)

                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = stringResource(R.string.btn_login_cta),
                        onClick = {
                            focusManager.clearFocus()
                            vm.login {}
                        },
                        loading = state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    // 管理员提示 —— 需求 3 验收点 4
                    val isAdmin = state.role == "admin"
                    if (isAdmin) {
                        Surface(
                            color = Primary600.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.auth_admin_closed),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary700,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.auth_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onRegister, enabled = !isAdmin) {
                            Text(stringResource(R.string.auth_register_now))
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Text(
                stringResource(R.string.auth_footer),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun RoleSwitcher(role: String, onRoleChange: (String) -> Unit) {
    val options = listOf(
        "student" to stringResource(R.string.auth_role_student),
        "teacher" to stringResource(R.string.auth_role_teacher),
        "admin" to stringResource(R.string.auth_role_admin),
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
            val container = if (selected) Brush.horizontalGradient(listOf(Primary600, Primary400))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
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

/**
 * 装饰性的浮动光晕背景。
 *
 * 需求 3 验收点 1：改为 lissajous 循环轨迹，降低透明度 ≤ 0.15，避免抢焦点。
 */
@Composable
private fun AnimatedBackdrop() {
    val infinite = rememberInfiniteTransition(label = "backdrop")
    val phase by infinite.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(8000)),
        label = "phase"
    )
    val rad = Math.toRadians(phase.toDouble())
    val dx1 = (sin(rad) * 30).toFloat()
    val dy1 = (cos(rad * 1.3) * 20).toFloat()
    val dx2 = (cos(rad * 0.8) * 24).toFloat()
    val dy2 = (sin(rad * 1.2) * 18).toFloat()
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (80 + dx1).dp, y = (100 + dy1).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40 + dx2).dp, y = (-80 + dy2).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Pink500.copy(alpha = 0.15f))
        )
    }
}

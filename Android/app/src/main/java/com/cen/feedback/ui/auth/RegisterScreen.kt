package com.cen.feedback.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "创建账号",
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "选择身份后注册，登录后将自动进入对应界面",
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.White.copy(alpha = 0.85f),
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
                        "管理员账号不开放公开注册，请联系系统团队获取账号",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = s.username, onValueChange = vm::setUsername,
                        label = { Text("用户名") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.nickname, onValueChange = vm::setNickname,
                        label = { Text("昵称") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.email, onValueChange = vm::setEmail,
                        label = { Text("邮箱（可选）") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = s.password, onValueChange = vm::setPassword,
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    InlineError(s.error)
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = "注 册",
                        onClick = { vm.register { } },
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
    val options = listOf("student" to "学生", "teacher" to "教师")
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
                androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Primary600, Primary400))
            else
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(androidx.compose.ui.graphics.Color.Transparent,
                           androidx.compose.ui.graphics.Color.Transparent)
                )
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
                    color = if (selected) androidx.compose.ui.graphics.Color.White
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

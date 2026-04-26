package com.cen.feedback.ui.student.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.theme.*

@Composable
fun StudentProfileScreen(
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit = {},
    session: SessionViewModel = hiltViewModel(),
) {
    val s by session.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Primary600, Primary400, Pink500.copy(alpha = 0.6f))))
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        s.nickname ?: "同学",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "学生 · ID #${s.userId ?: "-"}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingItem(Icons.Rounded.Notifications, "消息通知", subtitle = "教师答疑、问卷推送",
            onClick = { onNavigate("common/notifications") })
        SettingItem(Icons.Rounded.Lock, "账户安全", subtitle = "修改密码、绑定邮箱",
            onClick = { onNavigate("common/account-security") })
        SettingItem(Icons.Rounded.Bookmark, "我的收藏", subtitle = "课程资料 / 历史回答",
            onClick = { onNavigate("common/bookmarks") })
        SettingItem(Icons.Rounded.AutoAwesome, "AI 助手设置", subtitle = "RAG 检索源、对话偏好",
            onClick = { onNavigate("common/assistant-settings") })
        SettingItem(Icons.Rounded.HelpOutline, "帮助与反馈", subtitle = "常见问题、联系开发者",
            onClick = { onNavigate("common/help-feedback") })

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            "退出登录",
            icon = Icons.Rounded.Logout,
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Primary600) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

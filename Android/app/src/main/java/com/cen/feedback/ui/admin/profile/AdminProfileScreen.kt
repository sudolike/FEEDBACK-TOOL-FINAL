package com.cen.feedback.ui.admin.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.ui.session.SessionViewModel
import com.cen.feedback.ui.theme.*

@Composable
fun AdminProfileScreen(
    onLogout: () -> Unit,
    session: SessionViewModel = hiltViewModel(),
) {
    val s by session.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Primary800, Primary600, Pink500.copy(alpha = 0.6f))))
        ) {
            Column(modifier = Modifier.statusBarsPadding().padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AdminPanelSettings, null, tint = Color.White) }
                Spacer(Modifier.height(12.dp))
                Text(
                    s.nickname ?: "管理员",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "ID #${s.userId ?: "-"} · 角色：管理员",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        ListItem(
            headlineContent = { Text("安全提示") },
            supportingContent = {
                Text("• 管理员账号不开放注册\n• 请定期轮换密码\n• 不要在公共场景泄漏账号")
            },
            leadingContent = { Icon(Icons.Rounded.Security, null, tint = Pink500) },
        )
        Divider()
        ListItem(
            headlineContent = { Text("默认密码（首次登录后请修改）") },
            supportingContent = {
                Text(
                    "admin01 / Admin@Cen2026!Feedback\n" +
                    "admin02 / Cen#Admin2026!Master",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            leadingContent = { Icon(Icons.Rounded.VpnKey, null, tint = Primary600) },
        )
        Divider()

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Danger500),
        ) {
            Icon(Icons.Rounded.Logout, null)
            Spacer(Modifier.width(6.dp))
            Text("退出登录")
        }
    }
}

package com.cen.feedback.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cen.feedback.ui.admin.approval.AdminCourseApprovalScreen
import com.cen.feedback.ui.admin.courses.AdminCourseListScreen
import com.cen.feedback.ui.admin.dashboard.AdminDashboardScreen
import com.cen.feedback.ui.admin.profile.AdminProfileScreen
import com.cen.feedback.ui.admin.users.AdminUserListScreen
import com.cen.feedback.ui.session.SessionViewModel

/**
 * 管理员端主框架（5 个底栏 Tab）：
 *  - 看板：用户/课程/反馈数据总览
 *  - 审批：待审批课程列表 → 通过 / 驳回
 *  - 课程：全平台课程（含 pending / approved / rejected 切换）
 *  - 用户：分页 / 启停 / 重置密码 / 删除
 *  - 我的：账号信息 + 退出
 */
enum class AdminTab(val title: String, val icon: ImageVector) {
    Dashboard("看板",  Icons.Rounded.Dashboard),
    Approval("审批",   Icons.Rounded.PlaylistAddCheck),
    Courses("课程",    Icons.Rounded.Class),
    Users("用户",      Icons.Rounded.Group),
    Profile("我的",    Icons.Rounded.Person),
}

@Composable
fun AdminMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(AdminTab.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                AdminTab.values().forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.title) },
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "admin-tab",
            ) { current ->
                when (current) {
                    AdminTab.Dashboard -> AdminDashboardScreen(onJumpTab = { tab = it })
                    AdminTab.Approval  -> AdminCourseApprovalScreen()
                    AdminTab.Courses   -> AdminCourseListScreen()
                    AdminTab.Users     -> AdminUserListScreen()
                    AdminTab.Profile   -> AdminProfileScreen(onLogout = session::signOut)
                }
            }
        }
    }
}

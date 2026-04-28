package com.cen.feedback.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cen.feedback.R
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
enum class AdminTab(
    val titleRes: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Dashboard(R.string.tab_dashboard, Icons.Rounded.Dashboard, Icons.Filled.Dashboard),
    Approval(R.string.tab_approval, Icons.Rounded.PlaylistAddCheck, Icons.Filled.PlaylistAddCheck),
    Courses(R.string.tab_courses, Icons.Rounded.Class, Icons.Filled.Class),
    Users(R.string.tab_users, Icons.Rounded.Group, Icons.Filled.Group),
    Profile(R.string.tab_profile, Icons.Rounded.Person, Icons.Filled.Person),
}

@Composable
fun AdminMainScaffold(
    navController: NavController,
    session: SessionViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(AdminTab.Dashboard) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                AdminTab.values().forEach { item ->
                    val selected = tab == item
                    val scale by animateFloatAsState(
                        if (selected) 1f else 0.92f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab-scale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != item) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tab = item
                        },
                        icon = {
                            Icon(
                                if (selected) item.iconSelected else item.icon, null,
                                modifier = Modifier.scale(scale),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
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
                transitionSpec = {
                    (slideInHorizontally { 40 } + fadeIn()) togetherWith fadeOut()
                },
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

package com.cen.feedback.ui.student.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.data.model.StudentEnrollmentRow
import com.cen.feedback.ui.components.EmptyStateAction
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.SectionTitle
import com.cen.feedback.ui.components.StatusChip
import com.cen.feedback.ui.components.shimmerCards
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.student.home.CourseRow
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary600

@Composable
fun StudentCourseListScreen(
    navController: NavController,
    vm: StudentCourseListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(s.opMsg) {
        s.opMsg?.let {
            snackbar.showSnackbar(it)
            vm.consumeMsg()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            GradientTopBar(
                title = "我的课程",
                actions = {
                    AssistChip(
                        onClick = { navController.navigate(Routes.STUDENT_DISCOVER) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.White) },
                        label = { Text("发现课程", color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                if (s.invitations.isNotEmpty()) {
                    item {
                        SectionTitle("教师邀请", trailing = {
                            StatusChip("${s.invitations.size}", Pink500)
                        })
                    }
                    items(s.invitations) { row ->
                        InvitationRow(
                            row = row,
                            onAccept = { row.enrollment?.id?.let(vm::acceptInvite) },
                            onDecline = { row.enrollment?.id?.let(vm::declineInvite) },
                        )
                    }
                }

                if (s.pendingApplies.isNotEmpty()) {
                    item { SectionTitle("待审批的申请") }
                    items(s.pendingApplies) { row ->
                        PendingApplyRow(
                            row = row,
                            onCancel = { row.enrollment?.id?.let(vm::cancelApply) },
                        )
                    }
                }

                if (s.rejectedApplies.isNotEmpty()) {
                    item { SectionTitle("已驳回") }
                    items(s.rejectedApplies) { row ->
                        RejectedApplyRow(row)
                    }
                }

                item {
                    SectionTitle(
                        title = "我的课程",
                        trailing = {
                            StatusChip("${s.courses.size}", Primary600)
                        },
                    )
                }

                if (s.loading && s.courses.isEmpty()) {
                    shimmerCards()
                } else if (s.courses.isEmpty()) {
                    item {
                        EmptyStateAction(
                            icon = Icons.Rounded.Inbox,
                            title = "尚未加入任何课程",
                            subtitle = "可以点\"发现课程\"申请加入，或等待教师邀请。",
                            actionText = "发现课程",
                            onAction = { navController.navigate(Routes.STUDENT_DISCOVER) },
                        )
                    }
                } else {
                    items(s.courses) { c ->
                        CourseRow(c) { navController.navigate(Routes.courseDetail(c.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationRow(
    row: StudentEnrollmentRow,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(row.course?.name ?: "课程", fontWeight = FontWeight.SemiBold)
            row.course?.code?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip("教师邀请", Pink500)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDecline) {
                    Icon(Icons.Rounded.Close, null)
                    Spacer(Modifier.width(4.dp))
                    Text("拒绝")
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalButton(onClick = onAccept) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text("接受")
                }
            }
        }
    }
}

@Composable
private fun PendingApplyRow(
    row: StudentEnrollmentRow,
    onCancel: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.course?.name ?: "课程", fontWeight = FontWeight.SemiBold)
                row.enrollment?.applyMessage?.takeIf { it.isNotBlank() }?.let {
                    Text("留言：$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip("等待教师审批", Primary600)
            }
            TextButton(onClick = onCancel) {
                Icon(Icons.Rounded.Cancel, null)
                Spacer(Modifier.width(4.dp))
                Text("撤回")
            }
        }
    }
}

@Composable
private fun RejectedApplyRow(row: StudentEnrollmentRow) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(row.course?.name ?: "课程",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer)
            row.enrollment?.rejectReason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text("驳回原因：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

package com.cen.feedback.ui.student.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.QuestionnaireFullInfoDTO
import com.cen.feedback.ui.components.GlassCard
import com.cen.feedback.ui.components.MetricCard
import com.cen.feedback.ui.components.SectionTitle
import com.cen.feedback.ui.components.StatusChip
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.Accent500
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Slate900
import com.cen.feedback.ui.theme.Success500
import com.cen.feedback.ui.theme.Warning500

/**
 * 学生首页：渐变标题区 + 数据指标 + 当前进行中问卷 + 我的课程。
 */
@Composable
fun StudentHomeScreen(
    navController: NavController,
    onJumpTab: (com.cen.feedback.ui.student.StudentTab) -> Unit = {},
    vm: StudentHomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            HeaderHero(
                nickname = s.nickname,
                courseCount = s.courses.size,
                pendingQ = s.questionnaires.count { it.status == 1 && it.hasSubmitted != true },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.LibraryBooks,
                    label = "我的课程",
                    value = s.courses.size.toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = "待填问卷",
                    value = s.questionnaires.count { it.status == 1 && it.hasSubmitted != true }
                        .toString(),
                    accent = Warning500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle("进行中的问卷") }
        if (s.questionnaires.none { it.status == 1 }) {
            item {
                EmptyHint("暂无进行中的问卷～\n好好享受当下的课堂吧。")
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.questionnaires.filter { it.status == 1 }) { q ->
                        QuestionnaireCard(q) {
                            val cId = q.course?.id ?: return@QuestionnaireCard
                            val qId = q.questionnaire?.id ?: return@QuestionnaireCard
                            navController.navigate(Routes.questionnaireFill(cId, qId))
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("我的课程", trailing = {
                TextButton(onClick = { onJumpTab(com.cen.feedback.ui.student.StudentTab.Courses) }) {
                    Text("查看全部")
                }
            })
        }
        if (s.courses.isEmpty()) {
            item { EmptyHint("还没有选课记录\n请联系教师将你加入课程。") }
        } else {
            items(s.courses.take(6)) { c ->
                CourseRow(c) {
                    navController.navigate(Routes.courseDetail(c.id))
                }
            }
        }
    }
}

@Composable
private fun HeaderHero(nickname: String, courseCount: Int, pendingQ: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Primary600, Primary400, Pink500.copy(alpha = 0.7f)))),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("早安，$nickname", color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "今天已选 $courseCount 门课，${if (pendingQ > 0) "$pendingQ 份问卷待你完成" else "暂无待处理问卷"}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "试试 AI 助手：选课推荐 / 课程难度评估 / 课评摘要",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun QuestionnaireCard(q: QuestionnaireFullInfoDTO, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .heightIn(min = 140.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = q.statusDescription ?: "进行中",
                    color = if (q.status == 1) Success500 else Slate900.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(8.dp))
                if (q.hasSubmitted == true) {
                    StatusChip(text = "已提交", color = Accent500)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                q.questionnaire?.title ?: "未命名问卷",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${q.course?.name ?: ""} · ${q.teacher?.nickname ?: q.teacher?.username ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CourseRow(c: Courses, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    c.name?.firstOrNull()?.toString() ?: "课",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name ?: "—", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(c.code, c.academicYear, c.semester?.let { "第 $it 学期" })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

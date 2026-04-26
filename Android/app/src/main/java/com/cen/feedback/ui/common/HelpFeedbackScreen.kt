package com.cen.feedback.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.SectionTitle

private val FAQ_LIST = listOf(
    "找不到老师创建的课程？" to
        "课程必须经过管理员审批后才会公开。如果老师已审批通过但你看不到，请到\"发现课程\"页搜索并申请加入，或等待老师邀请。",
    "申请加入课程多久会被处理？" to
        "教师收到申请后会在课程详情的\"学生\"Tab 处理。一般同一节课的申请教师在每周授课前批量处理。",
    "为什么我提交的反馈是匿名的？" to
        "为了保护学生隐私，教师端展示反馈与问卷答题时只看到匿名 ID，不会显示真实账号。",
    "AI 助手回答里 \"引用\" 是什么？" to
        "引用是 RAG 系统从课程资料 / 评价 / 问卷中检索到的原文片段，让你可以核对回答的来源。",
    "忘记密码怎么办？" to
        "请联系管理员（admin01 / admin02）使用\"重置密码\"功能给你下发新密码。",
)

@Composable
fun HelpFeedbackScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientTopBar(title = "帮助与反馈", onBack = onBack)

        SectionTitle("常见问题")
        FAQ_LIST.forEach { (q, a) ->
            FaqItem(question = q, answer = a)
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("联系我们")
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MailOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("反馈邮箱", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "feedback-tool@example.com",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "也可以前往 https://github.com/sudolike/FEEDBACK-TOOL-FINAL 提交 Issue。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by rememberSaveable(question) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

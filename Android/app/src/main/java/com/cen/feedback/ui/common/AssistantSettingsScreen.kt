package com.cen.feedback.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.SectionTitle

/**
 * "AI 助手设置"页：
 *  - RAG 检索源开关（前端状态，后续可写入服务端 user.preferences）
 *  - 对话风格 / 语言偏好
 *  - 清空本地对话历史
 */
@Composable
fun AssistantSettingsScreen(onBack: () -> Unit) {
    var useResources by rememberSaveable { mutableStateOf(true) }
    var useFeedback by rememberSaveable { mutableStateOf(true) }
    var useQuestionnaire by rememberSaveable { mutableStateOf(true) }
    var conciseMode by rememberSaveable { mutableStateOf(false) }
    var citeSources by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientTopBar(title = "AI 助手设置", onBack = onBack)

        SectionTitle("RAG 检索源")
        SettingSwitch(Icons.Rounded.MenuBook, "课程资料", "在回答中检索 PPT / PDF / 录播",
            useResources) { useResources = it }
        SettingSwitch(Icons.Rounded.RateReview, "课程评价", "汇总学生评价用于推荐",
            useFeedback) { useFeedback = it }
        SettingSwitch(Icons.Rounded.Quiz, "问卷答题", "基于问卷数据给出趋势分析",
            useQuestionnaire) { useQuestionnaire = it }

        Spacer(Modifier.height(8.dp))
        SectionTitle("对话偏好")
        SettingSwitch(Icons.Rounded.AutoAwesome, "简洁模式", "回答更短、关键点列表化",
            conciseMode) { conciseMode = it }
        SettingSwitch(Icons.Rounded.MenuBook, "引用来源", "在回答末尾附带引用",
            citeSources) { citeSources = it }

        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("说明", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "当前为本地偏好设置，不会改变后端 RAG 行为；后续会通过用户偏好接口同步到服务端。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

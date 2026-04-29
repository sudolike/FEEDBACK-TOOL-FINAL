package com.cen.feedback.ui.teacher.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.TypewriterText
import com.cen.feedback.ui.student.ai.AiViewModel
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600

/**
 * 教师端"AI 助教"全屏页：
 *  - 与学生端 AiViewModel 共用底层接口
 *  - 提供面向教师的快捷指令（总结反馈、起草题目、教学建议）
 */
@Composable
fun TeacherAssistantScreen(
    onBack: () -> Unit,
    vm: AiViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        GradientTopBar(title = "AI 助教", onBack = onBack)

        if (state.messages.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("教学小助手已就绪", fontWeight = FontWeight.SemiBold)
                Text(
                    "可以让我帮你总结问卷反馈、起草题目、给出改进建议。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            QuickActions(
                actions = listOf(
                    "总结这次问卷反馈" to "请基于我最近发布的问卷反馈，总结学生评价并给出 3 条改进建议。",
                    "起草问卷题目" to "请帮我针对刚结束的章节起草一份课程反馈问卷，给出 8-10 题。",
                    "本周教学建议" to "结合最近的课评数据，给我本周可执行的教学改进建议。",
                ),
                sending = state.sending,
                onAction = { vm.send(it) },
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { m ->
                val isUser = m.role == "user"
                val isLastAssistant =
                    !isUser && m === state.messages.lastOrNull() && !state.sending
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        color = if (isUser) Primary600 else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                        ),
                    ) {
                        if (isLastAssistant) {
                            TypewriterText(
                                fullText = m.text,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text(
                                m.text,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (state.sending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "助教思考中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 输入栏
        Surface(tonalElevation = 4.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("跟 AI 助教说点什么…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    singleLine = false,
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val txt = input.trim()
                        if (txt.isNotEmpty() && !state.sending) {
                            vm.send(txt)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !state.sending,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Rounded.Send, null)
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    actions: List<Pair<String, String>>,
    sending: Boolean,
    onAction: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { (label, prompt) ->
            AssistChip(
                onClick = { if (!sending) onAction(prompt) },
                label = { Text(label) },
            )
        }
    }
}

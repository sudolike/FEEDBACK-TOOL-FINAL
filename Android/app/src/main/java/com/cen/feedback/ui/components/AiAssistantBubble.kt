package com.cen.feedback.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Slate900

/* === 一条消息的轻量数据 === */
data class AiMsg(val role: String, val text: String)

/**
 * 学生端浮动 AI 助手按钮 + 弹出聊天面板。
 * 调用方仅需提供：当前消息列表、加载状态、发送回调。
 */
@Composable
fun AiAssistantFab(
    messages: List<AiMsg>,
    sending: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    quickActions: List<Pair<String, String>> = listOf(
        "总结这门课的口碑" to "请基于课程评价数据总结这门课的口碑、优点、不足。",
        "选课推荐" to "我现在该选什么课？请基于历史评价给我推荐。",
        "评估课程难度" to "请基于历史反馈评估这门课的难度，并给出学习建议。",
    ),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        // 浮动按钮
        AnimatedVisibility(!open, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            PulsingFab(onClick = { open = true })
        }
        // 弹出面板
        AnimatedVisibility(
            open,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        ) {
            AiPanel(
                messages = messages,
                sending = sending,
                quickActions = quickActions,
                onSend = onSend,
                onClose = { open = false },
            )
        }
    }
}

@Composable
private fun PulsingFab(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "fab-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(64.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Primary600, Pink500)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.AutoAwesome, "AI 助手", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun AiPanel(
    messages: List<AiMsg>,
    sending: Boolean,
    quickActions: List<Pair<String, String>>,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 480.dp, max = 600.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 24.dp,
    ) {
        Column {
            // 头部
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Primary600, Primary400)))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 学习助手", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("基于课程数据 RAG 检索增强", color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "关闭", tint = Color.White)
                    }
                }
            }
            // 消息区
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Hi～我是你的学习助手 ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("我可以帮你总结课程口碑、推荐选课方向，或评估课程难度。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickActions.forEach { (label, prompt) ->
                                AssistChip(
                                    onClick = { onSend(prompt) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { MsgBubble(it) }
                    if (sending) item { TypingIndicator() }
                }
            }
            // 输入区
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("问我点什么…") },
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = input.isNotBlank() && !sending
                    val sendScale by animateFloatAsStatePublic(if (canSend) 1f else 0.9f)
                    IconButton(
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty() && !sending) {
                                onSend(text); input = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .scale(sendScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (canSend) listOf(Primary600, Pink500)
                                    else listOf(Color.Gray, Color.Gray)
                                )
                            ),
                        enabled = canSend,
                    ) {
                        Icon(Icons.Rounded.Send, "发送", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun MsgBubble(m: AiMsg) {
    val isUser = m.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bg = if (isUser) Primary600 else MaterialTheme.colorScheme.surfaceVariant
        val fg = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp,
            ),
            color = bg,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .widthIn(max = 280.dp),
        ) {
            Text(
                m.text,
                color = fg,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infinite = rememberInfiniteTransition(label = "typing")
    val frame by infinite.animateFloat(
        0f, 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.LinearEasing)),
        label = "frame",
    )
    Surface(
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0..2) {
                val active = frame.toInt() == i
                val a by animateFloatAsStatePublic(if (active) 1f else 0.4f)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Slate900.copy(alpha = a))
                )
            }
        }
    }
}

@Composable
private fun animateFloatAsStatePublic(target: Float): State<Float> =
    androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "anim",
    )

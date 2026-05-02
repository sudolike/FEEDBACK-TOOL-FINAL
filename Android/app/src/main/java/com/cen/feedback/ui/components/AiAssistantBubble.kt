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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cen.feedback.R
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Slate900

/* === 一条消息的轻量数据 ===
 * sources 为 RAG 引用来源，可为空
 */
data class AiMsg(
    val role: String,
    val text: String,
    val sources: List<String> = emptyList(),
)

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
        "HKU 课程评价总结" to "请总结 HKU GEOG7310 这门课的定位、适合人群和整体口碑。",
        "HKU 选课推荐" to "如果我在 HKU 想学云计算和数据分析，GEOG7307、GEOG7310、COMP7305 更推荐哪门？",
        "HKU 课程难度评估" to "HKU COMP3230 难度怎么样，适合什么背景的学生？",
    ),
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        // 浮动按钮
        AnimatedVisibility(!open, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            PulsingFab(onClick = { open = true })
        }
        // 弹出面板 —— 需求 8 验收点 1：从 FAB 位置放大展开
        AnimatedVisibility(
            open,
            enter = scaleIn(
                initialScale = 0.3f,
                transformOrigin = TransformOrigin(1f, 1f),
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
            ) + fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = scaleOut(
                targetScale = 0.3f,
                transformOrigin = TransformOrigin(1f, 1f),
            ) + fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
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
    // 打字机跳过开关 —— 需求 11 验收点 3
    var skipTyping by remember { mutableStateOf(false) }

    // 记录上一次"是否在底部"
    val atBottom = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(messages.size) {
        // 需求 11 验收点 4：只有用户已停留在底部时才自动跟随
        if (messages.isNotEmpty() && atBottom.value) {
            listState.animateScrollToItem(messages.lastIndex)
        }
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
                        Text(stringResource(R.string.ai_title), color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.ai_subtitle),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.btn_close), tint = Color.White)
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
                            Text(
                                stringResource(R.string.ai_welcome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.ai_welcome_sub),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
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
                    items(messages) { msg ->
                        MsgBubble(
                            m = msg,
                            // 只有最后一条 assistant 消息走打字机
                            isLastAssistant = (msg === messages.lastOrNull()
                                    && msg.role == "assistant" && !sending),
                            skipTyping = skipTyping,
                        )
                    }
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
                        placeholder = { Text(stringResource(R.string.ai_placeholder)) },
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = input.isNotBlank() && !sending
                    val sendScale by animateFloatAsStatePublic(if (canSend) 1f else 0.9f)
                    IconButton(
                        onClick = {
                            if (sending) {
                                // 需求 8 验收点 4：发送中点击 → 跳过打字机动画
                                skipTyping = true
                                return@IconButton
                            }
                            val text = input.trim()
                            if (text.isNotEmpty()) {
                                // 发送新消息前，让上一个打字机立即完成
                                skipTyping = true
                                onSend(text); input = ""
                                skipTyping = false
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .scale(sendScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (canSend || sending) listOf(Primary600, Pink500)
                                    else listOf(Color.Gray, Color.Gray)
                                )
                            ),
                        enabled = canSend || sending,
                    ) {
                        Icon(
                            if (sending) Icons.Rounded.Stop else Icons.Rounded.Send,
                            if (sending) stringResource(R.string.ai_stop_generating)
                            else stringResource(R.string.btn_send),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MsgBubble(
    m: AiMsg,
    isLastAssistant: Boolean = false,
    skipTyping: Boolean = false,
) {
    val isUser = m.role == "user"
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val bg = if (isUser) Primary600 else MaterialTheme.colorScheme.surfaceVariant
        val fg = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp,
                ),
                color = bg,
                modifier = Modifier
                    .combinedClickable(
                        onClick = { /* 单击无动作，预留 */ },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        },
                    ),
            ) {
                if (isLastAssistant) {
                    TypewriterText(
                        fullText = m.text,
                        color = fg,
                        style = MaterialTheme.typography.bodyMedium,
                        skip = skipTyping,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                } else if (isUser) {
                    Text(
                        m.text,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    MarkdownText(
                        text = m.text,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // 长按菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ai_copy)) },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                    onClick = {
                        clipboard.setText(AnnotatedString(m.text))
                        showMenu = false
                    },
                )
            }

            // RAG 引用 chip —— 需求 8 验收点 2
            if (!isUser && m.sources.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Source, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    m.sources.take(6).forEach { src ->
                        AssistChip(
                            onClick = { /* 可展开详情，留待业务接入 */ },
                            label = {
                                Text(
                                    src,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            },
                            modifier = Modifier.heightIn(min = 24.dp),
                        )
                    }
                }
            }
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

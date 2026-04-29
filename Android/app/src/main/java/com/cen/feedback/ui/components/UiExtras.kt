package com.cen.feedback.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportGmailerrorred
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cen.feedback.ui.theme.BrandGradients
import com.cen.feedback.ui.theme.GradientKind
import com.cen.feedback.ui.theme.LocalBrandGradients
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Success500
import com.cen.feedback.ui.theme.Warning500
import com.cen.feedback.ui.theme.toLinearBrush
import kotlinx.coroutines.delay
import java.util.Calendar

/* ============================================================================
 * Needs 4 —— 首页 Hero 统一抽象
 * ========================================================================= */

/**
 * 三端通用的首页 Hero 头部。
 *
 * @param title 主标题（如「早安，阿灿」）
 * @param subtitle 副标题（统计/提示）
 * @param kind 渐变类型，由 [GradientKind] 区分学生/教师/管理员
 * @param tipIcon 底部提示条图标
 * @param tipText 底部提示条文案
 * @param onTipClick 底部提示条点击回调（可空，为空则为装饰性展示）
 * @param avatarIcon 头像区域图标，默认头像人物
 */
@Composable
fun HomeHero(
    title: String,
    subtitle: String,
    kind: GradientKind = GradientKind.StudentHero,
    tipIcon: ImageVector? = Icons.Rounded.AutoAwesome,
    tipText: String? = null,
    onTipClick: (() -> Unit)? = null,
    avatarIcon: ImageVector = Icons.Rounded.Person,
    modifier: Modifier = Modifier,
) {
    val gradients = LocalBrandGradients.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradients.toLinearBrush(kind)),
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
                    Icon(avatarIcon, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (tipText != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp),
                    onClick = onTipClick ?: {},
                    enabled = onTipClick != null,
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (tipIcon != null) {
                            Icon(tipIcon, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            tipText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 根据当前小时返回问候语（早安/午安/晚安）。 */
@Composable
fun rememberGreeting(nickname: String?): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val name = nickname?.takeIf { it.isNotBlank() } ?: "同学"
    return when (hour) {
        in 5..10 -> "早安，$name"
        in 11..13 -> "午安，$name"
        in 14..17 -> "下午好，$name"
        in 18..22 -> "晚上好，$name"
        else -> "夜深了，$name"
    }
}

/* ============================================================================
 * Needs 7 —— 统一空态 & 错误态
 * ========================================================================= */

/** 带 action 按钮的增强空态。老接口 [EmptyState] 保持兼容（不带 action）。 */
@Composable
fun EmptyStateAction(
    icon: ImageVector = Icons.Rounded.Inbox,
    title: String = "暂无数据",
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Primary600.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Primary600, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            SecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

/** 错误态组件：用于替代散落的 InlineError + 重试按钮 */
@Composable
fun ErrorState(
    title: String = "出了点小状况",
    message: String? = null,
    icon: ImageVector = Icons.Rounded.WifiOff,
    retryText: String = "重试",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            SecondaryButton(text = retryText, onClick = onRetry, icon = Icons.Rounded.Refresh)
        }
    }
}

/* ============================================================================
 * Needs 6 —— 问卷填写 sticky 底栏 / 线性进度
 * ========================================================================= */

/**
 * 问卷填写的进度条。
 *
 * @param answered 已答数
 * @param total 总题数
 */
@Composable
fun QuestionnaireProgress(
    answered: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val safe = total.coerceAtLeast(1)
    val progress = (answered.toFloat() / safe).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "已答 $answered / 共 $total 题",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (answered >= total && total > 0) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    tint = Success500, modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Primary600,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/* ============================================================================
 * Needs 11 —— AI 打字机文字
 * ========================================================================= */

/**
 * 打字机效果文本。
 *
 * 使用方式：
 * ```
 * TypewriterText(fullText = msg.text, key = msg.text)
 * ```
 *
 * 关键特性：
 * - 以 [charDelayMs] 逐字符发射
 * - 当 [skip] 为 true（或外部输入变化）SHALL 立即显示全文
 * - 动画进行中末尾显示光标 `▍`
 * - 完成后回调 [onFinished]
 */
@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 24L,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    skip: Boolean = false,
    onFinished: () -> Unit = {},
) {
    var display by remember(fullText) { mutableStateOf("") }
    var done by remember(fullText) { mutableStateOf(false) }

    LaunchedEffect(fullText, skip) {
        if (skip) {
            display = fullText
            done = true
            onFinished()
            return@LaunchedEffect
        }
        display = ""
        done = false
        val sb = StringBuilder()
        for (ch in fullText) {
            sb.append(ch)
            display = sb.toString()
            delay(charDelayMs)
        }
        done = true
        onFinished()
    }

    // 光标闪烁动画
    val infinite = rememberInfiniteTransition(label = "caret")
    val caretAlpha by infinite.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "caret-alpha",
    )

    Text(
        text = if (!done) display + "▍" else display,
        modifier = modifier,
        color = if (color == Color.Unspecified) LocalContentColor.current else color.copy(
            alpha = if (!done) (0.8f + caretAlpha * 0.2f) else 1f
        ),
        style = style,
    )
}

/* ============================================================================
 * Needs 10 —— 高亮闪烁（用于审批通过/驳回的过渡动画）
 * ========================================================================= */

/**
 * 高亮闪烁容器。当 [trigger] 变化时执行一次 150ms 的背景色闪烁。
 */
@Composable
fun HighlightFlash(
    trigger: Any?,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger != null) {
            alpha.snapTo(0.5f)
            alpha.animateTo(0f, tween(durationMillis = 450))
        }
    }
    Box(modifier = modifier) {
        content()
        if (alpha.value > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color.copy(alpha = alpha.value)),
            )
        }
    }
}

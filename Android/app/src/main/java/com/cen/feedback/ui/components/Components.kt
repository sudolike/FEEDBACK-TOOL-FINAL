package com.cen.feedback.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cen.feedback.ui.theme.Primary400
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Primary700
import com.cen.feedback.ui.theme.Slate200
import com.cen.feedback.ui.theme.Slate900

/**
 * 紫蓝渐变背景。学生端首页 / 头部默认采用。
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Primary600, Primary400),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        content = content,
    )
}

/**
 * 通用胶囊按钮（点击有缩放反馈、按压加深）。
 *
 * loading == true 时压缩为圆形 loading（需求 10）。
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn-scale")
    // loading 时使用 IntrinsicSize.Min 压缩为圆形
    val wrapModifier = if (loading) Modifier.width(52.dp) else modifier
    Surface(
        modifier = wrapModifier
            .scale(scale)
            .height(52.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = if (enabled) 6.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(Primary600, Primary400))
                    else Brush.horizontalGradient(listOf(Slate200, Slate200))
                )
                .clickable(
                    interactionSource = interaction,
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.24f)),
                    enabled = enabled && !loading,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(targetState = loading, label = "btn-content") { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (icon != null) Icon(icon, null, tint = Color.White)
                        Text(
                            text,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 第二层级按钮（描边 + 浅底）。
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) Icon(icon, null, modifier = Modifier.padding(end = 6.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

/**
 * 玻璃态卡片，悬浮微阴影 + 圆角。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = Slate900.copy(alpha = 0.08f))
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = MaterialTheme.colorScheme.surface,
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/**
 * 数据指标卡：图标 + 数值 + 标题。
 */
@Composable
fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color = Primary600,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Slate900.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 章节标题：左侧渐变小条 + 标题 + 可选 trailing。
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Primary600, Primary400)))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/** 状态徽章（小药丸） */
@Composable
fun StatusChip(
    text: String,
    color: Color = Primary600,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 简单 5 星评分组件（可点击） */
@Composable
fun StarRatingBar(
    rating: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 28.dp,
    activeColor: Color = Color(0xFFFBBF24),
    inactiveColor: Color = Color(0xFFE5E7EB),
    enabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..5) {
            val filled = i <= rating
            val targetScale by animateFloatAsState(if (filled) 1f else 0.94f, label = "star-$i")
            Icon(
                imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = if (filled) activeColor else inactiveColor,
                modifier = Modifier
                    .size(starSize)
                    .scale(targetScale)
                    .clickable(enabled = enabled) { onChange(i) }
            )
        }
    }
}

/** 简易空状态 */
@Composable
fun EmptyState(
    icon: ImageVector = Icons.Rounded.Inbox,
    title: String = "暂无数据",
    subtitle: String? = null,
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
    }
}

/** 全屏加载罩（用于关键操作中） */
@Composable
fun LoadingOverlay(visible: Boolean) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .pointerInput(Unit) { },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(20.dp)
                        .size(36.dp),
                    color = Primary600,
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}

/** Snackbar 友好的轻量内联错误提示 */
@Composable
fun InlineError(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(text != null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Error, null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text.orEmpty(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** 通用顶部 AppBar（可选返回 + 渐变背景） */
@Composable
fun GradientTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    gradientColors: List<Color>? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = gradientColors ?: listOf(Primary600, Primary400)
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(colors)),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = Color.White)
                }
            } else Spacer(Modifier.width(8.dp))
            Text(
                title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            actions()
        }
    }
}

/** 列表骨架占位（淡入淡出脉冲）*/
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

fun LazyListScope.shimmerCards(count: Int = 3) {
    items(items = (0 until count).toList()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }
    }
}

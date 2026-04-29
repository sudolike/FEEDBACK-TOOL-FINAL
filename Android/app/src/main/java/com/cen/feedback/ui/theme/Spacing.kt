package com.cen.feedback.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局间距 token，替代散落的 4/8/12/16/24.dp。
 *
 * 使用方式：
 * ```
 * val spacing = LocalAppSpacing.current
 * Modifier.padding(spacing.md)
 * ```
 */
data class AppSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/** 默认间距方案 */
val DefaultSpacing = AppSpacing()

val LocalAppSpacing = staticCompositionLocalOf { DefaultSpacing }

/**
 * 品牌渐变 token（区分学生 / 教师 / 管理员 & 深浅色）。
 *
 * 统一入口，避免页面散写 `Brush.linearGradient(listOf(Primary600, Primary400))`。
 */
data class BrandGradients(
    /** 学生端主渐变（紫 → 粉紫，活泼轻快） */
    val studentHero: List<Color>,
    /** 教师端主渐变（紫 → 青蓝，理性严谨） */
    val teacherHero: List<Color>,
    /** 管理员主渐变（深紫 → 粉紫，权威沉稳） */
    val adminHero: List<Color>,
    /** 按钮 / FAB 主渐变 */
    val primary: List<Color>,
    /** 强调渐变（AI 助手按钮、警告类） */
    val accent: List<Color>,
)

/** 浅色模式渐变 */
val LightBrandGradients = BrandGradients(
    studentHero = listOf(Primary600, Primary400, Pink500.copy(alpha = 0.7f)),
    teacherHero = listOf(Accent600, Primary600, Primary400),
    adminHero = listOf(Primary800, Primary600, Pink500),
    primary = listOf(Primary600, Primary400),
    accent = listOf(Primary600, Pink500),
)

/** 深色模式渐变（饱和度降低 20%，避免刺眼） */
val DarkBrandGradients = BrandGradients(
    studentHero = listOf(Primary800, Primary600, Pink500.copy(alpha = 0.55f)),
    teacherHero = listOf(Accent600.copy(alpha = 0.85f), Primary700, Primary500),
    adminHero = listOf(Primary900, Primary700, Pink500.copy(alpha = 0.75f)),
    primary = listOf(Primary700, Primary500),
    accent = listOf(Primary700, Pink500.copy(alpha = 0.85f)),
)

val LocalBrandGradients = compositionLocalOf { LightBrandGradients }

/** 便捷扩展：取当前渐变 token 并转为水平 Brush */
@Composable
@ReadOnlyComposable
fun BrandGradients.toHorizontalBrush(kind: GradientKind): Brush = Brush.horizontalGradient(
    when (kind) {
        GradientKind.StudentHero -> studentHero
        GradientKind.TeacherHero -> teacherHero
        GradientKind.AdminHero -> adminHero
        GradientKind.Primary -> primary
        GradientKind.Accent -> accent
    }
)

@Composable
@ReadOnlyComposable
fun BrandGradients.toLinearBrush(kind: GradientKind): Brush = Brush.linearGradient(
    when (kind) {
        GradientKind.StudentHero -> studentHero
        GradientKind.TeacherHero -> teacherHero
        GradientKind.AdminHero -> adminHero
        GradientKind.Primary -> primary
        GradientKind.Accent -> accent
    }
)

enum class GradientKind {
    StudentHero, TeacherHero, AdminHero, Primary, Accent,
}

/** 补充一个 pill 药丸形状（需求 1 验收点 3） */
val PillShape = RoundedCornerShape(50)

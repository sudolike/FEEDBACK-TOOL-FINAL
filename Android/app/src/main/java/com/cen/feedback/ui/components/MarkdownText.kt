package com.cen.feedback.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  轻量 Markdown 渲染
 *  ----------------------------------------------------------------------------
 *  专门服务于 AI 回复（聊天气泡 / 问卷分析摘要）。
 *
 *  支持的子集（覆盖 LLM 99% 的输出格式）：
 *    - 标题：行首 1~6 个 `#` + 空格 → 加粗 + 较大字号
 *    - 粗体：`**xxx**` 或 `__xxx__`
 *    - 斜体：`*xxx*` 或 `_xxx_`
 *    - 行内代码：`` `xxx` ``
 *    - 无序列表：行首 `- ` / `* ` / `+ ` → 替换为 `•  `
 *    - 引用块：行首 `> ` → 加 `│ ` 前缀 + 灰色斜体
 *    - 行内链接：`[text](url)` → 仅保留 `text`，去掉 url 噪声
 *
 *  设计原则：
 *    - 严格成对：未闭合的 `**` / `*` / `` ` `` 仅删除标记本身，保留正文
 *    - 容错优先：解析失败时降级为纯文本（永远不抛异常）
 *    - 流式友好：`fullText` 还在打字机半截时也能正确渲染（因此适合给
 *      [TypewriterText] 实时使用）
 * ========================================================================= */

private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
private val BULLET_REGEX = Regex("""^\s*[-*+]\s+(.*)$""")
private val QUOTE_REGEX = Regex("""^\s*>\s?(.*)$""")
private val NUMBERED_REGEX = Regex("""^(\s*)(\d+)[\.\)]\s+(.*)$""")

private val BOLD_REGEX = Regex("""\*\*([^*\n]+?)\*\*|__([^_\n]+?)__""")
private val INLINE_CODE_REGEX = Regex("""`([^`\n]+?)`""")
private val LINK_REGEX = Regex("""\[([^\]]+)\]\([^)]+\)""")
// 严格的斜体：单 * 不与 ** 重叠，且两侧不是空白
private val ITALIC_REGEX = Regex("""(?<!\*)\*([^*\s][^*\n]*?[^*\s]|[^*\s])\*(?!\*)""")
private val UNDERLINE_ITALIC_REGEX = Regex("""(?<!_)_([^_\s][^_\n]*?[^_\s]|[^_\s])_(?!_)""")

/**
 * 把 markdown 文本解析为带样式的 [AnnotatedString]。
 *
 * 输入完全为空 / 解析失败时返回原文本（不带样式），保证调用方永远不空白。
 */
fun formatMarkdown(raw: String): AnnotatedString {
    if (raw.isEmpty()) return AnnotatedString("")

    return runCatching {
        val normalized = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            // 收敛过多的空行，让段落保持紧凑
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

        buildAnnotatedString {
            val lines = normalized.split("\n")
            for ((idx, lineRaw) in lines.withIndex()) {
                renderLine(lineRaw)
                if (idx != lines.lastIndex) append("\n")
            }
        }
    }.getOrElse { AnnotatedString(raw) }
}

private fun AnnotatedString.Builder.renderLine(line: String) {
    if (line.isEmpty()) return

    HEADING_REGEX.find(line)?.let {
        val level = it.groupValues[1].length
        val content = it.groupValues[2]
        val fontSize = when (level) {
            1 -> 18.sp
            2 -> 17.sp
            3 -> 16.sp
            else -> 15.sp
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
            renderInline(content)
        }
        return
    }

    BULLET_REGEX.find(line)?.let {
        append("•  ")
        renderInline(it.groupValues[1])
        return
    }

    QUOTE_REGEX.find(line)?.let {
        withStyle(
            SpanStyle(
                fontStyle = FontStyle.Italic,
                color = Color(0xFF6B7280),
            )
        ) {
            append("│  ")
            renderInline(it.groupValues[1])
        }
        return
    }

    NUMBERED_REGEX.find(line)?.let {
        val prefix = it.groupValues[1]
        val num = it.groupValues[2]
        val content = it.groupValues[3]
        append(prefix)
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("$num. ")
        }
        renderInline(content)
        return
    }

    renderInline(line)
}

/* ----------------------------- 行内解析 ----------------------------- */

private data class InlineSpan(
    val start: Int,
    val end: Int,
    val style: SpanStyle,
    val text: String,
)

private fun AnnotatedString.Builder.renderInline(text: String) {
    if (text.isEmpty()) return

    val spans = mutableListOf<InlineSpan>()

    // 顺序：链接 > 行内代码 > 粗体 > 斜体（避免重叠误判）
    LINK_REGEX.findAll(text).forEach {
        spans += InlineSpan(
            start = it.range.first,
            end = it.range.last + 1,
            style = SpanStyle(
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Medium,
            ),
            text = it.groupValues[1],
        )
    }
    INLINE_CODE_REGEX.findAll(text).forEach { m ->
        if (overlaps(spans, m.range)) return@forEach
        spans += InlineSpan(
            start = m.range.first,
            end = m.range.last + 1,
            style = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0x1A8B5CF6),
            ),
            text = m.groupValues[1],
        )
    }
    BOLD_REGEX.findAll(text).forEach { m ->
        if (overlaps(spans, m.range)) return@forEach
        val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }
        spans += InlineSpan(
            start = m.range.first,
            end = m.range.last + 1,
            style = SpanStyle(fontWeight = FontWeight.Bold),
            text = inner,
        )
    }
    ITALIC_REGEX.findAll(text).forEach { m ->
        if (overlaps(spans, m.range)) return@forEach
        spans += InlineSpan(
            start = m.range.first,
            end = m.range.last + 1,
            style = SpanStyle(fontStyle = FontStyle.Italic),
            text = m.groupValues[1],
        )
    }
    UNDERLINE_ITALIC_REGEX.findAll(text).forEach { m ->
        if (overlaps(spans, m.range)) return@forEach
        spans += InlineSpan(
            start = m.range.first,
            end = m.range.last + 1,
            style = SpanStyle(fontStyle = FontStyle.Italic),
            text = m.groupValues[1],
        )
    }

    spans.sortBy { it.start }

    if (spans.isEmpty()) {
        append(stripDanglingMarks(text))
        return
    }

    var cursor = 0
    for (span in spans) {
        if (span.start < cursor) continue
        if (cursor < span.start) {
            append(stripDanglingMarks(text.substring(cursor, span.start)))
        }
        withStyle(span.style) { append(span.text) }
        cursor = span.end
    }
    if (cursor < text.length) {
        append(stripDanglingMarks(text.substring(cursor)))
    }
}

private fun overlaps(spans: List<InlineSpan>, range: IntRange): Boolean =
    spans.any { it.start <= range.first && it.end - 1 >= range.last }

/**
 * 删除单独残留的 markdown 标记（未闭合的 `**` / `__` / 行内 `>` 等），
 * 让最终文本干净不出现 `**` 这种符号噪声。
 */
private fun stripDanglingMarks(text: String): String {
    return text
        // 单独出现的 `**`（成对的早被 BOLD_REGEX 消费了）
        .replace(Regex("""\*{2,}"""), "")
        // 单独出现的 `__`
        .replace(Regex("""_{2,}"""), "")
        // 行尾 / 行内孤立的 `>` 引用残留（句子开头的 `>` 已被 QUOTE_REGEX 处理）
        .replace(Regex("""^\s*>\s*""", RegexOption.MULTILINE), "")
}

/* ----------------------------- Composable 包装 ----------------------------- */

/**
 * 静态 markdown 文本。适合非流式场景（如分析摘要、提示文案）。
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val annotated = remember(text) { formatMarkdown(text) }
    Text(
        text = annotated,
        modifier = modifier,
        color = if (color == Color.Unspecified) LocalContentColor.current else color,
        style = style,
    )
}

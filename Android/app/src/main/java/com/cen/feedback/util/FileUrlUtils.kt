package com.cen.feedback.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.cen.feedback.BuildConfig

/**
 * 文件资源 URL 处理工具：
 *  - 后端 /file/upload 现在返回相对路径（"/file/{uuid}"），客户端按 BASE_URL 拼接
 *  - 兼容旧数据中存的 "http://localhost:9091/file/{uuid}"（替换 host 为 BASE_URL）
 */
object FileUrlUtils {

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val base = BuildConfig.BASE_URL.trimEnd('/')
        return when {
            raw.startsWith("http://localhost", ignoreCase = true) ||
            raw.startsWith("https://localhost", ignoreCase = true) ||
            raw.startsWith("http://127.0.0.1", ignoreCase = true) ||
            raw.startsWith("https://127.0.0.1", ignoreCase = true) -> {
                // 替换 host 为客户端可访问的 BASE_URL 主机
                val pathStart = raw.indexOf('/', raw.indexOf("://") + 3)
                val path = if (pathStart >= 0) raw.substring(pathStart) else "/"
                base + path
            }
            raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true) -> raw
            raw.startsWith("/") -> base + raw
            else -> "$base/$raw"
        }
    }

    /** 用系统应用打开（图片/PDF 内联预览，视频走播放器） */
    fun openInExternal(context: Context, rawUrl: String?, fileType: String? = null) {
        val url = normalize(rawUrl)
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "文件链接无效", Toast.LENGTH_SHORT).show()
            return
        }
        val mime = mimeOf(fileType, url)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (mime != null) setDataAndType(Uri.parse(url), mime)
                else data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 没有应用能处理此类型 — 退化为浏览器打开
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                Toast.makeText(context, "未找到可以打开此类型文件的应用", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "打开失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mimeOf(ext: String?, url: String): String? {
        val realExt = ext?.lowercase()
            ?: url.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() }
            ?: return null
        return when (realExt) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(realExt)
        }
    }
}

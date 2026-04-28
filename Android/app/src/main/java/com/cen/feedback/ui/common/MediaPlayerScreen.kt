package com.cen.feedback.ui.common

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cen.feedback.R
import com.cen.feedback.ui.components.GradientTopBar
import com.cen.feedback.ui.components.SecondaryButton

/**
 * 课程资源媒体播放器（需求 13）。
 *
 * 使用系统 [VideoView]，不引入第三方依赖。当播放失败（如格式不支持或网络错误）时，
 * 降级为"点击下载"回退按钮，通过系统 intent 打开浏览器下载。
 */
@Composable
fun MediaPlayerScreen(
    url: String,
    title: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var errored by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        GradientTopBar(
            title = title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.media_title),
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (!errored) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener {
                                loading = false
                                it.isLooping = false
                                start()
                            }
                            setOnErrorListener { _, _, _ ->
                                errored = true
                                loading = false
                                true
                            }
                            setVideoURI(Uri.parse(url))
                        }
                    },
                )
                if (loading) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline, null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        stringResource(R.string.media_cannot_play),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.media_open_download),
                        icon = Icons.Rounded.Download,
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** 判断一条资源 url/filename 是否为可内部播放的媒体类型。 */
fun String?.isPlayableMedia(): Boolean {
    if (this.isNullOrBlank()) return false
    val lower = this.lowercase()
    return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") ||
        lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".wav") ||
        lower.endsWith(".webm") || lower.endsWith(".aac")
}

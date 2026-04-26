package com.cen.feedback.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cen.feedback.ui.components.EmptyState
import com.cen.feedback.ui.components.GradientTopBar

/**
 * "我的收藏"页占位实现：
 *   后端尚未提供 bookmark 表，先以空状态 + 文案提示为主，
 *   未来接入 /bookmarks 即可填充列表。
 */
@Composable
fun BookmarksScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GradientTopBar(title = "我的收藏", onBack = onBack)
        EmptyState(
            icon = Icons.Rounded.Bookmark,
            title = "尚未收藏任何内容",
            subtitle = "课程详情、问答帖详情中长按或点击收藏后会出现在这里。",
        )
    }
}

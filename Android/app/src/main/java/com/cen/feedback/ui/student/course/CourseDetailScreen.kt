package com.cen.feedback.ui.student.course

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cen.feedback.data.model.*
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.common.isPlayableMedia
import com.cen.feedback.ui.theme.*
import com.cen.feedback.util.FileUrlUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private enum class CourseTab(val title: String) {
    Overview("概览"), Resources("资料"), Assignments("作业"),
    Questionnaires("问卷"), Qa("问答"), Reviews("课评"),
}

@Composable
fun CourseDetailScreen(
    courseId: Long,
    navController: NavController,
    vm: CourseDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId) { vm.load(courseId) }
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(CourseTab.Overview) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CourseHeader(s.course, onBack = { navController.popBackStack() }) {
                navController.navigate(Routes.rateTeacher(courseId, s.course?.teacherId ?: 0L))
            }
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary600,
                indicator = { positions ->
                    if (tab.ordinal < positions.size) {
                        val pos = positions[tab.ordinal]
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(pos)
                                .height(32.dp)
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Primary100)
                        )
                    }
                },
                divider = { HorizontalDivider() },
            ) {
                CourseTab.values().forEach {
                    Tab(
                        selected = tab == it,
                        onClick = { tab = it },
                        text = {
                            Text(
                                it.title,
                                color = if (tab == it) Primary700
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (tab == it) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(220),
                        ) { it / 6 } + fadeIn()) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(220),
                            ) { -it / 6 } + fadeOut())
                    } else {
                        (androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.tween(220),
                        ) { -it / 6 } + fadeIn()) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(220),
                            ) { it / 6 } + fadeOut())
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "course-tab",
            ) { current ->
                when (current) {
                    CourseTab.Overview -> OverviewTab(s, navController, courseId)
                    CourseTab.Resources -> ResourcesTab(s, vm, navController)
                    CourseTab.Assignments -> AssignmentsTab(s, navController)
                    CourseTab.Questionnaires -> QuestionnairesTab(s, navController, courseId)
                    CourseTab.Qa -> QaTab(courseId, navController)
                    CourseTab.Reviews -> ReviewsTab(s, vm)
                }
            }
        }
        LoadingOverlay(visible = s.loading || s.sending)
    }

    if (s.message != null) {
        LaunchedEffect(s.message) { vm.consumeMessage() }
    }
}

@Composable
private fun CourseHeader(course: Courses?, onBack: () -> Unit, onRateTeacher: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Primary600, Primary400)))
    ) {
        Column(modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onRateTeacher,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.RateReview, null)
                    Spacer(Modifier.width(4.dp))
                    Text("评价老师")
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    course?.name ?: "课程",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(
                        course?.code,
                        course?.academicYear,
                        course?.semester?.let { "第 $it 学期" },
                        course?.location,
                    ).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!course?.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        course?.description.orEmpty(),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/* === 概览 === */
@Composable
private fun OverviewTab(s: CourseDetailUi, navController: NavController, courseId: Long) {
    val ctx = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Description, label = "课程资料",
                    value = s.resources.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Assignment, label = "作业",
                    value = s.assignments.size.toString(),
                    accent = Accent500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Quiz, label = "课程问卷",
                    value = s.questionnaires.size.toString(),
                    accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.QuestionAnswer, label = "讨论",
                    value = s.posts.size.toString(),
                    accent = Success500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { SectionTitle("最近资料") }
        if (s.resources.isEmpty()) item { EmptyState(title = "尚无资料") }
        else items(s.resources.take(3)) { r ->
            ResourceRow(r, onClick = {
                FileUrlUtils.openInExternal(ctx, r.fileUrl, r.fileType)
            })
        }

        item { SectionTitle("最近作业") }
        if (s.assignments.isEmpty()) item { EmptyState(title = "暂无作业") }
        else items(s.assignments.take(3)) { a ->
            AssignmentRow(a) { navController.navigate(Routes.assignmentDetail(a.id ?: 0L)) }
        }
    }
}

/* === 资料 === */
@Composable
private fun ResourcesTab(s: CourseDetailUi, vm: CourseDetailViewModel, navController: NavController) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // 大文件复制移到 IO 协程，避免主线程阻塞 / 大视频闪退
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val name = queryDisplayName(ctx, uri) ?: "upload-${System.currentTimeMillis()}"
                    val tmp = File(ctx.cacheDir, name)
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tmp).use { output -> input.copyTo(output, bufferSize = 64 * 1024) }
                    } ?: throw IllegalStateException("无法读取所选文件")
                    vm.uploadResource(ctx, tmp, name, "lecture")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                "上传资料",
                onClick = { launcher.launch("*/*") },
                icon = Icons.Rounded.UploadFile,
                modifier = Modifier.weight(1f),
            )
        }
        if (s.resources.isEmpty()) {
            EmptyState(title = "还没有资料", subtitle = "上传 PPT、PDF、视频、代码 等任意文件",
                modifier = Modifier.padding(top = 32.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(s.resources) { r ->
                    ResourceRowWithPlay(
                        r = r,
                        navController = navController,
                        onOpenExternal = {
                            vm.markDownload(r.id)
                            FileUrlUtils.openInExternal(ctx, r.fileUrl, r.fileType)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(r: CourseResource, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when ((r.fileType ?: "").lowercase()) {
                        "pdf" -> Icons.Rounded.PictureAsPdf
                        "mp4", "mov" -> Icons.Rounded.PlayCircle
                        "ppt", "pptx" -> Icons.Rounded.Slideshow
                        "doc", "docx" -> Icons.Rounded.Description
                        else -> Icons.Rounded.InsertDriveFile
                    }, null, tint = Primary600
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(r.title ?: r.fileName ?: "未命名", fontWeight = FontWeight.SemiBold)
                Text(
                    "${r.fileType ?: ""} · ${(r.fileSize ?: 0) / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(r.category ?: "other", color = Accent500)
        }
    }
}

@Composable
private fun ResourceRowWithPlay(
    r: CourseResource,
    navController: NavController,
    onOpenExternal: () -> Unit,
) {
    val playable = remember(r.fileUrl, r.fileName, r.fileType) {
        r.fileUrl.isPlayableMedia() || r.fileName.isPlayableMedia()
            || (r.fileType?.lowercase() in setOf("mp4", "mov", "mkv", "mp3", "m4a", "wav", "webm"))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        // 整行点击：媒体文件走内置播放器，其余走外部应用打开
        onClick = {
            if (playable && !r.fileUrl.isNullOrBlank()) {
                navController.navigate(
                    Routes.mediaPlayer(url = r.fileUrl, title = r.title ?: r.fileName)
                )
            } else {
                onOpenExternal()
            }
        },
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when ((r.fileType ?: "").lowercase()) {
                        "pdf" -> Icons.Rounded.PictureAsPdf
                        "mp4", "mov", "mkv", "webm" -> Icons.Rounded.PlayCircle
                        "mp3", "m4a", "wav", "aac" -> Icons.Rounded.Audiotrack
                        "ppt", "pptx" -> Icons.Rounded.Slideshow
                        "doc", "docx" -> Icons.Rounded.Description
                        else -> Icons.Rounded.InsertDriveFile
                    }, null, tint = Primary600
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(r.title ?: r.fileName ?: "未命名", fontWeight = FontWeight.SemiBold)
                Text(
                    "${r.fileType ?: ""} · ${(r.fileSize ?: 0) / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (playable && !r.fileUrl.isNullOrBlank()) {
                IconButton(onClick = {
                    navController.navigate(
                        Routes.mediaPlayer(
                            url = r.fileUrl,
                            title = r.title ?: r.fileName,
                        )
                    )
                }) {
                    Icon(
                        Icons.Rounded.PlayCircleFilled, null,
                        tint = Primary600,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            StatusChip(r.category ?: "other", color = Accent500)
        }
    }
}

/* === 作业 === */
@Composable
private fun AssignmentsTab(s: CourseDetailUi, navController: NavController) {
    if (s.assignments.isEmpty()) {
        EmptyState(title = "尚无作业", subtitle = "等待老师布置")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(s.assignments) { a ->
            AssignmentRow(a) { navController.navigate(Routes.assignmentDetail(a.id ?: 0L)) }
        }
    }
}

@Composable
fun AssignmentRow(a: Assignment, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assignment, null, tint = Accent500, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(a.title ?: "未命名作业", fontWeight = FontWeight.SemiBold)
                Text(
                    "截止：${a.deadline ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(
                when (a.status) { 0 -> "未发布"; 1 -> "进行中"; 2 -> "已结束"; else -> "—" },
                color = when (a.status) { 1 -> Success500; 2 -> Slate600; else -> Warning500 }
            )
        }
    }
}

/* === 问卷 === */
@Composable
private fun QuestionnairesTab(s: CourseDetailUi, navController: NavController, courseId: Long) {
    if (s.questionnaires.isEmpty()) {
        EmptyState(title = "暂无问卷", subtitle = "老师还没有为这门课发布问卷")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(s.questionnaires) { q ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                onClick = {
                    val qId = q.questionnaire?.id ?: return@Surface
                    navController.navigate(Routes.questionnaireFill(courseId, qId))
                },
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Quiz, null, tint = Pink500)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(q.questionnaire?.title ?: "—", fontWeight = FontWeight.SemiBold)
                        Text(
                            q.questionnaire?.description.orEmpty().take(60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusChip(
                        q.statusDescription ?: "—",
                        color = if (q.status == 1) Success500 else Slate600,
                    )
                }
            }
        }
    }
}

/* === 问答 === */
@Composable
private fun QaTab(courseId: Long, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("打开课程问答区，发表问题或回答同学", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            "前往问答区",
            icon = Icons.Rounded.QuestionAnswer,
            onClick = { navController.navigate(Routes.courseQa(courseId)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/* === 课评 === */
@Composable
private fun ReviewsTab(s: CourseDetailUi, vm: CourseDetailViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Text("我对这门课的评价", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                StarRatingBar(
                    rating = s.myFeedbackRating,
                    onChange = vm::setMyFeedbackRating,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = s.myFeedbackContent,
                    onValueChange = vm::setMyFeedbackContent,
                    placeholder = { Text("说说你对课程内容、节奏、收获的真实感受…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                InlineError(s.error)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    "提交评价",
                    onClick = vm::submitFeedback,
                    icon = Icons.Rounded.Send,
                    loading = s.sending,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { SectionTitle("同学们的评价") }
        if (s.feedbacks.isEmpty()) item { EmptyState(title = "暂无课评") }
        else items(s.feedbacks) { dto ->
            val fb = dto.feedback ?: return@items
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // 学生姓名匿名显示
                            "同学 #${(dto.student?.id ?: 0L) % 1000}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        StarRatingBar(rating = fb.rating ?: 0, onChange = {}, enabled = false,
                            starSize = 18.dp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(fb.content, style = MaterialTheme.typography.bodyMedium)
                    if (fb.createdAt != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(fb.createdAt, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(ctx: android.content.Context, uri: Uri): String? {
    val p = ctx.contentResolver.query(uri, null, null, null, null) ?: return null
    return p.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
    }
}

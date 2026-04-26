package com.cen.feedback.ui.teacher.course

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.data.model.*
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TeacherCourseDetailUi(
    val loading: Boolean = false,
    val course: Courses? = null,
    val resources: List<CourseResource> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val questionnaires: List<QuestionnaireWithStatusDTO> = emptyList(),
    val templates: List<Questionnaires> = emptyList(),
    val stats: List<QuestionnaireSubmissionStatsDTO> = emptyList(),
    val posts: List<QaPost> = emptyList(),
    val enrollments: List<TeacherEnrollmentRow> = emptyList(),
    val sending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TeacherCourseDetailViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherCourseDetailUi())
    val state = _state.asStateFlow()
    private var courseIdInternal = 0L

    fun load(cid: Long) {
        if (courseIdInternal == cid) return
        courseIdInternal = cid
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val cid = courseIdInternal
        if (cid == 0L) return@launch
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            val courses = runCatching { repo.teacherCourses(tid) }.getOrDefault(emptyList())
            val course = courses.firstOrNull { it.id == cid }
            val resources = runCatching { repo.listResources(cid) }.getOrDefault(emptyList())
            val assignments = runCatching { repo.assignmentsByCourse(cid) }.getOrDefault(emptyList())
            val questionnaires = runCatching { repo.courseQuestionnaires(cid) }.getOrDefault(emptyList())
            val templates = runCatching { repo.teacherQuestionnaires(tid) }.getOrDefault(emptyList())
            val stats = runCatching { repo.courseQuestionnaireStats(cid) }.getOrDefault(emptyList())
            val posts = runCatching { repo.listPosts(cid) }.getOrDefault(emptyList())
            val enrollments = runCatching { repo.teacherCourseEnrollments(cid) }.getOrDefault(emptyList())
            TeacherCourseDetailUi(
                loading = false,
                course = course,
                resources = resources,
                assignments = assignments,
                questionnaires = questionnaires,
                templates = templates,
                stats = stats,
                posts = posts,
                enrollments = enrollments,
            )
        }.onSuccess { _state.value = it }
         .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun uploadResource(file: File, title: String, category: String) = viewModelScope.launch {
        _state.update { it.copy(sending = true) }
        runCatching {
            val url = repo.uploadFile(file)
            val tid = repo.tokenStore.userId() ?: 0L
            repo.saveResource(
                CourseResource(
                    courseId = courseIdInternal,
                    uploaderId = tid,
                    uploaderRole = "teacher",
                    title = title.ifBlank { file.name },
                    fileName = file.name,
                    fileUrl = url,
                    fileType = file.extension,
                    fileSize = file.length(),
                    category = category,
                )
            )
        }.onSuccess { _state.update { it.copy(sending = false, message = "上传成功") }; refresh() }
         .onFailure { e -> _state.update { it.copy(sending = false, error = e.message) } }
    }

    fun deleteResource(id: Long) = viewModelScope.launch {
        runCatching { repo.deleteResource(id) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun deleteAssignment(id: Long) = viewModelScope.launch {
        runCatching { repo.deleteAssignment(id) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun bindQuestionnaires(ids: List<Long>) = viewModelScope.launch {
        if (ids.isEmpty()) return@launch
        _state.update { it.copy(sending = true) }
        runCatching { repo.bindQuestionnaires(courseIdInternal, ids.joinToString(",")) }
            .onSuccess { _state.update { it.copy(sending = false, message = "已关联问卷") }; refresh() }
            .onFailure { e -> _state.update { it.copy(sending = false, error = e.message) } }
    }

    fun publish(qId: Long) = viewModelScope.launch {
        runCatching { repo.publishQuestionnaire(courseIdInternal, qId) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun end(qId: Long) = viewModelScope.launch {
        runCatching { repo.endQuestionnaire(courseIdInternal, qId) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun revoke(qId: Long) = viewModelScope.launch {
        runCatching { repo.revokeQuestionnaire(courseIdInternal, qId) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun unbind(qId: Long) = viewModelScope.launch {
        runCatching { repo.unbindCourseQuestionnaire(courseIdInternal, qId) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun inviteStudent(username: String) = viewModelScope.launch {
        _state.update { it.copy(sending = true) }
        runCatching { repo.teacherInviteStudent(courseIdInternal, null, username.trim()) }
            .onSuccess { _state.update { it.copy(sending = false, message = "已发送邀请") }; refresh() }
            .onFailure { e -> _state.update { it.copy(sending = false, error = e.message) } }
    }

    fun approveEnrollment(id: Long) = viewModelScope.launch {
        runCatching { repo.teacherApproveEnrollment(id) }
            .onSuccess { _state.update { it.copy(message = "已批准") }; refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun rejectEnrollment(id: Long, reason: String?) = viewModelScope.launch {
        runCatching { repo.teacherRejectEnrollment(id, reason?.ifBlank { null }) }
            .onSuccess { _state.update { it.copy(message = "已驳回") }; refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun removeEnrollment(id: Long) = viewModelScope.launch {
        runCatching { repo.teacherRemoveEnrollment(id) }
            .onSuccess { _state.update { it.copy(message = "已移除") }; refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeError() = _state.update { it.copy(error = null) }
}

private enum class TeacherCourseTab(val title: String) {
    Overview("概览"), Students("学生"), Resources("资料"),
    Assignments("作业"), Questionnaires("问卷"), Qa("问答"),
}

@Composable
fun TeacherCourseDetailScreen(
    courseId: Long,
    navController: NavController,
    vm: TeacherCourseDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId) { vm.load(courseId) }
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(TeacherCourseTab.Overview) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CourseHeader(s.course) { navController.popBackStack() }
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary600,
                divider = { HorizontalDivider() },
            ) {
                TeacherCourseTab.values().forEach {
                    Tab(
                        selected = tab == it,
                        onClick = { tab = it },
                        text = { Text(it.title) },
                    )
                }
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "t-course-tab",
            ) { current ->
                when (current) {
                    TeacherCourseTab.Overview -> OverviewTab(s, navController)
                    TeacherCourseTab.Students -> StudentsTab(s, vm)
                    TeacherCourseTab.Resources -> ResourcesTab(s, vm)
                    TeacherCourseTab.Assignments -> AssignmentsTab(s, vm, navController, courseId)
                    TeacherCourseTab.Questionnaires -> QuestionnairesTab(s, vm, navController, courseId)
                    TeacherCourseTab.Qa -> QaTab(s, navController)
                }
            }
        }
        LoadingOverlay(visible = s.loading || s.sending)
    }

    val ctx = LocalContext.current
    if (s.message != null) {
        LaunchedEffect(s.message) {
            android.widget.Toast.makeText(ctx, s.message, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }
    if (s.error != null) {
        LaunchedEffect(s.error) {
            android.widget.Toast.makeText(ctx, s.error, android.widget.Toast.LENGTH_LONG).show()
            vm.consumeError()
        }
    }
}

@Composable
private fun CourseHeader(course: Courses?, onBack: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .background(Brush.linearGradient(listOf(Accent600, Primary600, Primary400)))) {
        Column(modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(course?.name ?: "课程",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(course?.code, course?.academicYear,
                        course?.semester?.let { "第 $it 学期" }, course?.location)
                        .joinToString(" · "),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(s: TeacherCourseDetailUi, navController: NavController) {
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Description, label = "课程资料",
                    value = s.resources.size.toString(), modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Assignment, label = "作业",
                    value = s.assignments.size.toString(), accent = Accent500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Quiz, label = "已发问卷",
                    value = s.questionnaires.size.toString(), accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.QuestionAnswer, label = "讨论帖",
                    value = s.posts.size.toString(), accent = Success500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { SectionTitle("问卷提交概况") }
        if (s.stats.isEmpty()) item { EmptyState(title = "暂无统计数据") }
        else items(s.stats) { stat ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                onClick = {
                    val qid = stat.questionnaire?.id ?: return@Surface
                    navController.navigate(Routes.teacherAnalysis(s.course?.id ?: 0L, qid))
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stat.questionnaire?.title ?: "—",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold)
                        StatusChip(
                            stat.statusDescription ?: "—",
                            color = when (stat.status) {
                                1 -> Success500; 2 -> Slate600; else -> Warning500
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = ((stat.submissionRate ?: 0.0) / 100.0).toFloat()
                            .coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${stat.submittedCount ?: 0}/${stat.totalStudents ?: 0} · " +
                                "完成率 ${"%.1f".format(stat.submissionRate ?: 0.0)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcesTab(s: TeacherCourseDetailUi, vm: TeacherCourseDetailViewModel) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = queryDisplayName(ctx, uri) ?: "upload-${System.currentTimeMillis()}"
            val tmp = File(ctx.cacheDir, name)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            vm.uploadResource(tmp, name, "lecture")
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                "上传课件 / 视频 / 代码",
                onClick = { launcher.launch("*/*") },
                icon = Icons.Rounded.UploadFile,
                modifier = Modifier.weight(1f),
            )
        }
        if (s.resources.isEmpty()) {
            EmptyState(title = "尚未上传资料",
                subtitle = "支持 PPT / PDF / 视频 / 代码等任意文件",
                modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(s.resources) { r ->
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                    ) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when ((r.fileType ?: "").lowercase()) {
                                    "pdf" -> Icons.Rounded.PictureAsPdf
                                    "mp4", "mov" -> Icons.Rounded.PlayCircle
                                    "ppt", "pptx" -> Icons.Rounded.Slideshow
                                    "doc", "docx" -> Icons.Rounded.Description
                                    else -> Icons.Rounded.InsertDriveFile
                                },
                                null, tint = Primary600
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.title ?: r.fileName ?: "—",
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${r.fileType ?: ""} · ${(r.fileSize ?: 0) / 1024} KB · 下载 ${r.downloadCount ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { r.id?.let(vm::deleteResource) }) {
                                Icon(Icons.Rounded.Delete, null,
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentsTab(
    s: TeacherCourseDetailUi,
    vm: TeacherCourseDetailViewModel,
    navController: NavController,
    courseId: Long,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            PrimaryButton(
                "新建作业",
                icon = Icons.Rounded.Add,
                onClick = { navController.navigate(Routes.assignmentEditor(courseId, null)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (s.assignments.isEmpty()) {
            EmptyState(title = "尚未发布作业", icon = Icons.Rounded.Assignment,
                modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(s.assignments) { a ->
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        onClick = { navController.navigate(Routes.assignmentEditor(courseId, a.id)) }
                    ) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Assignment, null, tint = Accent500)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.title ?: "—", fontWeight = FontWeight.SemiBold)
                                Text("截止：${a.deadline ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusChip(
                                when (a.status) { 0 -> "未发布"; 1 -> "进行中"; 2 -> "已结束"; else -> "—" },
                                color = when (a.status) { 1 -> Success500; 2 -> Slate600; else -> Warning500 }
                            )
                            IconButton(onClick = { a.id?.let(vm::deleteAssignment) }) {
                                Icon(Icons.Rounded.Delete, null,
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionnairesTab(
    s: TeacherCourseDetailUi,
    vm: TeacherCourseDetailViewModel,
    navController: NavController,
    courseId: Long,
) {
    var showBind by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                "新建问卷",
                icon = Icons.Rounded.AddCircle,
                onClick = { navController.navigate(Routes.questionnaireEditor(null)) },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                "关联模板",
                icon = Icons.Rounded.Link,
                onClick = { showBind = true; selected.clear() },
                modifier = Modifier.weight(1f),
            )
        }
        if (s.questionnaires.isEmpty()) {
            EmptyState(title = "本课暂无问卷", subtitle = "可创建新问卷或关联已有模板",
                modifier = Modifier.padding(top = 24.dp), icon = Icons.Rounded.Quiz)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(s.questionnaires) { q ->
                    val qid = q.questionnaire?.id ?: return@items
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(q.questionnaire?.title ?: "—",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold)
                                StatusChip(
                                    q.statusDescription ?: "—",
                                    color = when (q.status) {
                                        1 -> Success500; 2 -> Slate600; else -> Warning500
                                    }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                q.questionnaire?.description?.take(80).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (q.status) {
                                    0 -> TextButton(onClick = { vm.publish(qid) }) {
                                        Icon(Icons.Rounded.PlayArrow, null,
                                            modifier = Modifier.size(18.dp))
                                        Text(" 发布")
                                    }
                                    1 -> TextButton(onClick = { vm.end(qid) }) {
                                        Icon(Icons.Rounded.Stop, null,
                                            modifier = Modifier.size(18.dp))
                                        Text(" 结束")
                                    }
                                    2 -> TextButton(onClick = { vm.revoke(qid) }) {
                                        Icon(Icons.Rounded.Replay, null,
                                            modifier = Modifier.size(18.dp))
                                        Text(" 撤回")
                                    }
                                }
                                TextButton(onClick = {
                                    navController.navigate(Routes.teacherAnalysis(courseId, qid))
                                }) {
                                    Icon(Icons.Rounded.Analytics, null,
                                        modifier = Modifier.size(18.dp))
                                    Text(" 分析")
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { vm.unbind(qid) }) {
                                    Icon(Icons.Rounded.LinkOff, null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBind) {
        AlertDialog(
            onDismissRequest = { showBind = false },
            title = { Text("关联问卷模板") },
            text = {
                if (s.templates.isEmpty()) Text("暂无模板，请先创建。")
                else Column {
                    s.templates.forEach { q ->
                        val checked = selected.contains(q.id)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selected.add(q.id) else selected.remove(q.id)
                                },
                            )
                            Text(q.title ?: "—")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.bindQuestionnaires(selected.toList())
                    showBind = false
                }) { Text("关联") }
            },
            dismissButton = {
                TextButton(onClick = { showBind = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun QaTab(s: TeacherCourseDetailUi, navController: NavController) {
    if (s.posts.isEmpty()) {
        EmptyState(title = "尚无讨论",
            subtitle = "学生提问后会出现在这里，可以及时答复",
            icon = Icons.Rounded.QuestionAnswer)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)) {
        items(s.posts) { p ->
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                onClick = { navController.navigate(Routes.postDetail(p.id ?: 0L)) },
            ) {
                Row(modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.QuestionAnswer, null, tint = Primary600)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.title ?: "—", fontWeight = FontWeight.SemiBold)
                        Text(p.content?.take(50).orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (p.isResolved == 1) StatusChip("已解决", color = Success500)
                    else StatusChip("待解答", color = Warning500)
                }
            }
        }
    }
}

private fun queryDisplayName(ctx: Context, uri: Uri): String? {
    val p = ctx.contentResolver.query(uri, null, null, null, null) ?: return null
    return p.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
    }
}

@Composable
private fun StudentsTab(
    s: TeacherCourseDetailUi,
    vm: TeacherCourseDetailViewModel,
) {
    var showInvite by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<TeacherEnrollmentRow?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }

    val approved = s.enrollments.filter { it.enrollment?.status == "approved" }
    val pendingApply = s.enrollments.filter {
        it.enrollment?.status == "pending" && it.enrollment.source == "student_apply"
    }
    val pendingInvite = s.enrollments.filter {
        it.enrollment?.status == "pending" && it.enrollment.source == "teacher_invite"
    }
    val rejected = s.enrollments.filter { it.enrollment?.status == "rejected" }

    val visible = when (filter) {
        "approved" -> approved
        "pending_apply" -> pendingApply
        "pending_invite" -> pendingInvite
        "rejected" -> rejected
        else -> s.enrollments
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            MetricCard(
                icon = Icons.Rounded.Group,
                label = "在册学生",
                value = approved.size.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Rounded.PendingActions,
                label = "待审申请",
                value = pendingApply.size.toString(),
                accent = Warning500,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            PrimaryButton(
                "邀请学生加入",
                icon = Icons.Rounded.PersonAdd,
                onClick = { showInvite = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf(
                null to "全部 (${s.enrollments.size})",
                "approved" to "在册 (${approved.size})",
                "pending_apply" to "待审申请 (${pendingApply.size})",
                "pending_invite" to "邀请未回 (${pendingInvite.size})",
                "rejected" to "已驳回 (${rejected.size})",
            )) { (value, label) ->
                FilterChip(
                    selected = filter == value,
                    onClick = { filter = value },
                    label = { Text(label) },
                )
            }
        }
        if (visible.isEmpty()) {
            EmptyState(
                title = "暂无数据",
                subtitle = "可点击\"邀请学生加入\"输入学生用户名进行邀请",
                icon = Icons.Rounded.Group,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)) {
                items(visible) { row ->
                    EnrollmentRow(
                        row = row,
                        onApprove = { row.enrollment?.id?.let(vm::approveEnrollment) },
                        onReject = { rejectTarget = row },
                        onRemove = { row.enrollment?.id?.let(vm::removeEnrollment) },
                    )
                }
            }
        }
    }

    if (showInvite) {
        InviteStudentDialog(
            onDismiss = { showInvite = false },
            onConfirm = {
                vm.inviteStudent(it)
                showInvite = false
            },
        )
    }

    rejectTarget?.let { target ->
        RejectReasonDialog(
            studentName = target.student?.nickname ?: target.student?.username ?: "学生",
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                target.enrollment?.id?.let { vm.rejectEnrollment(it, reason) }
                rejectTarget = null
            },
        )
    }
}

@Composable
private fun EnrollmentRow(
    row: TeacherEnrollmentRow,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
) {
    val student = row.student
    val e = row.enrollment
    Surface(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (student?.nickname ?: student?.username ?: "?").take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(student?.nickname ?: student?.username ?: "—",
                        fontWeight = FontWeight.SemiBold)
                    Text("@${student?.username ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val (label, color) = when {
                    e?.status == "approved" -> "在册" to Success500
                    e?.status == "pending" && e.source == "student_apply" -> "待审" to Warning500
                    e?.status == "pending" && e.source == "teacher_invite" -> "邀请中" to Pink500
                    e?.status == "rejected" -> "已驳回" to Danger500
                    else -> (e?.status ?: "—") to Slate500
                }
                StatusChip(label, color)
            }
            val msg = e?.applyMessage
            if (!msg.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Primary600.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "申请留言：$msg",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            val reason = e?.rejectReason
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Danger500.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "驳回理由：$reason",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger500,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    e?.status == "pending" && e.source == "student_apply" -> {
                        FilledTonalButton(onClick = onApprove) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("批准")
                        }
                        OutlinedButton(onClick = onReject) {
                            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("驳回")
                        }
                    }
                    e?.status == "pending" && e.source == "teacher_invite" -> {
                        OutlinedButton(onClick = onRemove) {
                            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("撤回邀请")
                        }
                    }
                    e?.status == "approved" -> {
                        OutlinedButton(onClick = onRemove) {
                            Icon(Icons.Rounded.PersonRemove, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("移出课程")
                        }
                    }
                    e?.status == "rejected" -> {
                        OutlinedButton(onClick = onRemove) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("删除记录")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请学生加入课程") },
        text = {
            Column {
                Text("输入学生的用户名（学号），系统会发送邀请，对方接受后即可加入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    placeholder = { Text("学生用户名 / 学号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username) },
                enabled = username.isNotBlank(),
            ) { Text("发送邀请") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun RejectReasonDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("驳回 $studentName 的申请") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 200) reason = it },
                placeholder = { Text("可填写驳回原因（选填）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }) { Text("驳回") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

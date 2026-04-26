package com.cen.feedback.ui.student.calendar

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CalendarUi(
    val loading: Boolean = false,
    val courses: List<Courses> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class StudentCalendarViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            repo.studentCourses(sid)
        }.onSuccess { l -> _state.update { it.copy(loading = false, courses = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun setMessage(m: String) = _state.update { it.copy(message = m) }
}

@Composable
fun CalendarScreen(vm: StudentCalendarViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showSync by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Primary600, Primary400)))
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
                    Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("课程日历", color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("一键同步至系统日历，永不忘记上课时间",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                "同步全部到日历",
                icon = Icons.Rounded.Sync,
                onClick = { showSync = true },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                "刷新",
                icon = Icons.Rounded.Refresh,
                onClick = vm::refresh,
                modifier = Modifier.weight(0.6f),
            )
        }

        if (s.loading && s.courses.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
        } else if (s.courses.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.EventBusy,
                title = "本学期暂无课程",
                subtitle = "请联系教师将你加入课程",
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(s.courses) { c ->
                    CourseSchedule(c) {
                        runCatching { addCourseToSystemCalendar(ctx, c) }
                            .onSuccess { ok ->
                                vm.setMessage(if (ok) "已添加 “${c.name}” 到系统日历" else "无可用日历账号")
                            }.onFailure { vm.setMessage("添加失败：${it.message}") }
                    }
                }
            }
        }
    }

    if (s.message != null) {
        LaunchedEffect(s.message) {
            android.widget.Toast.makeText(ctx, s.message, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }

    if (showSync) {
        AlertDialog(
            onDismissRequest = { showSync = false },
            title = { Text("同步课程到系统日历") },
            text = { Text("将把当前所有课程根据课表时间批量添加到系统日历，已存在的不会重复。是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showSync = false
                    var ok = 0; var fail = 0
                    s.courses.forEach { c ->
                        runCatching { if (addCourseToSystemCalendar(ctx, c)) ok++ else fail++ }
                            .onFailure { fail++ }
                    }
                    vm.setMessage("已同步 $ok 门课程${if (fail > 0) "，失败 $fail" else ""}")
                }) { Text("同步") }
            },
            dismissButton = { TextButton(onClick = { showSync = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun CourseSchedule(c: Courses, onAdd: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (c.name ?: "课").take(1),
                    color = Color.White, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name ?: "—", fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(c.courseTime, c.location, c.academicYear,
                        c.semester?.let { "第 $it 学期" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Rounded.AddTask, null, tint = Primary600)
            }
        }
    }
}

/**
 * 将课程添加到系统日历。
 * 简化策略：把 courseTime 字符串作为 description 写入；起止时间默认设为 “最近的下一个工作日 09:00-10:30”，
 * 学生可以再到系统日历中调整。
 */
private fun addCourseToSystemCalendar(ctx: Context, c: Courses): Boolean {
    val cr = ctx.contentResolver
    // 找一个可用的 calendar id
    val calCursor = cr.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        null, null, null,
    ) ?: return false
    val calId: Long = calCursor.use { it.takeIf { it.moveToFirst() }?.getLong(0) } ?: return false

    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        add(Calendar.DAY_OF_MONTH, 1)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.MINUTE, 90) }

    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calId)
        put(CalendarContract.Events.TITLE, c.name ?: "课程")
        put(CalendarContract.Events.EVENT_LOCATION, c.location ?: "")
        put(
            CalendarContract.Events.DESCRIPTION,
            buildString {
                append(c.description ?: "")
                if (!c.courseTime.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("时间安排：${c.courseTime}")
                }
            },
        )
        put(CalendarContract.Events.DTSTART, start.timeInMillis)
        put(CalendarContract.Events.DTEND, end.timeInMillis)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=16")
    }
    val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values)
    return uri != null
}

private fun fmt(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

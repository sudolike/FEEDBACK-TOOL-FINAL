package com.cen.feedback.ui.teacher.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.Questionnaires
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.teacher.TeacherTab
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUi(
    val loading: Boolean = false,
    val nickname: String = "",
    val courses: List<Courses> = emptyList(),
    val questionnaires: List<Questionnaires> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherHomeUi())
    val state = _state.asStateFlow()
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            val nick = repo.tokenStore.username() ?: "老师"
            val courses = runCatching { repo.teacherCourses(tid) }.getOrDefault(emptyList())
            val qs = runCatching { repo.teacherQuestionnaires(tid) }.getOrDefault(emptyList())
            Triple(nick, courses, qs)
        }.onSuccess { v ->
            _state.update { it.copy(loading = false, nickname = v.first,
                courses = v.second, questionnaires = v.third) }
        }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun TeacherHomeScreen(
    navController: NavController,
    onJumpTab: (TeacherTab) -> Unit,
    vm: TeacherHomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    // 每次进入 Dashboard tab（包括从 Surveys / Courses 切换回来）都拉一次最新数据
    LaunchedEffect(Unit) { vm.refresh() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            HeaderHero(
                nickname = s.nickname,
                courseCount = s.courses.size,
                qCount = s.questionnaires.size,
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.Class,
                    label = "我的课程",
                    value = s.courses.size.toString(),
                    accent = Primary600,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    icon = Icons.Rounded.Quiz,
                    label = "问卷模板",
                    value = s.questionnaires.size.toString(),
                    accent = Pink500,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionTitle("教学课程", trailing = {
                TextButton(onClick = { onJumpTab(TeacherTab.Courses) }) { Text("管理") }
            })
        }
        if (s.courses.isEmpty()) {
            item {
                EmptyState(title = "尚未维护课程",
                    subtitle = "请联系管理员或在 Web 端创建课程",
                    icon = Icons.Rounded.Class)
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.courses) { c ->
                        TeacherCourseTile(c) {
                            navController.navigate(Routes.teacherCourseDetail(c.id))
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("问卷模板", trailing = {
                TextButton(onClick = { onJumpTab(TeacherTab.Questionnaires) }) { Text("查看") }
            })
        }
        if (s.questionnaires.isEmpty()) {
            item { EmptyState(title = "暂无问卷模板",
                subtitle = "去问卷中心创建第一份模板吧",
                icon = Icons.Rounded.Quiz) }
        } else {
            items(s.questionnaires.take(5)) { q ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    onClick = { navController.navigate(Routes.questionnaireEditor(q.id)) },
                ) {
                    Row(modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Quiz, null, tint = Pink500)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.title ?: "—", fontWeight = FontWeight.SemiBold)
                            Text(
                                q.description?.take(50).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderHero(nickname: String, courseCount: Int, qCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Accent600, Primary600, Primary400))),
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
                ) { Icon(Icons.Rounded.Person, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("欢迎回来，$nickname 老师",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "正在维护 $courseCount 门课程，已有 $qCount 份问卷模板",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI 助教：自动总结问卷答复 / 草拟新题 / 给出教学建议",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherCourseTile(c: Courses, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 130.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Primary600, Primary400))),
                contentAlignment = Alignment.Center,
            ) {
                Text((c.name ?: "课").take(1), color = Color.White,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(c.name ?: "—",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(c.code, c.academicYear,
                    c.semester?.let { "第 $it 学期" }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

package com.cen.feedback.ui.teacher.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.QuestionnaireSubmissionStatsDTO
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisHubUi(
    val loading: Boolean = false,
    val courses: List<Courses> = emptyList(),
    val statsByCourse: Map<Long, List<QuestionnaireSubmissionStatsDTO>> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class AnalysisHubViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AnalysisHubUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            val courses = repo.teacherCourses(tid)
            val map = courses.associate { c ->
                c.id to runCatching { repo.courseQuestionnaireStats(c.id) }
                    .getOrDefault(emptyList())
            }
            courses to map
        }.onSuccess { (cs, m) ->
            _state.update { it.copy(loading = false, courses = cs, statsByCourse = m) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}

@Composable
fun TeacherAnalysisHubScreen(
    navController: NavController,
    vm: AnalysisHubViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(title = "数据分析")
        if (s.loading && s.courses.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
        } else if (s.courses.isEmpty()) {
            EmptyState(title = "暂无可分析数据",
                subtitle = "等问卷有学生提交后再来吧",
                icon = Icons.Rounded.Analytics,
                modifier = Modifier.padding(top = 48.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(s.courses) { c ->
                    val stats = s.statsByCourse[c.id].orEmpty()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Class, null, tint = Primary600)
                                Spacer(Modifier.width(8.dp))
                                Text(c.name ?: "—", fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                                Text(
                                    "${stats.size} 份问卷",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (stats.isEmpty()) {
                                Text("尚无问卷绑定",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                stats.forEach { stat ->
                                    val qid = stat.questionnaire?.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        onClick = {
                                            if (qid != null) {
                                                navController.navigate(Routes.teacherAnalysis(c.id, qid))
                                            }
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row {
                                                Text(stat.questionnaire?.title ?: "—",
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f))
                                                StatusChip(
                                                    stat.statusDescription ?: "—",
                                                    color = when (stat.status) {
                                                        1 -> Success500; 2 -> Slate600
                                                        else -> Warning500
                                                    }
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = ((stat.submissionRate ?: 0.0) / 100.0)
                                                    .toFloat().coerceIn(0f, 1f),
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "${stat.submittedCount ?: 0}/${stat.totalStudents ?: 0} · " +
                                                    "完成率 ${"%.1f".format(stat.submissionRate ?: 0.0)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

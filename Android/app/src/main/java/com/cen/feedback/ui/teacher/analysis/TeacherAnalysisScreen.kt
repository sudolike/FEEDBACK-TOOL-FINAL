package com.cen.feedback.ui.teacher.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cen.feedback.data.model.CourseDashboard
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUi(
    val loading: Boolean = false,
    val analysis: Map<String, Any?>? = null,
    val summary: String? = null,
    val dashboard: CourseDashboard? = null,
    val error: String? = null,
)

@HiltViewModel
class TeacherAnalysisViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AnalysisUi())
    val state = _state.asStateFlow()

    fun load(courseId: Long, qId: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val analysis = runCatching { repo.questionnaireAiAnalysis(courseId, qId) }
                .getOrDefault(emptyMap())
            val summary = runCatching { repo.summarize(courseId, qId) }
                .getOrNull()?.get("summary")?.toString()
            val dashboard = runCatching { repo.courseDashboard(courseId) }.getOrNull()
            Triple(analysis, summary, dashboard)
        }.onSuccess { (a, sum, dash) ->
            _state.update {
                it.copy(loading = false, analysis = a, summary = sum, dashboard = dash)
            }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}

@Composable
fun TeacherAnalysisScreen(
    courseId: Long,
    qId: Long,
    onBack: () -> Unit,
    vm: TeacherAnalysisViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId, qId) { vm.load(courseId, qId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(title = "问卷数据分析", onBack = onBack)
        if (s.loading && s.analysis == null) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards(count = 4) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Primary600, Primary400)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("AI 综合摘要", color = Color.White,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s.summary ?: "暂无总结，待 AI 助手归纳…",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                if (s.dashboard != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(
                                Icons.Rounded.RateReview, "课评条数",
                                s.dashboard!!.totalFeedback?.toString() ?: "0",
                                modifier = Modifier.weight(1f),
                            )
                            MetricCard(
                                Icons.Rounded.Star, "平均评分",
                                "%.2f".format(s.dashboard!!.avgRating ?: 0.0),
                                accent = Warning500,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(
                                Icons.Rounded.Group, "学生总数",
                                s.dashboard!!.totalStudents?.toString() ?: "0",
                                accent = Accent500,
                                modifier = Modifier.weight(1f),
                            )
                            MetricCard(
                                Icons.Rounded.Quiz, "问卷提交",
                                s.dashboard!!.totalQuestionnaireSubmissions?.toString() ?: "0",
                                accent = Pink500,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    val hist = s.dashboard!!.ratingHistogram
                    if (!hist.isNullOrEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("评分分布", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                RatingHistogram(hist)
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("逐题分析", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        val items = (s.analysis?.get("questions") as? List<*>).orEmpty()
                        if (items.isEmpty()) {
                            Text("暂无分析数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        } else {
                            items.forEachIndexed { idx, raw ->
                                @Suppress("UNCHECKED_CAST")
                                val q = raw as? Map<String, Any?> ?: return@forEachIndexed
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        "${idx + 1}. ${q["title"] ?: "—"}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    val type = (q["type"] ?: "text").toString()
                                    Text(
                                        "题型：$type · 提交数：${q["responses"] ?: 0}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    val dist = q["distribution"] as? Map<*, *>
                                    if (!dist.isNullOrEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        dist.forEach { (k, v) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    k.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.width(80.dp),
                                                )
                                                LinearProgressIndicator(
                                                    progress = ((v as? Number)?.toFloat() ?: 0f)
                                                        .coerceAtMost(100f) / 100f,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("${v}",
                                                    style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    val avg = q["average"]
                                    if (avg != null) {
                                        Text("平均：$avg",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    val samples = q["samples"] as? List<*>
                                    if (!samples.isNullOrEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "学生回答示例：",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        samples.take(5).forEach { sample ->
                                            Text(
                                                "· ${sample}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                                            )
                                        }
                                    }
                                    Divider(modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingHistogram(hist: List<Int>) {
    val max = (hist.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        hist.forEachIndexed { i, v ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((100f * v / max).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(Brush.verticalGradient(listOf(Primary400, Primary600)))
                )
                Spacer(Modifier.height(4.dp))
                Text("${i + 1}★", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

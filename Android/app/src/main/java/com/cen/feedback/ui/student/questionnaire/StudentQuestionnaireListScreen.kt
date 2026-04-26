package com.cen.feedback.ui.student.questionnaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.cen.feedback.data.model.QuestionnaireFullInfoDTO
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.nav.Routes
import com.cen.feedback.ui.theme.Pink500
import com.cen.feedback.ui.theme.Slate600
import com.cen.feedback.ui.theme.Success500
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StuQListUi(
    val loading: Boolean = false,
    val list: List<QuestionnaireFullInfoDTO> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class StudentQuestionnaireListViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(StuQListUi())
    val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            repo.studentAllQuestionnaires(sid)
        }
            .onSuccess { l -> _state.update { it.copy(loading = false, list = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun StudentQuestionnaireListScreen(
    navController: NavController,
    vm: StudentQuestionnaireListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    val ongoing = s.list.filter { it.status == 1 }
    val completed = s.list.filter { it.status == 2 }
    val items = if (tab == 0) ongoing else completed

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(title = "我的问卷")
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text("进行中（${ongoing.size}）") })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text("已结束（${completed.size}）") })
        }
        if (s.loading && s.list.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
        } else if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Quiz,
                title = if (tab == 0) "暂无进行中的问卷" else "暂无已结束的问卷",
                modifier = Modifier.padding(top = 48.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(items) { q ->
                    QListRow(q) {
                        val cId = q.course?.id ?: return@QListRow
                        val qId = q.questionnaire?.id ?: return@QListRow
                        navController.navigate(Routes.questionnaireFill(cId, qId))
                    }
                }
            }
        }
    }
}

@Composable
private fun QListRow(q: QuestionnaireFullInfoDTO, onClick: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(q.questionnaire?.title ?: "—", fontWeight = FontWeight.SemiBold)
                Text(
                    "${q.course?.name ?: ""} · ${q.teacher?.nickname ?: q.teacher?.username ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(
                if (q.hasSubmitted == true) "已提交"
                else q.statusDescription ?: "进行中",
                color = when {
                    q.hasSubmitted == true -> Slate600
                    q.status == 1 -> Success500
                    else -> Pink500
                }
            )
        }
    }
}

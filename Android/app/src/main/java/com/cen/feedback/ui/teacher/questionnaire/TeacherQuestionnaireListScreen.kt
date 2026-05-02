package com.cen.feedback.ui.teacher.questionnaire

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
import com.cen.feedback.data.model.Questionnaires
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

data class TeacherQuestionnaireListUi(
    val loading: Boolean = false,
    val list: List<Questionnaires> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TeacherQuestionnaireListViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherQuestionnaireListUi())
    val state = _state.asStateFlow()
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            repo.teacherQuestionnaires(tid)
        }.onSuccess { l -> _state.update { it.copy(loading = false, list = l) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }
    fun delete(q: Questionnaires) = viewModelScope.launch {
        runCatching { repo.deleteQuestionnaire(q) }
            .onSuccess { refresh() }
            .onFailure { e -> _state.update { it.copy(error = e.message) } }
    }
}

@Composable
fun TeacherQuestionnaireListScreen(
    navController: NavController,
    vm: TeacherQuestionnaireListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }
    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "问卷模板",
            actions = {
                IconButton(onClick = { navController.navigate(Routes.questionnaireEditor(null)) }) {
                    Icon(Icons.Rounded.Add, null, tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        )
        if (s.loading && s.list.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
        } else if (s.list.isEmpty()) {
            EmptyState(title = "暂无问卷模板",
                subtitle = "点击右上角 + 创建第一份模板",
                icon = Icons.Rounded.Quiz,
                modifier = Modifier.padding(top = 48.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(s.list) { q ->
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        onClick = { navController.navigate(Routes.questionnaireEditor(q.id)) }
                    ) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Quiz, null, tint = Pink500)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(q.title ?: "—", fontWeight = FontWeight.SemiBold)
                                Text(
                                    q.description?.take(60).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.delete(q) }) {
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

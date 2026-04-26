package com.cen.feedback.ui.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.QuestionnaireFullInfoDTO
import com.cen.feedback.data.repo.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentHomeUi(
    val loading: Boolean = false,
    val nickname: String = "",
    val courses: List<Courses> = emptyList(),
    val questionnaires: List<QuestionnaireFullInfoDTO> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentHomeUi())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: return@runCatching null
            val nick = repo.tokenStore.nicknameFlow.let { repo.tokenStore.username() ?: "同学" }
            val courses = runCatching { repo.studentCourses(sid) }.getOrDefault(emptyList())
            val qs = runCatching { repo.studentAllQuestionnaires(sid) }.getOrDefault(emptyList())
            Triple(nick, courses, qs)
        }.onSuccess { v ->
            if (v == null) _state.update { it.copy(loading = false, error = "未登录") }
            else _state.update {
                it.copy(loading = false, nickname = v.first, courses = v.second, questionnaires = v.third)
            }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }
}

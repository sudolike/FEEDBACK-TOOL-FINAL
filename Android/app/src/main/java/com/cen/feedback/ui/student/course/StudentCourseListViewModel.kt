package com.cen.feedback.ui.student.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.model.StudentEnrollmentRow
import com.cen.feedback.data.repo.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentCourseListUi(
    val loading: Boolean = false,
    val courses: List<Courses> = emptyList(),
    /** 教师邀请中的（pending + teacher_invite） */
    val invitations: List<StudentEnrollmentRow> = emptyList(),
    /** 我自己提交的待审批申请（pending + student_apply） */
    val pendingApplies: List<StudentEnrollmentRow> = emptyList(),
    /** 被拒绝的申请（rejected） */
    val rejectedApplies: List<StudentEnrollmentRow> = emptyList(),
    val opMsg: String? = null,
    val error: String? = null,
)

@HiltViewModel
class StudentCourseListViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentCourseListUi())
    val state = _state.asStateFlow()

    init { refresh() }

    data class Bundle(
        val approved: List<Courses>,
        val invitations: List<StudentEnrollmentRow>,
        val pendingApplies: List<StudentEnrollmentRow>,
        val rejectedApplies: List<StudentEnrollmentRow>,
    )

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId()
                ?: return@runCatching Bundle(emptyList(), emptyList(), emptyList(), emptyList())
            val approved = repo.studentCourses(sid)
            val all = runCatching { repo.studentMyEnrollments(null) }.getOrDefault(emptyList())
            Bundle(
                approved = approved,
                invitations = all.filter {
                    it.enrollment?.status == "pending" && it.enrollment.source == "teacher_invite"
                },
                pendingApplies = all.filter {
                    it.enrollment?.status == "pending" && it.enrollment.source == "student_apply"
                },
                rejectedApplies = all.filter { it.enrollment?.status == "rejected" },
            )
        }
            .onSuccess { b ->
                _state.update { it.copy(
                    loading = false,
                    courses = b.approved,
                    invitations = b.invitations,
                    pendingApplies = b.pendingApplies,
                    rejectedApplies = b.rejectedApplies,
                ) }
            }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun acceptInvite(id: Long) = viewModelScope.launch {
        runCatching { repo.studentAcceptInvite(id) }
            .onSuccess { _state.update { it.copy(opMsg = "已加入课程") }; refresh() }
            .onFailure { e -> _state.update { it.copy(opMsg = e.message ?: "操作失败") } }
    }

    fun declineInvite(id: Long) = viewModelScope.launch {
        runCatching { repo.studentCancelEnrollment(id) }
            .onSuccess { _state.update { it.copy(opMsg = "已拒绝邀请") }; refresh() }
            .onFailure { e -> _state.update { it.copy(opMsg = e.message ?: "操作失败") } }
    }

    fun cancelApply(id: Long) = viewModelScope.launch {
        runCatching { repo.studentCancelEnrollment(id) }
            .onSuccess { _state.update { it.copy(opMsg = "已撤回申请") }; refresh() }
            .onFailure { e -> _state.update { it.copy(opMsg = e.message ?: "操作失败") } }
    }

    fun consumeMsg() = _state.update { it.copy(opMsg = null) }
}

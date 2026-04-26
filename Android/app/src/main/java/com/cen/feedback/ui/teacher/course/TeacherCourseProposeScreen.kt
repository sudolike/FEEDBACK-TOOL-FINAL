package com.cen.feedback.ui.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.CourseProposalRequest
import com.cen.feedback.data.model.Courses
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherCourseProposeUi(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val academicYear: String = "",
    val semester: Int? = null,
    val courseTime: String = "",
    val location: String = "",
    val rejectReason: String? = null,
    val originalStatus: String? = null,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class TeacherCourseProposeViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherCourseProposeUi())
    val state = _state.asStateFlow()

    fun load(editingId: Long?) {
        if (editingId == null || editingId <= 0L) {
            _state.update { it.copy(editingId = null) }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, editingId = editingId) }
            runCatching {
                val tid = repo.tokenStore.userId() ?: 0L
                repo.teacherAllCourses(tid).firstOrNull { it.id == editingId }
                    ?: throw IllegalArgumentException("课程不存在或已被管理员删除")
            }.onSuccess { c: Courses ->
                _state.update {
                    it.copy(
                        loading = false,
                        name = c.name.orEmpty(),
                        code = c.code.orEmpty(),
                        description = c.description.orEmpty(),
                        academicYear = c.academicYear.orEmpty(),
                        semester = c.semester,
                        courseTime = c.courseTime.orEmpty(),
                        location = c.location.orEmpty(),
                        rejectReason = c.rejectReason,
                        originalStatus = c.status,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun setCode(v: String) = _state.update { it.copy(code = v, error = null) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setAcademicYear(v: String) = _state.update { it.copy(academicYear = v) }
    fun setSemester(v: Int?) = _state.update { it.copy(semester = v) }
    fun setCourseTime(v: String) = _state.update { it.copy(courseTime = v) }
    fun setLocation(v: String) = _state.update { it.copy(location = v) }
    fun consume() = _state.update { it.copy(error = null, message = null) }

    fun submit(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank() || s.code.isBlank()) {
            _state.update { it.copy(error = "课程名和课程代码不能为空") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            runCatching {
                val tid = repo.tokenStore.userId() ?: 0L
                repo.submitCourseProposal(
                    CourseProposalRequest(
                        id = s.editingId,
                        name = s.name.trim(),
                        code = s.code.trim(),
                        teacherId = tid.takeIf { it > 0 },
                        description = s.description.ifBlank { null },
                        academicYear = s.academicYear.ifBlank { null },
                        semester = s.semester,
                        courseTime = s.courseTime.ifBlank { null },
                        location = s.location.ifBlank { null },
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, message = "已提交，等待管理员审批") }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(saving = false, error = e.message ?: "提交失败") }
            }
        }
    }
}

@Composable
fun TeacherCourseProposeScreen(
    editingId: Long?,
    onBack: () -> Unit,
    vm: TeacherCourseProposeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(editingId) { vm.load(editingId) }

    val isEditing = (editingId != null && editingId > 0)
    val isResubmit = s.originalStatus == "rejected"

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = if (isResubmit) "重新提交课程" else if (isEditing) "修改课程申请" else "申请新课程",
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (isResubmit && !s.rejectReason.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Danger500.copy(alpha = 0.1f),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = Danger500)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("管理员驳回理由", fontWeight = FontWeight.SemiBold, color = Danger500)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.rejectReason ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate800,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Primary100.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Info, null, tint = Primary700)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "提交后该课程将进入「待审批」状态，管理员通过后才会对学生可见。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary800,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = s.name, onValueChange = vm::setName,
                label = { Text("课程名称*") },
                leadingIcon = { Icon(Icons.Rounded.School, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.code, onValueChange = vm::setCode,
                label = { Text("课程代码*") },
                leadingIcon = { Icon(Icons.Rounded.Tag, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.description, onValueChange = vm::setDescription,
                label = { Text("课程介绍") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.academicYear, onValueChange = vm::setAcademicYear,
                label = { Text("学年（如 2025-2026）") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("学期", modifier = Modifier.padding(end = 12.dp))
                FilterChip(
                    selected = s.semester == 1,
                    onClick = { vm.setSemester(1) },
                    label = { Text("春季") },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = s.semester == 2,
                    onClick = { vm.setSemester(2) },
                    label = { Text("秋季") },
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.courseTime, onValueChange = vm::setCourseTime,
                label = { Text("上课时间（例如 周三 14:00-16:00）") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = s.location, onValueChange = vm::setLocation,
                label = { Text("上课地点") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            InlineError(s.error)
            if (s.message != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Success500.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Success500)
                        Spacer(Modifier.width(8.dp))
                        Text(s.message ?: "", color = Slate800, modifier = Modifier.weight(1f))
                        TextButton(onClick = vm::consume) { Text("好") }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (isResubmit) "重新提交审批" else "提交审批",
                icon = Icons.Rounded.UploadFile,
                loading = s.saving || s.loading,
                onClick = { vm.submit { } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = "取消",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

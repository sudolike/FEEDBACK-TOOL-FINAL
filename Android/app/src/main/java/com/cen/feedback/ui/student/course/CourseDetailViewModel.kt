package com.cen.feedback.ui.student.course

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.*
import com.cen.feedback.data.repo.FeedbackRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class CourseDetailUi(
    val loading: Boolean = false,
    val course: Courses? = null,
    val resources: List<CourseResource> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val questionnaires: List<QuestionnaireWithStatusDTO> = emptyList(),
    val feedbacks: List<CourseFeedbackDTO> = emptyList(),
    val posts: List<QaPost> = emptyList(),
    val myFeedbackContent: String = "",
    val myFeedbackRating: Int = 0,
    val sending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseDetailUi())
    val state = _state.asStateFlow()

    private var courseIdInternal: Long = 0L

    fun load(courseId: Long) {
        if (courseIdInternal == courseId) return
        courseIdInternal = courseId
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val cid = courseIdInternal
        if (cid == 0L) return@launch
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val sid = repo.tokenStore.userId() ?: 0L
            // 课程基础信息从学生课表中找
            val courses = runCatching { repo.studentCourses(sid) }.getOrDefault(emptyList())
            val course = courses.firstOrNull { it.id == cid }
            val resources = runCatching { repo.listResources(cid) }.getOrDefault(emptyList())
            val assignments = runCatching { repo.assignmentsByCourse(cid) }.getOrDefault(emptyList())
            val qstatus = runCatching { repo.questionnairesByStatus(cid, sid) }.getOrDefault(emptyMap())
            val ongoing = qstatus["ongoing"].orEmpty()
            val completed = qstatus["completed"].orEmpty()
            val fb = runCatching { repo.courseFeedbackList(cid) }.getOrDefault(emptyList())
            val posts = runCatching { repo.listPosts(cid) }.getOrDefault(emptyList())
            CourseDetailUi(
                loading = false,
                course = course,
                resources = resources,
                assignments = assignments,
                questionnaires = ongoing + completed,
                feedbacks = fb,
                posts = posts,
            )
        }.onSuccess { _state.value = it }
         .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun setMyFeedbackContent(v: String) = _state.update { it.copy(myFeedbackContent = v) }
    fun setMyFeedbackRating(v: Int) = _state.update { it.copy(myFeedbackRating = v) }

    fun submitFeedback() = viewModelScope.launch {
        val s = _state.value
        if (s.myFeedbackContent.isBlank() || s.myFeedbackRating <= 0) {
            _state.update { it.copy(error = "请填写内容并评分") }
            return@launch
        }
        val sid = repo.tokenStore.userId() ?: return@launch
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            repo.saveCourseFeedback(
                CourseFeedback(
                    courseId = courseIdInternal,
                    studentId = sid,
                    rating = s.myFeedbackRating,
                    content = s.myFeedbackContent.trim(),
                )
            )
        }.onSuccess {
            _state.update {
                it.copy(sending = false, message = "感谢你的反馈～",
                    myFeedbackContent = "", myFeedbackRating = 0)
            }
            refresh()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "提交失败") }
        }
    }

    /**
     * 上传资料：先 /file/upload 获取 url，再保存 CourseResource 元数据。
     */
    fun uploadResource(context: Context, file: File, title: String, category: String) = viewModelScope.launch {
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val url = repo.uploadFile(file)
            val sid = repo.tokenStore.userId() ?: 0L
            val role = repo.tokenStore.role() ?: "student"
            repo.saveResource(
                CourseResource(
                    courseId = courseIdInternal,
                    uploaderId = sid,
                    uploaderRole = role,
                    title = title.ifBlank { file.name },
                    fileName = file.name,
                    fileUrl = url,
                    fileType = file.extension,
                    fileSize = file.length(),
                    category = category,
                    description = "",
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, message = "上传成功") }
            refresh()
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "上传失败") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeError() = _state.update { it.copy(error = null) }
}

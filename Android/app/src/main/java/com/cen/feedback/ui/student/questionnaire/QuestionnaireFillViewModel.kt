package com.cen.feedback.ui.student.questionnaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.QuestionnaireResponses
import com.cen.feedback.data.model.Questionnaires
import com.cen.feedback.data.repo.FeedbackRepository
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 问卷"问题"结构。后端 `Questionnaires.questions` 是 JSON 字符串，
 * 数组元素结构来源于教师创建时存的字段（兼容已有数据）。
 */
@JsonClass(generateAdapter = true)
data class QuestionItem(
    val id: String? = null,
    val type: String? = null,            // single / multiple / text / rating
    val title: String? = null,
    val required: Boolean? = null,
    val options: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class AnswerItem(
    val id: String? = null,
    val value: Any? = null,              // String / List<String> / Int
)

data class QuestionnaireFillUi(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val questions: List<QuestionItem> = emptyList(),
    val answers: Map<String, Any?> = emptyMap(),
    val readonly: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)

@HiltViewModel
class QuestionnaireFillViewModel @Inject constructor(
    private val repo: FeedbackRepository,
    moshi: Moshi,
) : ViewModel() {

    private val listAdapter: JsonAdapter<List<QuestionItem>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, QuestionItem::class.java))
    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

    private val _state = MutableStateFlow(QuestionnaireFillUi())
    val state = _state.asStateFlow()

    private var courseIdInternal: Long = 0L
    private var qIdInternal: Long = 0L

    fun load(courseId: Long, qId: Long) {
        courseIdInternal = courseId
        qIdInternal = qId
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val sid = repo.tokenStore.userId() ?: 0L
                // 1) 看学生是否已经提交
                val resp = runCatching { repo.studentResponseDetail(courseId, qId, sid) }
                    .getOrNull()
                val q = resp?.questionnaire ?: pickFromList(courseId, qId)
                val items = (q?.questions?.let { runCatching { listAdapter.fromJson(it) }.getOrNull() }
                    ?: emptyList())
                val pre = if (resp?.answers != null) {
                    runCatching { mapAdapter.fromJson(resp.answers) }.getOrNull() ?: emptyMap()
                } else emptyMap()
                QuestionnaireFillUi(
                    loading = false,
                    title = q?.title ?: "问卷",
                    description = q?.description ?: "",
                    questions = items,
                    answers = pre,
                    readonly = resp != null,
                    submitted = resp != null,
                )
            }.onSuccess { _state.value = it }
             .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    private suspend fun pickFromList(courseId: Long, qId: Long): Questionnaires? {
        val sid = repo.tokenStore.userId() ?: return null
        val list = runCatching { repo.questionnairesByStatus(courseId, sid) }.getOrNull() ?: return null
        val all = (list["ongoing"].orEmpty() + list["completed"].orEmpty())
        return all.firstOrNull { it.questionnaire?.id == qId }?.questionnaire
    }

    fun setAnswer(qid: String, value: Any?) {
        _state.update { it.copy(answers = it.answers.toMutableMap().apply { put(qid, value) }) }
    }

    fun submit(onDone: () -> Unit) {
        val s = _state.value
        // required 校验
        val missing = s.questions.firstOrNull {
            it.required == true && (s.answers[it.id ?: ""].let { v ->
                v == null || (v is String && v.isBlank()) || (v is List<*> && v.isEmpty())
            })
        }
        if (missing != null) {
            _state.update { it.copy(error = "请完成必答题：${missing.title}") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            runCatching {
                val sid = repo.tokenStore.userId() ?: 0L
                val json = mapAdapter.toJson(s.answers)
                repo.saveQuestionnaireResponse(
                    QuestionnaireResponses(
                        courseId = courseIdInternal,
                        questionnaireId = qIdInternal,
                        studentId = sid,
                        answers = json,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(sending = false, submitted = true) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(sending = false, error = e.message ?: "提交失败") }
            }
        }
    }

    fun consumeError() = _state.update { it.copy(error = null) }

    /** UI 层设置错误信息（供 UI 上层做字段校验时使用） */
    fun setError(msg: String?) = _state.update { it.copy(error = msg) }
}

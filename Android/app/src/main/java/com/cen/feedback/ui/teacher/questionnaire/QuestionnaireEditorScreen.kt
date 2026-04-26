package com.cen.feedback.ui.teacher.questionnaire

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.Questionnaires
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class EditorQuestion(
    val id: String,
    var type: String = "single",
    var title: String = "",
    var required: Boolean = true,
    var options: List<String> = listOf("选项1", "选项2"),
)

data class QuestionnaireEditorUi(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val items: List<EditorQuestion> = emptyList(),
    val sending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class QuestionnaireEditorViewModel @Inject constructor(
    private val repo: FeedbackRepository,
    moshi: Moshi,
) : ViewModel() {
    private val listAdapter: JsonAdapter<List<EditorQuestion>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, EditorQuestion::class.java))

    private val _state = MutableStateFlow(QuestionnaireEditorUi())
    val state = _state.asStateFlow()
    private var editingId: Long? = null

    fun load(qId: Long?) {
        editingId = qId
        if (qId == null) {
            _state.value = QuestionnaireEditorUi(
                items = listOf(EditorQuestion(id = UUID.randomUUID().toString(),
                    type = "rating", title = "你对本节课整体满意度？",
                    required = true, options = emptyList()))
            )
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val tid = repo.tokenStore.userId() ?: 0L
                val list = repo.teacherQuestionnaires(tid)
                list.firstOrNull { it.id == qId }
            }.onSuccess { q ->
                if (q == null) {
                    _state.update { it.copy(loading = false, error = "问卷不存在") }
                } else {
                    val items = runCatching { listAdapter.fromJson(q.questions ?: "[]") }
                        .getOrNull()
                        .orEmpty()
                        .ifEmpty {
                            listOf(EditorQuestion(id = UUID.randomUUID().toString(),
                                type = "rating", title = "整体满意度？", required = true,
                                options = emptyList()))
                        }
                    _state.update {
                        it.copy(
                            loading = false,
                            title = q.title.orEmpty(),
                            description = q.description.orEmpty(),
                            items = items,
                        )
                    }
                }
            }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun addQuestion(type: String) {
        val q = EditorQuestion(
            id = UUID.randomUUID().toString(),
            type = type,
            title = "新题目",
            required = true,
            options = if (type == "single" || type == "multiple") listOf("选项1", "选项2") else emptyList(),
        )
        _state.update { it.copy(items = it.items + q) }
    }
    fun removeQuestion(id: String) =
        _state.update { it.copy(items = it.items.filterNot { q -> q.id == id }) }
    fun updateQuestion(id: String, transform: (EditorQuestion) -> EditorQuestion) =
        _state.update { it.copy(items = it.items.map { q -> if (q.id == id) transform(q) else q }) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "请填写问卷标题") }
            return@launch
        }
        _state.update { it.copy(sending = true, error = null) }
        runCatching {
            val tid = repo.tokenStore.userId() ?: 0L
            repo.saveQuestionnaire(
                Questionnaires(
                    id = editingId ?: 0L,
                    title = s.title,
                    description = s.description,
                    createdBy = tid,
                    questions = listAdapter.toJson(s.items),
                )
            )
        }.onSuccess {
            _state.update { it.copy(sending = false, saved = true, message = "已保存") }
        }.onFailure { e ->
            _state.update { it.copy(sending = false, error = e.message ?: "保存失败") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun QuestionnaireEditorScreen(
    editingId: Long?,
    onBack: () -> Unit,
    vm: QuestionnaireEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(editingId) { vm.load(editingId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    if (s.message != null) {
        LaunchedEffect(s.message) {
            android.widget.Toast.makeText(ctx, s.message, android.widget.Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(
                title = if (editingId == null) "新建问卷" else "编辑问卷",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = vm::save,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("保存")
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = s.title, onValueChange = vm::setTitle,
                            label = { Text("问卷标题") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = s.description, onValueChange = vm::setDescription,
                            label = { Text("问卷说明（可选）") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2, shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
                itemsIndexed(s.items, key = { _, it -> it.id }) { idx, q ->
                    EditorQuestionCard(
                        index = idx + 1,
                        item = q,
                        onChange = { fn -> vm.updateQuestion(q.id, fn) },
                        onDelete = { vm.removeQuestion(q.id) },
                    )
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("添加题目", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            QuickAddBtn("单选", Icons.Rounded.RadioButtonChecked) { vm.addQuestion("single") }
                            QuickAddBtn("多选", Icons.Rounded.CheckBox) { vm.addQuestion("multiple") }
                            QuickAddBtn("评分", Icons.Rounded.Star) { vm.addQuestion("rating") }
                            QuickAddBtn("简答", Icons.Rounded.TextFields) { vm.addQuestion("text") }
                        }
                    }
                }
                item { InlineError(s.error) }
            }
        }
        LoadingOverlay(visible = s.sending)
    }
}

@Composable
private fun RowScope.QuickAddBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                                  onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        color = Primary600.copy(alpha = 0.1f),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = Primary600)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Primary600)
        }
    }
}

@Composable
private fun EditorQuestionCard(
    index: Int,
    item: EditorQuestion,
    onChange: ((EditorQuestion) -> EditorQuestion) -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Primary600),
                contentAlignment = Alignment.Center,
            ) { Text("$index", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            Spacer(Modifier.width(8.dp))
            StatusChip(
                when (item.type) {
                    "single" -> "单选"; "multiple" -> "多选"
                    "rating" -> "评分"; else -> "简答"
                },
                color = when (item.type) {
                    "single" -> Primary600; "multiple" -> Accent500
                    "rating" -> Warning500; else -> Slate600
                }
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null,
                    tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = item.title,
            onValueChange = { v -> onChange { it.copy(title = v) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入题目") },
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("必答", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(6.dp))
            Switch(
                checked = item.required,
                onCheckedChange = { v -> onChange { it.copy(required = v) } },
            )
        }
        if (item.type == "single" || item.type == "multiple") {
            Spacer(Modifier.height(6.dp))
            item.options.forEachIndexed { i, opt ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)) {
                    OutlinedTextField(
                        value = opt,
                        onValueChange = { v ->
                            onChange { q -> q.copy(options = q.options.toMutableList().apply { set(i, v) }) }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("选项 ${i + 1}") },
                        shape = RoundedCornerShape(10.dp),
                    )
                    IconButton(onClick = {
                        onChange { q -> q.copy(options = q.options.toMutableList().also { it.removeAt(i) }) }
                    }) {
                        Icon(Icons.Rounded.RemoveCircle, null,
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TextButton(onClick = {
                onChange { q -> q.copy(options = q.options + "选项 ${q.options.size + 1}") }
            }) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("添加选项")
            }
        }
    }
}

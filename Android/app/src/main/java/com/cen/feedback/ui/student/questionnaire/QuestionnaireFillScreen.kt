package com.cen.feedback.ui.student.questionnaire

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Success500

@Composable
fun QuestionnaireFillScreen(
    courseId: Long,
    questionnaireId: Long,
    onBack: () -> Unit,
    vm: QuestionnaireFillViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId, questionnaireId) { vm.load(courseId, questionnaireId) }
    val s by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(
                title = if (s.readonly) "查看作答" else "填写问卷",
                onBack = onBack,
            )
            if (s.loading) {
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
                return@Column
            }
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(s.title, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold)
                            if (s.description.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(s.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            if (s.readonly) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = Success500)
                                    Spacer(Modifier.width(6.dp))
                                    Text("你已提交此问卷",
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                if (s.questions.isEmpty()) {
                    item { EmptyState(title = "题目为空", subtitle = "请联系教师检查问卷配置") }
                }

                items(s.questions.withIndex().toList()) { (idx, q) ->
                    val qId = q.id ?: idx.toString()
                    QuestionRow(
                        idx = idx + 1,
                        item = q,
                        value = s.answers[qId],
                        readonly = s.readonly,
                        onValueChange = { vm.setAnswer(qId, it) },
                    )
                }

                if (!s.readonly) {
                    item {
                        InlineError(s.error)
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(
                            text = "提交问卷（匿名）",
                            icon = Icons.Rounded.Send,
                            onClick = { vm.submit(onBack) },
                            loading = s.sending,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "提交后教师将看到匿名结果，不会展示你的姓名。",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        LoadingOverlay(visible = s.sending)
    }
}

@Composable
private fun QuestionRow(
    idx: Int,
    item: QuestionItem,
    value: Any?,
    readonly: Boolean,
    onValueChange: (Any?) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Primary600),
                contentAlignment = Alignment.Center,
            ) {
                Text("$idx", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                item.title.orEmpty() + if (item.required == true) " *" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))

        when ((item.type ?: "text").lowercase()) {
            "single" -> {
                val opts = item.options.orEmpty()
                Column {
                    opts.forEach { opt ->
                        val selected = value == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { if (!readonly) onValueChange(opt) },
                            )
                            Text(opt, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            "multiple" -> {
                val opts = item.options.orEmpty()
                @Suppress("UNCHECKED_CAST")
                val list = (value as? List<String>) ?: emptyList()
                Column {
                    opts.forEach { opt ->
                        val checked = list.contains(opt)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (readonly) return@Checkbox
                                    val newList = if (it) list + opt else list - opt
                                    onValueChange(newList)
                                },
                            )
                            Text(opt, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            "rating" -> {
                StarRatingBar(
                    rating = (value as? Int) ?: (value as? Double)?.toInt() ?: 0,
                    onChange = { if (!readonly) onValueChange(it) },
                    enabled = !readonly,
                )
            }
            else -> {
                OutlinedTextField(
                    value = (value as? String) ?: "",
                    onValueChange = { if (!readonly) onValueChange(it) },
                    readOnly = readonly,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

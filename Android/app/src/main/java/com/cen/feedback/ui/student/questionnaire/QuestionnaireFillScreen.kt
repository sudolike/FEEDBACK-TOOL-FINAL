package com.cen.feedback.ui.student.questionnaire

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cen.feedback.R
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.Primary100
import com.cen.feedback.ui.theme.Primary600
import com.cen.feedback.ui.theme.Success500
import kotlinx.coroutines.launch

@Composable
fun QuestionnaireFillScreen(
    courseId: Long,
    questionnaireId: Long,
    onBack: () -> Unit,
    vm: QuestionnaireFillViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId, questionnaireId) { vm.load(courseId, questionnaireId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val total = s.questions.size
    val answered = s.questions.count { q ->
        val key = q.id ?: s.questions.indexOf(q).toString()
        val v = s.answers[key]
        when ((q.type ?: "text").lowercase()) {
            "multiple" -> (v as? List<*>)?.isNotEmpty() == true
            "rating" -> ((v as? Int) ?: (v as? Double)?.toInt() ?: 0) > 0
            else -> v?.toString()?.isNotBlank() == true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientTopBar(
                title = stringResource(
                    if (s.readonly) R.string.quest_title_readonly else R.string.quest_title_fill
                ),
                onBack = onBack,
            )
            if (!s.readonly && total > 0) {
                QuestionnaireProgress(answered = answered, total = total)
            }
            if (s.loading) {
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
                return@Column
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = if (s.readonly) 24.dp else 96.dp),
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
                                    Text(stringResource(R.string.quest_already_submitted),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                if (s.questions.isEmpty()) {
                    item {
                        EmptyStateAction(
                            title = stringResource(R.string.quest_empty_title),
                            subtitle = stringResource(R.string.quest_empty_subtitle),
                        )
                    }
                }

                items(s.questions.size) { idx ->
                    val q = s.questions[idx]
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
                        Text(
                            stringResource(R.string.quest_privacy_hint),
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

        // sticky 提交底栏 —— 需求 6 验收点 4
        if (!s.readonly && s.questions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.quest_progress_format, answered, total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.quest_privacy_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    PrimaryButton(
                        text = stringResource(R.string.btn_submit),
                        icon = Icons.Rounded.Send,
                        onClick = {
                            // 必答题校验 —— 需求 6 验收点 5
                            val missingIdx = s.questions.indexOfFirst { q ->
                                if (q.required != true) return@indexOfFirst false
                                val key = q.id ?: s.questions.indexOf(q).toString()
                                val v = s.answers[key]
                                when ((q.type ?: "text").lowercase()) {
                                    "multiple" -> (v as? List<*>).isNullOrEmpty()
                                    "rating" -> ((v as? Int) ?: (v as? Double)?.toInt() ?: 0) == 0
                                    else -> v?.toString().isNullOrBlank()
                                }
                            }
                            if (missingIdx >= 0) {
                                vm.setError(
                                    "第 ${missingIdx + 1} 题未作答"
                                )
                                scope.launch {
                                    // +1 因为顶部有简介卡片 item
                                    listState.animateScrollToItem(missingIdx + 1)
                                }
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.submit(onBack)
                            }
                        },
                        loading = s.sending,
                    )
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
                Column(modifier = Modifier.selectableGroup()) {
                    opts.forEach { opt ->
                        val selected = value == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) Primary100
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) Primary600 else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable(enabled = !readonly) { onValueChange(opt) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (checked) Primary100
                                    else Color.Transparent
                                )
                                .clickable(enabled = !readonly) {
                                    val newList = if (checked) list - opt else list + opt
                                    onValueChange(newList)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                            )
                            Text(opt, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            "rating" -> {
                val rating = (value as? Int) ?: (value as? Double)?.toInt() ?: 0
                Column(
                    modifier = Modifier.semantics {
                        this.role = Role.Button
                        this.stateDescription = "$rating 星，共 5 星"
                    }
                ) {
                    StarRatingBar(
                        rating = rating,
                        onChange = { if (!readonly) onValueChange(it) },
                        enabled = !readonly,
                    )
                    if (rating > 0) {
                        Spacer(Modifier.height(6.dp))
                        val hintRes = when (rating) {
                            1 -> R.string.quest_rating_hint_1
                            2 -> R.string.quest_rating_hint_2
                            3 -> R.string.quest_rating_hint_3
                            4 -> R.string.quest_rating_hint_4
                            else -> R.string.quest_rating_hint_5
                        }
                        Text(
                            stringResource(hintRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary600,
                        )
                    }
                }
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

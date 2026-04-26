package com.cen.feedback.ui.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cen.feedback.data.model.User
import com.cen.feedback.data.repo.FeedbackRepository
import com.cen.feedback.ui.components.*
import com.cen.feedback.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val ROLE_FILTERS = listOf(
    "" to "全部",
    "student" to "学生",
    "teacher" to "教师",
    "admin"   to "管理员",
)

data class AdminUserUi(
    val loading: Boolean = false,
    val users: List<User> = emptyList(),
    val total: Long = 0L,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
    val keyword: String = "",
    val role: String = "",
    val error: String? = null,
    val message: String? = null,
    val acting: Long? = null,
)

@HiltViewModel
class AdminUserViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminUserUi())
    val state = _state.asStateFlow()
    init { refresh() }

    fun setKeyword(v: String) = _state.update { it.copy(keyword = v) }
    fun setRole(v: String) {
        _state.update { it.copy(role = v, pageNum = 1) }; refresh()
    }
    fun consumeMessage() = _state.update { it.copy(message = null, error = null) }

    fun search() {
        _state.update { it.copy(pageNum = 1) }; refresh()
    }
    fun nextPage() {
        _state.update { it.copy(pageNum = it.pageNum + 1) }; refresh()
    }
    fun prevPage() {
        if (_state.value.pageNum <= 1) return
        _state.update { it.copy(pageNum = it.pageNum - 1) }; refresh()
    }

    fun refresh() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            repo.adminUsersPage(
                pageNum = s.pageNum, pageSize = s.pageSize,
                username = s.keyword, role = s.role,
            )
        }.onSuccess { p ->
            _state.update {
                it.copy(
                    loading = false,
                    users = p.records ?: emptyList(),
                    total = p.total ?: 0L,
                )
            }
        }.onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun toggleStatus(u: User) = viewModelScope.launch {
        if (u.id <= 0L) return@launch
        _state.update { it.copy(acting = u.id) }
        runCatching {
            if (u.status == 0) repo.adminEnableUser(u.id) else repo.adminDisableUser(u.id)
        }.onSuccess {
            _state.update { it.copy(acting = null, message = if (u.status == 0) "已启用" else "已停用") }
            refresh()
        }.onFailure { e -> _state.update { it.copy(acting = null, error = e.message) } }
    }

    fun resetPassword(u: User) = viewModelScope.launch {
        _state.update { it.copy(acting = u.id) }
        runCatching { repo.adminResetUserPassword(u.id) }
            .onSuccess { ret ->
                val newPwd = ret["password"] ?: "(已重置)"
                _state.update { it.copy(acting = null, message = "新密码：$newPwd（请告知该用户）") }
            }
            .onFailure { e -> _state.update { it.copy(acting = null, error = e.message) } }
    }

    fun delete(u: User) = viewModelScope.launch {
        _state.update { it.copy(acting = u.id) }
        runCatching { repo.adminDeleteUser(u.id) }
            .onSuccess { _state.update { it.copy(acting = null, message = "已删除") }; refresh() }
            .onFailure { e -> _state.update { it.copy(acting = null, error = e.message) } }
    }
}

@Composable
fun AdminUserListScreen(
    vm: AdminUserViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<User?>(null) }
    var deletingFor by remember { mutableStateOf<User?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "用户管理 (${s.total})",
            actions = {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Rounded.Refresh, "刷新", tint = Color.White)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = s.keyword,
                onValueChange = vm::setKeyword,
                label = { Text("用户名搜索") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    IconButton(onClick = vm::search) { Icon(Icons.Rounded.Search, null) }
                }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ROLE_FILTERS) { (value, label) ->
                FilterChip(
                    selected = s.role == value,
                    onClick = { vm.setRole(value) },
                    label = { Text(label) },
                )
            }
        }

        if (s.message != null || s.error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (s.error != null) MaterialTheme.colorScheme.errorContainer
                        else Success500.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (s.error != null) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                        null,
                        tint = if (s.error != null) MaterialTheme.colorScheme.onErrorContainer else Success500,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.error ?: s.message ?: "",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (s.error != null) MaterialTheme.colorScheme.onErrorContainer else Slate800,
                    )
                    TextButton(onClick = vm::consumeMessage) { Text("好") }
                }
            }
        }

        when {
            s.loading && s.users.isEmpty() ->
                LazyColumn(modifier = Modifier.fillMaxSize()) { shimmerCards() }
            s.users.isEmpty() ->
                EmptyState(title = "未找到用户", icon = Icons.Rounded.PersonOff,
                    modifier = Modifier.padding(top = 48.dp))
            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                items(s.users) { u ->
                    UserRow(
                        u = u,
                        loading = s.acting == u.id,
                        onMenu = { menuFor = u },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = vm::prevPage, enabled = s.pageNum > 1) { Text("上一页") }
            Text("第 ${s.pageNum} 页 · 共 ${s.total} 条", style = MaterialTheme.typography.bodySmall)
            TextButton(
                onClick = vm::nextPage,
                enabled = s.pageNum * s.pageSize < s.total,
            ) { Text("下一页") }
        }
    }

    if (menuFor != null) {
        UserActionDialog(
            u = menuFor!!,
            onDismiss = { menuFor = null },
            onToggle = { vm.toggleStatus(menuFor!!); menuFor = null },
            onReset = { vm.resetPassword(menuFor!!); menuFor = null },
            onDelete = { deletingFor = menuFor; menuFor = null },
        )
    }
    if (deletingFor != null) {
        AlertDialog(
            onDismissRequest = { deletingFor = null },
            title = { Text("删除用户") },
            text = { Text("确定删除「${deletingFor?.username ?: ""}」吗？管理员账号无法被另一个管理员直接删除。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(deletingFor!!); deletingFor = null }) {
                    Text("删除", color = Danger500)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFor = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun UserRow(
    u: User,
    loading: Boolean,
    onMenu: () -> Unit,
) {
    val (roleLabel, roleColor) = when (u.role) {
        "admin"   -> "管理员" to Pink500
        "teacher" -> "教师"   to Accent600
        "student" -> "学生"   to Primary600
        else      -> (u.role ?: "—") to Slate500
    }
    val statusLabel = if (u.status == 0) "已停用" else "正常"
    val statusColor = if (u.status == 0) Danger500 else Success500
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.5.dp,
        onClick = onMenu,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (u.nickname ?: u.username ?: "?").take(1),
                    color = roleColor, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(u.nickname ?: u.username ?: "—", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    StatusChip(roleLabel, color = roleColor)
                    Spacer(Modifier.width(4.dp))
                    StatusChip(statusLabel, color = statusColor)
                }
                Text(
                    "@${u.username ?: "—"}  ·  id ${u.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    color = Primary600, strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(Icons.Rounded.MoreHoriz, null, tint = Slate500)
            }
        }
    }
}

@Composable
private fun UserActionDialog(
    u: User,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理用户：${u.nickname ?: u.username}") },
        text = {
            Column {
                ListItem(
                    headlineContent = {
                        Text(if (u.status == 0) "启用账号" else "停用账号")
                    },
                    leadingContent = {
                        Icon(if (u.status == 0) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            null, tint = if (u.status == 0) Success500 else Warning500)
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("重置密码") },
                    leadingContent = { Icon(Icons.Rounded.Key, null, tint = Primary600) },
                )
                Spacer(Modifier.height(4.dp))
                ListItem(
                    headlineContent = { Text("删除用户", color = Danger500) },
                    leadingContent = { Icon(Icons.Rounded.Delete, null, tint = Danger500) },
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onToggle) {
                    Text(if (u.status == 0) "启用账号" else "停用账号")
                }
                TextButton(onClick = onReset) { Text("重置密码") }
                TextButton(onClick = onDelete) { Text("删除用户", color = Danger500) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

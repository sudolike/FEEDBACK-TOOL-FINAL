package com.cen.feedback.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore("auth")

/**
 * 持久化登录后的 Token / 角色 / 用户基本信息。
 * 后端登录返回 UserDTO（含 token、id、username、nickname、role、roleId）。
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val NICKNAME = stringPreferencesKey("nickname")
        val ROLE = stringPreferencesKey("role")
        val AVATAR = stringPreferencesKey("avatar")
    }

    val tokenFlow: Flow<String?> = context.tokenDataStore.data.map { it[Keys.TOKEN] }
    val userIdFlow: Flow<Long?> = context.tokenDataStore.data.map { it[Keys.USER_ID] }
    val roleFlow: Flow<String?> = context.tokenDataStore.data.map { it[Keys.ROLE] }
    val nicknameFlow: Flow<String?> = context.tokenDataStore.data.map { it[Keys.NICKNAME] }
    val avatarFlow: Flow<String?> = context.tokenDataStore.data.map { it[Keys.AVATAR] }

    suspend fun token(): String? = context.tokenDataStore.data.first()[Keys.TOKEN]
    suspend fun userId(): Long? = context.tokenDataStore.data.first()[Keys.USER_ID]
    suspend fun role(): String? = context.tokenDataStore.data.first()[Keys.ROLE]
    suspend fun username(): String? = context.tokenDataStore.data.first()[Keys.USERNAME]

    suspend fun save(
        token: String,
        userId: Long,
        username: String,
        nickname: String?,
        role: String,
        avatar: String?,
    ) {
        context.tokenDataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
            it[Keys.USERNAME] = username
            it[Keys.NICKNAME] = nickname ?: username
            it[Keys.ROLE] = role
            it[Keys.AVATAR] = avatar ?: ""
        }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }
}

package com.kangtaeyoung.daynote.data.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 로그인/가입 요청 본문. */
@Serializable
data class AuthRequest(val email: String, val password: String)

/** Supabase Auth 응답(필요한 필드만). */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: AuthUser? = null,
)

@Serializable
data class AuthUser(val id: String = "", val email: String? = null)

/** 로그인 후 메모리/안전저장소에 들고 다니는 세션. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
)

/** notes 테이블 행(컬럼명 = snake_case). */
@Serializable
data class NoteRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String = "",
    val content: String = "",
    val date: Long? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

/** tasks 테이블 행(컬럼명 = snake_case). */
@Serializable
data class TaskRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("note_id") val noteId: String? = null,
    val text: String = "",
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("due_date") val dueDate: Long? = null,
    @SerialName("all_day") val allDay: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

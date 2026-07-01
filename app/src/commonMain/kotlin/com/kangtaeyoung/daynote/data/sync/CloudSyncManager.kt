package com.kangtaeyoung.daynote.data.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * 클라우드 동기화 상태(UI 표시용). 구글 캘린더([SyncState])와 별개 — 이쪽은 메모·할 일 본체를
 * 멀티기기에서 같게 만드는 동기화다(Phase 6, Supabase).
 */
sealed interface CloudSyncState {
    /** 동기화 토글 꺼짐. */
    data object Disabled : CloudSyncState

    /** 토글은 켜졌으나 Supabase URL/anon key 미설정(설정 화면에서 입력 필요). */
    data object NeedsConfig : CloudSyncState

    /** 설정 완료 · 로그인 필요. */
    data object SignedOut : CloudSyncState

    /** 로그인됨 · 대기. */
    data class SignedIn(val userId: String) : CloudSyncState

    data object Syncing : CloudSyncState
    data class Synced(val lastSyncMillis: Long) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}

/**
 * 클라우드 동기화 추상화(설계원칙 4: 본체·UI 는 동기화 구현을 모른다). Supabase 구현은 Ktor 라
 * Android·Desktop 양쪽에서 같은 코드로 동작한다(구글 캘린더와 달리 플랫폼 격리 불필요).
 */
interface CloudSyncManager {

    val state: StateFlow<CloudSyncState>

    /** Supabase URL·anon key 가 설정돼 있는지. */
    fun isConfigured(): Boolean

    /** 설정/세션을 다시 읽어 상태를 정리한다(앱 시작·설정 변경 시). */
    suspend fun refreshState()

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut()

    /** 지금 동기화(양방향: 로컬 변경 push + 원격 변경 pull). */
    suspend fun syncNow()
}

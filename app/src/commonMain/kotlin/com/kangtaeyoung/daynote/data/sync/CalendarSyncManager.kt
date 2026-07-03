package com.kangtaeyoung.daynote.data.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * 동기화 상태(UI 표시용). Phase 3 골격 — 실제 구글 캘린더 연동은 OAuth 자격증명 준비 후 채운다.
 */
sealed interface SyncState {
    /** 플랫폼이 동기화를 지원하지 않음(예: 데스크톱). */
    data object Unavailable : SyncState

    /** 플랫폼은 지원하나 OAuth 자격증명 미설정(개발자가 Google Cloud Console 설정 필요). */
    data object NeedsSetup : SyncState

    data object SignedOut : SyncState
    data class SignedIn(val account: String) : SyncState
    data object Syncing : SyncState
    data class Synced(val lastSyncMillis: Long) : SyncState
    data class Error(val message: String) : SyncState
}

/** 구글 캘린더 목록의 한 항목 — 내 캘린더와 공유받은 캘린더를 모두 포함한다. */
data class GoogleCalendarInfo(
    val id: String,
    val name: String,
    /** 구글이 지정한 캘린더 배경색("#RRGGBB"). */
    val colorHex: String?,
    /** 기본(내) 캘린더 여부. */
    val primary: Boolean,
)

/**
 * 구글 캘린더 동기화 추상화(설계원칙 4: 본체는 동기화 구현을 모른다).
 *
 * Calendar API·Credential Manager 는 Android 전용이라 구현을 플랫폼 뒤로 격리한다
 * (Android=실연동(추후), Desktop=비활성 스텁). 본체·UI 는 이 인터페이스만 본다.
 */
interface CalendarSyncManager {

    /** 이 플랫폼에서 동기화가 가능한지(데스크톱=false). */
    val isAvailable: Boolean

    val state: StateFlow<SyncState>

    // 동기화 on/off 토글은 SettingsRepository(영속)로 옮겼다 — 매니저는 인증/동기화만 책임진다.
    // 로그인(인가)은 Activity·동의 UI가 필요해 플랫폼 Compose 쪽에서 시작한다
    // (Android: rememberGoogleCalendarSignIn). 결과 토큰은 플랫폼 구현이 내부적으로 받는다.

    suspend fun signOut()

    /** 지금 동기화(양방향). */
    suspend fun syncNow()

    /**
     * 계정의 캘린더 목록(공유받은 캘린더 포함) — 설정의 "표시할 캘린더" 체크 UI 용.
     * 미지원 플랫폼/미로그인이면 실패 Result.
     */
    suspend fun listCalendars(): Result<List<GoogleCalendarInfo>> =
        Result.failure(UnsupportedOperationException("이 플랫폼에서는 지원하지 않습니다."))
}

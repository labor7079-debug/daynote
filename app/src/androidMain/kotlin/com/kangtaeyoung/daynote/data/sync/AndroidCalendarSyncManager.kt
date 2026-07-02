package com.kangtaeyoung.daynote.data.sync

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android 동기화 매니저.
 *
 * - 로그인/인가: `rememberGoogleCalendarSignIn` 이 받은 액세스 토큰을 [onAccessToken] 으로 전달.
 *   **무음 재로그인**: 한 번 동의한 계정은 [silentAccessToken] 으로 재시작·토큰 만료 후에도
 *   동의 화면 없이 토큰을 자동 확보한다 → 세션마다 로그인할 필요가 없다.
 * - 동기화(3-B2, 현재): **한 방향 push** — 날짜 있는 메모를 구글 캘린더 종일 이벤트로 생성/수정/삭제.
 *   (캘린더→앱 pull, 할 일 동기화는 다음 슬라이스.) 충돌은 추후 서버 타임스탬프 기준으로 확장.
 */
class AndroidCalendarSyncManager(
    private val context: Context,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
) : CalendarSyncManager {

    private val api = CalendarApi()

    override val isAvailable: Boolean = true

    private val _state = MutableStateFlow<SyncState>(SyncState.NeedsSetup)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    @Volatile
    var accessToken: String? = null
        private set

    fun onAccessToken(token: String?) {
        if (token.isNullOrBlank()) {
            _state.value = SyncState.Error("액세스 토큰을 받지 못했습니다.")
            return
        }
        accessToken = token
        signedOut = false // 명시적 로그인 — 이후 무음 재인가 허용.
        _state.value = SyncState.SignedIn("구글 계정")
    }

    fun onAuthError(message: String) {
        _state.value = SyncState.Error(message)
    }

    override suspend fun signOut() {
        accessToken = null
        signedOut = true // 명시적 로그아웃 — 무음 재인가도 막는다(다시 로그인 버튼을 누를 때까지).
        _state.value = SyncState.SignedOut
    }

    /** 사용자가 명시적으로 로그아웃했는지. true 면 무음 재인가를 시도하지 않는다. */
    @Volatile
    private var signedOut = false

    /**
     * 무음(silent) 재인가 — 이미 동의한 계정이면 동의 화면 없이 토큰을 받는다.
     * 동의가 필요한 첫 사용(hasResolution)이나 실패 시 null → 설정 화면에서 1회 로그인 필요.
     */
    private suspend fun silentAccessToken(): String? = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GoogleAuthConfig.CALENDAR_SCOPE)))
            .build()
        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { result ->
                cont.resume(if (result.hasResolution()) null else result.accessToken)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /** 현재 토큰이 없으면 무음 재인가로 확보한다(명시적 로그아웃 상태면 건너뜀). */
    private suspend fun ensureToken(): String? {
        accessToken?.let { return it }
        if (signedOut) return null
        val token = silentAccessToken() ?: return null
        accessToken = token
        _state.value = SyncState.SignedIn("구글 계정")
        return token
    }

    override suspend fun syncNow() {
        val token = ensureToken()
        if (token == null) {
            _state.value = SyncState.Error("먼저 구글 로그인이 필요합니다. (최초 1회, 설정 화면)")
            return
        }
        _state.value = SyncState.Syncing
        try {
            pushAll(token)
            _state.value = SyncState.Synced(nowMillis())
        } catch (e: CalendarApiException) {
            if (e.code == 401) {
                // 토큰 만료 — 무음 재인가로 새 토큰을 받아 1회 재시도(재로그인 요구하지 않음).
                accessToken = null
                val renewed = if (signedOut) null else silentAccessToken()
                if (renewed != null) {
                    accessToken = renewed
                    runCatching { pushAll(renewed) }
                        .onSuccess { _state.value = SyncState.Synced(nowMillis()) }
                        .onFailure { _state.value = SyncState.Error(it.message ?: "동기화 실패") }
                } else {
                    _state.value = SyncState.Error("세션이 만료됐습니다. 다시 로그인해 주세요.")
                }
            } else {
                _state.value = SyncState.Error(e.message ?: "동기화 실패")
            }
        } catch (e: Exception) {
            _state.value = SyncState.Error(e.message ?: "동기화 실패")
        }
    }

    /** 실제 push 본체 — 삭제 반영 → 메모 → 할 일. 실패는 예외로 올려 [syncNow] 가 처리한다. */
    private suspend fun pushAll(token: String) {
        // 1) 삭제 반영: 소프트 삭제됐고 원격 이벤트가 있는 메모/할 일 → 이벤트 삭제.
        for (note in noteDao.getDeletedNotesWithRemote()) {
            val remoteId = note.remoteId ?: continue
            api.deleteEvent(token, remoteId)
            noteDao.clearRemote(note.id)
        }
        for (task in taskDao.getDeletedTasksWithRemote()) {
            val remoteId = task.remoteId ?: continue
            api.deleteEvent(token, remoteId)
            taskDao.clearRemote(task.id)
        }
        // 2) 신규/수정 push: 날짜 있는 활성 메모 → 종일 이벤트.
        for (note in noteDao.getNotesToPush()) {
            val start = note.date ?: continue
            val remoteId = note.remoteId
            if (remoteId == null) {
                val eventId = api.insertEvent(token, note.title, note.content, start, allDay = true)
                noteDao.markSynced(note.id, eventId)
            } else {
                api.updateEvent(token, remoteId, note.title, note.content, start, allDay = true)
                noteDao.markSynced(note.id, remoteId)
            }
        }
        // 3) 마감일 있는 할 일 → 종일/시간 이벤트(allDay 플래그).
        for (task in taskDao.getTasksToPush()) {
            val start = task.dueDate ?: continue
            val remoteId = task.remoteId
            if (remoteId == null) {
                val eventId = api.insertEvent(token, task.text, "", start, allDay = task.allDay)
                taskDao.markSynced(task.id, eventId)
            } else {
                api.updateEvent(token, remoteId, task.text, "", start, allDay = task.allDay)
                taskDao.markSynced(task.id, remoteId)
            }
        }
    }
}

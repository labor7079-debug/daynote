package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android 동기화 매니저.
 *
 * - 로그인/인가: `rememberGoogleCalendarSignIn` 이 받은 액세스 토큰을 [onAccessToken] 으로 전달.
 * - 동기화(3-B2, 현재): **한 방향 push** — 날짜 있는 메모를 구글 캘린더 종일 이벤트로 생성/수정/삭제.
 *   (캘린더→앱 pull, 할 일 동기화는 다음 슬라이스.) 충돌은 추후 서버 타임스탬프 기준으로 확장.
 */
class AndroidCalendarSyncManager(
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
        _state.value = SyncState.SignedIn("구글 계정")
    }

    fun onAuthError(message: String) {
        _state.value = SyncState.Error(message)
    }

    override suspend fun signOut() {
        accessToken = null
        _state.value = SyncState.SignedOut
    }

    override suspend fun syncNow() {
        val token = accessToken
        if (token == null) {
            _state.value = SyncState.Error("먼저 구글 로그인이 필요합니다.")
            return
        }
        _state.value = SyncState.Syncing
        try {
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
            _state.value = SyncState.Synced(nowMillis())
        } catch (e: CalendarApiException) {
            if (e.code == 401) {
                accessToken = null
                _state.value = SyncState.Error("세션이 만료됐습니다. 다시 로그인해 주세요.")
            } else {
                _state.value = SyncState.Error(e.message ?: "동기화 실패")
            }
        } catch (e: Exception) {
            _state.value = SyncState.Error(e.message ?: "동기화 실패")
        }
    }
}

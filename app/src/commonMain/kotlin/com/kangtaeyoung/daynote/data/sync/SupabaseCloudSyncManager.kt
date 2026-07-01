package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.security.SecureStore
import com.kangtaeyoung.daynote.data.sync.supabase.AuthSession
import com.kangtaeyoung.daynote.data.sync.supabase.SupabaseException
import com.kangtaeyoung.daynote.data.sync.supabase.SupabaseSyncClient
import com.kangtaeyoung.daynote.data.sync.supabase.toEntity
import com.kangtaeyoung.daynote.data.sync.supabase.toRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Supabase 기반 클라우드 동기화(Phase 6-B). Ktor 라 Android·Desktop 양쪽 같은 코드.
 *
 * 동기화 선정은 **워터마크 델타**(updatedAt > lastSync)다 — syncStatus 와 독립이라 캘린더 동기화와
 * 간섭하지 않고, 소프트삭제 tombstone 도 함께 올라간다. 충돌은 [resolveByUpdatedAt](last-write-wins).
 */
class SupabaseCloudSyncManager(
    private val settings: SettingsRepository,
    private val secure: SecureStore,
    private val client: SupabaseSyncClient,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
) : CloudSyncManager {

    private val _state = MutableStateFlow<CloudSyncState>(CloudSyncState.Disabled)
    override val state: StateFlow<CloudSyncState> = _state.asStateFlow()

    private var config: SupabaseConfig = SupabaseConfig.EMPTY
    private var session: AuthSession? = loadSession()

    /** 자동 동기화와 수동 "지금 동기화" 가 겹쳐 이중 push 되지 않도록 직렬화한다. */
    private val syncMutex = Mutex()

    override fun isConfigured(): Boolean = config.isValid

    override suspend fun refreshState() {
        config = settings.getSupabaseConfig()
        session = loadSession()
        _state.value = when {
            !settings.isCloudSyncEnabled() -> CloudSyncState.Disabled
            !config.isValid -> CloudSyncState.NeedsConfig
            session == null -> CloudSyncState.SignedOut
            else -> CloudSyncState.SignedIn(session!!.userId)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = authThen {
        client.signIn(config, email, password)
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = authThen {
        client.signUp(config, email, password)
    }

    private suspend fun authThen(block: suspend () -> AuthSession): Result<Unit> {
        config = settings.getSupabaseConfig()
        if (!config.isValid) {
            _state.value = CloudSyncState.NeedsConfig
            return Result.failure(IllegalStateException("Supabase URL/anon key 를 먼저 입력하세요."))
        }
        return runCatching {
            val s = block()
            storeSession(s)
            session = s
            _state.value = CloudSyncState.SignedIn(s.userId)
        }.onSuccess { syncNow() }
            .onFailure { _state.value = CloudSyncState.Error(it.message ?: "인증 실패") }
    }

    override suspend fun signOut() {
        clearSession()
        session = null
        _state.value = if (settings.isCloudSyncEnabled() && config.isValid) {
            CloudSyncState.SignedOut
        } else {
            CloudSyncState.Disabled
        }
    }

    override suspend fun syncNow() = syncMutex.withLock {
        config = settings.getSupabaseConfig()
        when {
            !settings.isCloudSyncEnabled() -> { _state.value = CloudSyncState.Disabled; return@withLock }
            !config.isValid -> { _state.value = CloudSyncState.NeedsConfig; return@withLock }
            session == null -> { _state.value = CloudSyncState.SignedOut; return@withLock }
        }
        _state.value = CloudSyncState.Syncing
        runCatching { doSync() }
            .onSuccess { _state.value = CloudSyncState.Synced(nowMillis()) }
            .onFailure { e ->
                if (e is SupabaseException && e.status == 401) {
                    // 갱신도 실패 → 재로그인 필요.
                    clearSession(); session = null
                    _state.value = CloudSyncState.SignedOut
                } else {
                    _state.value = CloudSyncState.Error(e.message ?: "동기화 실패")
                }
            }
        Unit
    }

    /** 양방향 1회. push(델타) → pull(델타·충돌 해소) → 워터마크 전진. */
    private suspend fun doSync() {
        val userId = session!!.userId
        val since = settings.getCloudLastSync()
        var maxSeen = since

        // ① push: since 이후 변경된 로컬 행(삭제 tombstone 포함)
        val localNotes = noteDao.getNotesModifiedSince(since)
        val localTasks = taskDao.getTasksModifiedSince(since)
        withAuth { token -> client.pushNotes(config, token, localNotes.map { it.toRow(userId) }) }
        withAuth { token -> client.pushTasks(config, token, localTasks.map { it.toRow(userId) }) }
        localNotes.forEach { if (it.updatedAt > maxSeen) maxSeen = it.updatedAt }
        localTasks.forEach { if (it.updatedAt > maxSeen) maxSeen = it.updatedAt }

        // ② pull: since 이후 원격 변경 → 충돌 해소 후 로컬 반영
        val remoteNotes = withAuth { token -> client.pullNotes(config, token, since) }
        for (row in remoteNotes) {
            val local = noteDao.getByIdRaw(row.id)
            if (local == null || resolveByUpdatedAt(local.updatedAt, row.updatedAt) == ConflictWinner.REMOTE) {
                noteDao.upsert(row.toEntity(local))
            }
            if (row.updatedAt > maxSeen) maxSeen = row.updatedAt
        }
        val remoteTasks = withAuth { token -> client.pullTasks(config, token, since) }
        for (row in remoteTasks) {
            val local = taskDao.getByIdRaw(row.id)
            if (local == null || resolveByUpdatedAt(local.updatedAt, row.updatedAt) == ConflictWinner.REMOTE) {
                taskDao.upsert(row.toEntity(local))
            }
            if (row.updatedAt > maxSeen) maxSeen = row.updatedAt
        }

        // ③ 워터마크 전진(strict > 이므로 방금 처리한 행은 다음에 다시 안 잡힘 → echo 방지)
        settings.setCloudLastSync(maxSeen)
    }

    /**
     * 액세스 토큰으로 [block] 실행. 401 이면 refresh 토큰으로 1회 갱신 후 재시도.
     * 갱신 실패 시 401 SupabaseException 을 그대로 던져 상위에서 재로그인 처리.
     */
    private suspend fun <T> withAuth(block: suspend (token: String) -> T): T {
        val current = session ?: throw SupabaseException("로그인이 필요합니다.", status = 401)
        return try {
            block(current.accessToken)
        } catch (e: SupabaseException) {
            if (e.status != 401) throw e
            val refreshed = runCatching { client.refresh(config, current.refreshToken) }.getOrNull()
                ?: throw SupabaseException("세션 만료 — 다시 로그인하세요.", status = 401)
            storeSession(refreshed); session = refreshed
            block(refreshed.accessToken)
        }
    }

    // --- 세션 영속(안전 저장소) ---

    private fun loadSession(): AuthSession? {
        val access = secure.get(KEY_ACCESS) ?: return null
        val refresh = secure.get(KEY_REFRESH) ?: return null
        val uid = secure.get(KEY_USER) ?: return null
        return AuthSession(access, refresh, uid)
    }

    private fun storeSession(s: AuthSession) {
        secure.put(KEY_ACCESS, s.accessToken)
        secure.put(KEY_REFRESH, s.refreshToken)
        secure.put(KEY_USER, s.userId)
    }

    private fun clearSession() {
        secure.remove(KEY_ACCESS); secure.remove(KEY_REFRESH); secure.remove(KEY_USER)
    }

    private companion object {
        const val KEY_ACCESS = "sb_access_token"
        const val KEY_REFRESH = "sb_refresh_token"
        const val KEY_USER = "sb_user_id"
    }
}

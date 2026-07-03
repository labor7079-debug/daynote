package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.data.local.dao.SettingDao
import com.kangtaeyoung.daynote.data.local.entity.AppSettingEntity
import com.kangtaeyoung.daynote.data.sync.SupabaseConfig
import com.kangtaeyoung.daynote.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 앱 환경설정(영속) 접근. 화면 테마 + 구글 캘린더 토글 + 클라우드(Supabase) 동기화 토글·접속 설정. */
interface SettingsRepository {
    // 화면 테마(시스템/라이트/다크)
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    // 구글 캘린더 동기화 토글(Phase 3)
    fun observeSyncEnabled(): Flow<Boolean>
    suspend fun setSyncEnabled(enabled: Boolean)

    // 달력에 표시할 구글 캘린더(공유받은 캘린더 포함) — 체크된 캘린더 id 집합
    fun observeVisibleGoogleCalendarIds(): Flow<Set<String>>
    suspend fun getVisibleGoogleCalendarIds(): Set<String>
    suspend fun setVisibleGoogleCalendarIds(ids: Set<String>)

    // AI 제목 자동생성 토글(저장 시 제목 비면 자동 생성) — 기본 켜짐
    fun observeAutoTitle(): Flow<Boolean>
    suspend fun isAutoTitleEnabled(): Boolean
    suspend fun setAutoTitle(enabled: Boolean)

    // 할 일 마감 알림 토글 — 기본 켜짐
    fun observeRemindersEnabled(): Flow<Boolean>
    suspend fun isRemindersEnabled(): Boolean
    suspend fun setRemindersEnabled(enabled: Boolean)

    // 클라우드(Supabase) 동기화 토글 + 접속 설정(Phase 6)
    fun observeCloudSyncEnabled(): Flow<Boolean>
    suspend fun isCloudSyncEnabled(): Boolean
    suspend fun setCloudSyncEnabled(enabled: Boolean)

    fun observeSupabaseConfig(): Flow<SupabaseConfig>
    suspend fun getSupabaseConfig(): SupabaseConfig
    suspend fun setSupabaseConfig(config: SupabaseConfig)

    /** 클라우드 동기화 워터마크(이 시각 이후 변경분만 push/pull). */
    suspend fun getCloudLastSync(): Long
    suspend fun setCloudLastSync(millis: Long)
}

class SettingsRepositoryImpl(
    private val dao: SettingDao,
) : SettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> =
        dao.observe(KEY_THEME_MODE).map { ThemeMode.from(it) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dao.put(AppSettingEntity(KEY_THEME_MODE, mode.name))
    }

    override fun observeSyncEnabled(): Flow<Boolean> =
        dao.observe(KEY_SYNC_ENABLED).map { it == "true" }

    override suspend fun setSyncEnabled(enabled: Boolean) {
        dao.put(AppSettingEntity(KEY_SYNC_ENABLED, enabled.toString()))
    }

    // 캘린더 id 는 이메일/그룹 주소 형태라 줄바꿈을 포함하지 않는다 → 개행 구분 문자열로 저장.
    override fun observeVisibleGoogleCalendarIds(): Flow<Set<String>> =
        dao.observe(KEY_GOOGLE_VISIBLE_CALS).map { it.toIdSet() }

    override suspend fun getVisibleGoogleCalendarIds(): Set<String> =
        dao.get(KEY_GOOGLE_VISIBLE_CALS).toIdSet()

    override suspend fun setVisibleGoogleCalendarIds(ids: Set<String>) {
        dao.put(AppSettingEntity(KEY_GOOGLE_VISIBLE_CALS, ids.joinToString("\n")))
    }

    private fun String?.toIdSet(): Set<String> =
        this?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()

    // 기본 켜짐: 값이 없으면(=미설정) true, 명시적으로 "false" 일 때만 끔(알림 토글과 같은 정책).
    // 제목 없이 저장하면 AI(키 없으면 본문 첫 줄)로 자동 생성된다.
    override fun observeAutoTitle(): Flow<Boolean> =
        dao.observe(KEY_AUTO_TITLE).map { it != "false" }

    override suspend fun isAutoTitleEnabled(): Boolean =
        dao.get(KEY_AUTO_TITLE) != "false"

    override suspend fun setAutoTitle(enabled: Boolean) {
        dao.put(AppSettingEntity(KEY_AUTO_TITLE, enabled.toString()))
    }

    // 기본 켜짐: 값이 없으면(=미설정) true, 명시적으로 "false" 일 때만 끔.
    override fun observeRemindersEnabled(): Flow<Boolean> =
        dao.observe(KEY_REMINDERS).map { it != "false" }

    override suspend fun isRemindersEnabled(): Boolean =
        dao.get(KEY_REMINDERS) != "false"

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dao.put(AppSettingEntity(KEY_REMINDERS, enabled.toString()))
    }

    override fun observeCloudSyncEnabled(): Flow<Boolean> =
        dao.observe(KEY_CLOUD_SYNC_ENABLED).map { it == "true" }

    override suspend fun isCloudSyncEnabled(): Boolean =
        dao.get(KEY_CLOUD_SYNC_ENABLED) == "true"

    override suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dao.put(AppSettingEntity(KEY_CLOUD_SYNC_ENABLED, enabled.toString()))
    }

    override fun observeSupabaseConfig(): Flow<SupabaseConfig> =
        dao.observe(KEY_SUPABASE_URL).map { url ->
            SupabaseConfig(url = url.orEmpty(), anonKey = dao.get(KEY_SUPABASE_ANON_KEY).orEmpty())
        }

    override suspend fun getSupabaseConfig(): SupabaseConfig = SupabaseConfig(
        url = dao.get(KEY_SUPABASE_URL).orEmpty(),
        anonKey = dao.get(KEY_SUPABASE_ANON_KEY).orEmpty(),
    )

    override suspend fun setSupabaseConfig(config: SupabaseConfig) {
        dao.put(AppSettingEntity(KEY_SUPABASE_URL, config.url.trim()))
        dao.put(AppSettingEntity(KEY_SUPABASE_ANON_KEY, config.anonKey.trim()))
    }

    override suspend fun getCloudLastSync(): Long = dao.get(KEY_CLOUD_LAST_SYNC)?.toLongOrNull() ?: 0L

    override suspend fun setCloudLastSync(millis: Long) {
        dao.put(AppSettingEntity(KEY_CLOUD_LAST_SYNC, millis.toString()))
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SYNC_ENABLED = "sync_enabled"
        const val KEY_GOOGLE_VISIBLE_CALS = "google_visible_calendars"
        const val KEY_AUTO_TITLE = "auto_title"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        const val KEY_SUPABASE_URL = "supabase_url"
        const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        const val KEY_CLOUD_LAST_SYNC = "cloud_last_sync"
    }
}

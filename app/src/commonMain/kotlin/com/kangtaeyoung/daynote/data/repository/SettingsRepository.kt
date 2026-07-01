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
        const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        const val KEY_SUPABASE_URL = "supabase_url"
        const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        const val KEY_CLOUD_LAST_SYNC = "cloud_last_sync"
    }
}

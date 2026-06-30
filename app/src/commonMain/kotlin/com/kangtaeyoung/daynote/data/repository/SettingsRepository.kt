package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.data.local.dao.SettingDao
import com.kangtaeyoung.daynote.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 앱 환경설정(영속) 접근. 현재는 동기화 토글. 추후 테마 등도 여기에 얹는다. */
interface SettingsRepository {
    fun observeSyncEnabled(): Flow<Boolean>
    suspend fun setSyncEnabled(enabled: Boolean)
}

class SettingsRepositoryImpl(
    private val dao: SettingDao,
) : SettingsRepository {

    override fun observeSyncEnabled(): Flow<Boolean> =
        dao.observe(KEY_SYNC_ENABLED).map { it == "true" }

    override suspend fun setSyncEnabled(enabled: Boolean) {
        dao.put(AppSettingEntity(KEY_SYNC_ENABLED, enabled.toString()))
    }

    private companion object {
        const val KEY_SYNC_ENABLED = "sync_enabled"
    }
}

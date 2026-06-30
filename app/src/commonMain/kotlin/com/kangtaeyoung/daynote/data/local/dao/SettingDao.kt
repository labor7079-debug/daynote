package com.kangtaeyoung.daynote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kangtaeyoung.daynote.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

/** 키-값 환경설정 접근. 값은 문자열로 저장하고 상위에서 형 변환한다. */
@Dao
interface SettingDao {

    @Upsert
    suspend fun put(setting: AppSettingEntity)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?
}

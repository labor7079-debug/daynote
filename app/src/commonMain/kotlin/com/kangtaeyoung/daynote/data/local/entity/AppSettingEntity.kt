package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 키-값 환경설정(동기화 토글 등). DataStore 대신 기존 Room을 재사용해 의존성을 늘리지 않는다. */
@Entity(tableName = "settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

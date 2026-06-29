package com.kangtaeyoung.daynote.data.local.entity

/**
 * 동기화 상태 값. Phase 6 동기화 계층 대비.
 *
 * Room 컬럼은 단순 String 으로 저장해(타입 컨버터 불필요) 마이그레이션 부담을 줄인다.
 */
object SyncStatus {
    const val LOCAL_ONLY = "LOCAL_ONLY"
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
}

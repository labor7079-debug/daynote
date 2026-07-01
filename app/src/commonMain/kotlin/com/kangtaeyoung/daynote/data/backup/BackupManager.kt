package com.kangtaeyoung.daynote.data.backup

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import kotlinx.serialization.json.Json

/** 복원 결과 요약. */
data class ImportResult(val notes: Int, val tasks: Int)

/**
 * 로컬 백업/복원 — 메모·할 일 전체를 JSON 문자열로 내보내고, 같은 포맷을 읽어 upsert 로 복원한다.
 * 파일 선택/저장(플랫폼 의존)은 UI 레이어의 expect/actual([rememberBackupControls])이 담당한다.
 * 복원은 id 기준 upsert 라 같은 항목은 덮어쓰고 새 항목은 추가된다(중복 생성 없음).
 */
class BackupManager(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        val file = BackupFile(
            format = BackupFile.FORMAT,
            exportedAt = nowMillis(),
            notes = noteDao.getAllRaw().map { it.toBackup() },
            tasks = taskDao.getAllRaw().map { it.toBackup() },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    suspend fun importJson(text: String): ImportResult {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.format == BackupFile.FORMAT) { "DayNote 백업 파일이 아닙니다." }
        if (file.notes.isNotEmpty()) noteDao.upsertAll(file.notes.map { it.toEntity() })
        if (file.tasks.isNotEmpty()) taskDao.upsertAll(file.tasks.map { it.toEntity() })
        return ImportResult(file.notes.size, file.tasks.size)
    }
}

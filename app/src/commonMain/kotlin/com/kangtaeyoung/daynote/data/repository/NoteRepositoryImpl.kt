package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.core.randomUuid
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import com.kangtaeyoung.daynote.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * [NoteRepository] 의 로컬(Room) 구현. 진실의 원천은 Room 이며, 여기서 엔티티↔도메인 매핑과
 * id·타임스탬프 규칙, 검색어 FTS 정제를 담당한다.
 */
class NoteRepositoryImpl(
    private val dao: NoteDao,
    private val changes: LocalChangeNotifier,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNotesByDate(startOfDay: Long, endOfDay: Long): Flow<List<Note>> =
        dao.observeByDateRange(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }

    override fun observeUndatedNotes(): Flow<List<Note>> =
        dao.observeUndated().map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: String): Flow<Note?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getNote(id: String): Note? = dao.getById(id)?.toDomain()

    override suspend fun addNote(title: String, content: String, date: Long?): Note {
        val now = nowMillis()
        val note = Note(
            id = randomUuid(),
            title = title,
            content = content,
            date = date,
            isPinned = false,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(note.toEntity())
        changes.notifyChanged()
        return note
    }

    override suspend fun updateNote(note: Note) {
        // 동기화 메타(remoteId/createdAt 등)는 보존하고, 변경 사실만 PENDING 으로 표시한다.
        // (toEntity 매퍼는 remoteId 를 null 로 만들기 때문에 여기선 기존 엔티티를 복사한다.)
        val existing = dao.getById(note.id) ?: return
        dao.upsert(
            existing.copy(
                title = note.title,
                content = note.content,
                date = note.date,
                isPinned = note.isPinned,
                updatedAt = nowMillis(),
                syncStatus = SyncStatus.PENDING,
            ),
        )
        changes.notifyChanged()
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        val current = dao.getById(id) ?: return
        dao.upsert(current.copy(isPinned = pinned, updatedAt = nowMillis()))
        changes.notifyChanged()
    }

    override suspend fun deleteNote(id: String) {
        dao.softDelete(id, nowMillis())
        changes.notifyChanged()
    }

    override suspend fun restoreNote(id: String) {
        dao.restore(id, nowMillis())
        changes.notifyChanged()
    }

    override fun search(query: String): Flow<List<Note>> {
        val match = toFtsMatch(query) ?: return flowOf(emptyList())
        return dao.search(match).map { list -> list.map { it.toDomain() } }
    }
}

/**
 * 사용자 입력을 FTS4 MATCH 식으로 정제한다.
 * - FTS 연산자로 해석될 문자를 제거하고 공백으로 토큰화
 * - 각 토큰에 접두 와일드카드(`*`)를 붙여 부분 입력에도 매칭(예: "캘린" → "캘린*")
 * - 빈 입력은 null → 호출부에서 빈 결과 처리
 */
internal fun toFtsMatch(raw: String): String? {
    val cleaned = raw.replace(Regex("[\"*()^:\\-]"), " ")
    val tokens = cleaned.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" ") { "$it*" }
}

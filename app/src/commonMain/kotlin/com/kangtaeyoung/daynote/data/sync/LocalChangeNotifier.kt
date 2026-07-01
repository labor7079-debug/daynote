package com.kangtaeyoung.daynote.data.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 로컬 데이터 변경 신호(자동 동기화 트리거). 레포지토리는 쓰기 후 [notifyChanged] 만 부르고
 * 동기화의 존재를 모른다(설계원칙 4). [AutoSyncCoordinator] 가 이 신호를 디바운스해 syncNow 를 부른다.
 *
 * 버퍼 1 + DROP_OLDEST 라 [notifyChanged] 는 절대 suspend 하지 않는다 → 쓰기 경로를 막지 않는다.
 */
class LocalChangeNotifier {
    private val _changes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 코디네이터가 구독하는 변경 스트림. */
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    /** 로컬 메모/할 일이 바뀌었음을 알린다(쓰기 후 호출). */
    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }
}

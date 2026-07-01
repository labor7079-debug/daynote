package com.kangtaeyoung.daynote.data.sync

/**
 * 같은 레코드를 두 기기에서 고쳤을 때 누구를 살릴지(CLAUDE.md §6 충돌 처리).
 *
 * 처음에는 "마지막 수정 우선(last-write-wins)"으로 단순하게 시작한다.
 *
 * ⚠️ updatedAt 은 클라이언트 로컬 시각이라 기기 간 시계 오차(clock skew)에 취약하다.
 * 6-B 에서 비교 기준을 서버 타임스탬프(또는 논리적 버전)로 옮겨 "느린 시계가 최신 편집을 덮어쓰는"
 * 사고를 막는다.
 */
enum class ConflictWinner { LOCAL, REMOTE }

/**
 * 로컬/원격의 updatedAt 을 비교해 승자를 고른다. 같으면 LOCAL 유지(불필요한 덮어쓰기 방지).
 */
fun resolveByUpdatedAt(localUpdatedAt: Long, remoteUpdatedAt: Long): ConflictWinner =
    if (remoteUpdatedAt > localUpdatedAt) ConflictWinner.REMOTE else ConflictWinner.LOCAL

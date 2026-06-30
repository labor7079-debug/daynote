package com.kangtaeyoung.daynote.core

/** 클라이언트 생성 UUID(동기화 대비) — 플랫폼 구현으로 격리한다. */
expect fun randomUuid(): String

/** 현재 시각(epoch millis). createdAt/updatedAt/소프트삭제 타임스탬프에 쓴다. */
expect fun nowMillis(): Long

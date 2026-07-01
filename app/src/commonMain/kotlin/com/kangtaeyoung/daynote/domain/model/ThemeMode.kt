package com.kangtaeyoung.daynote.domain.model

/** 화면 테마 선택(영속). [SYSTEM]=기기 설정 따름, [LIGHT]/[DARK]=강제. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** 저장된 문자열 → [ThemeMode]. 미설정·미인식 값은 [SYSTEM]. */
        fun from(raw: String?): ThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

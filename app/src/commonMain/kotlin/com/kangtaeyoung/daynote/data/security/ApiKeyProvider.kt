package com.kangtaeyoung.daynote.data.security

/**
 * OpenAI API 키 저장 추상화(CLAUDE.md 4-B). 키는 코드에 하드코딩하지 않고 플랫폼 안전 저장소에 둔다.
 * - Android: EncryptedSharedPreferences(암호화).
 * - Desktop: 사용자 홈(`~/.daynote`)의 권한 제한 파일.
 *
 * UI·Repository 는 이 인터페이스만 본다(설계원칙 4).
 */
interface ApiKeyProvider {
    fun openAiKey(): String?
    fun setOpenAiKey(key: String)
    fun clear()
    fun hasKey(): Boolean = !openAiKey().isNullOrBlank()
}

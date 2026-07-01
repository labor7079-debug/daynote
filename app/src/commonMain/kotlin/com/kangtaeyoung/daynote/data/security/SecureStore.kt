package com.kangtaeyoung.daynote.data.security

/**
 * 범용 키-값 안전 저장소. 민감한 토큰(예: Supabase access/refresh token, user id)을 둔다.
 * - Android: EncryptedSharedPreferences(암호화).
 * - Desktop: 사용자 홈의 권한 제한 파일.
 *
 * [ApiKeyProvider] 와 같은 보안 메커니즘을 쓰되, 임의 키를 다룰 수 있게 일반화했다.
 */
interface SecureStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

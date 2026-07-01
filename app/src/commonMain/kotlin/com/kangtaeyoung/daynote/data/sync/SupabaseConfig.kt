package com.kangtaeyoung.daynote.data.sync

/**
 * Supabase 접속 설정. URL·anon key 는 비밀이 아니다(anon key 는 공개용이며 RLS 가 데이터를 보호).
 * 사용자별 인증 토큰(로그인 후 발급)만 민감 정보이며, 그건 6-B 에서 안전 저장소에 둔다.
 *
 * 코드 하드코딩 대신 설정 화면에서 입력 → settings 테이블에 영속(SettingsRepository).
 */
data class SupabaseConfig(
    val url: String,
    val anonKey: String,
) {
    val isValid: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()

    companion object {
        val EMPTY = SupabaseConfig(url = "", anonKey = "")
    }
}

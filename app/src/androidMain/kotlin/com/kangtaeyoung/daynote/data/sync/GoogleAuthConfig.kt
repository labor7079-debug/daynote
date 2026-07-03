package com.kangtaeyoung.daynote.data.sync

/**
 * 구글 인증/인가 설정(Android 전용).
 *
 * [ANDROID_CLIENT_ID] 는 Google Cloud Console에서 발급한 **Android OAuth 클라이언트 ID**다.
 * (패키지 `com.kangtaeyoung.daynote` + 디버그 SHA-1 `EB:77:ED:51:C0:AA:F5:27:17:2A:44:21:CB:69:56:9D:2D:AC:35:04` 로 바인딩)
 * Android OAuth 클라이언트 ID는 비밀값이 아니며(APK에 내장되는 게 정상, SHA-1 바인딩으로 보호) 커밋해도 된다.
 *
 * 캘린더 접근은 Authorization API(`Identity.getAuthorizationClient`)로 [CALENDAR_SCOPE] 를 요청해
 * 액세스 토큰을 받는다. 이 방식은 위 Android 클라이언트로 동작하며 별도 Web 클라이언트 ID가 필요 없다.
 */
object GoogleAuthConfig {
    const val ANDROID_CLIENT_ID =
        "10882027046-lo01dp4ulcmcepe1a1vsvmc3fg2vrudt.apps.googleusercontent.com"

    /** 인가 요청에 쓰는 전체 범위(플랫폼 공용 [GOOGLE_CALENDAR_SCOPES] 재사용).
     *  범위가 늘면 기존 사용자는 동의 화면이 한 번 더 뜬다. */
    val SCOPES = GOOGLE_CALENDAR_SCOPES
}

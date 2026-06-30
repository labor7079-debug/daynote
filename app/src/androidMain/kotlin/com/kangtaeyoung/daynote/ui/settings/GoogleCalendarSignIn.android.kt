package com.kangtaeyoung.daynote.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kangtaeyoung.daynote.data.sync.AndroidCalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.GoogleAuthConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import org.koin.compose.koinInject

/**
 * Android: Google Identity Authorization API 로 캘린더 액세스 토큰을 요청한다.
 *
 * 1) AuthorizationRequest(캘린더 scope) 생성 → authorize()
 * 2) 동의가 필요하면(hasResolution) PendingIntent 를 IntentSender 런처로 띄움
 * 3) 결과 Intent 에서 AuthorizationResult.accessToken 추출 → 매니저에 전달
 *
 * Android OAuth 클라이언트(패키지+SHA-1)로 매칭되며, 온디바이스 토큰이라 Web 클라이언트 ID 불필요.
 */
@Composable
actual fun rememberGoogleCalendarSignIn(): () -> Unit {
    val context = LocalContext.current
    val manager = koinInject<CalendarSyncManager>() as? AndroidCalendarSyncManager

    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        runCatching {
            Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data)
        }.onSuccess { authResult ->
            manager?.onAccessToken(authResult.accessToken)
        }.onFailure { e ->
            manager?.onAuthError(e.message ?: "인증 처리 실패")
        }
    }

    return remember(context, manager, resolutionLauncher) {
        action@{
            val activity = context as? Activity
            if (activity == null || manager == null) {
                manager?.onAuthError("인증 컨텍스트를 찾을 수 없습니다.")
                return@action
            }
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GoogleAuthConfig.CALENDAR_SCOPE)))
                .build()
            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { authResult ->
                    val pendingIntent = authResult.pendingIntent
                    if (authResult.hasResolution() && pendingIntent != null) {
                        runCatching {
                            resolutionLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                            )
                        }.onFailure { e -> manager.onAuthError(e.message ?: "동의 화면 실행 실패") }
                    } else {
                        // 이미 권한이 있는 경우 — 토큰 바로 반환.
                        manager.onAccessToken(authResult.accessToken)
                    }
                }
                .addOnFailureListener { e ->
                    manager.onAuthError(e.message ?: "구글 인가 실패")
                }
        }
    }
}

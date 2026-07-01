package com.kangtaeyoung.daynote.ui.ai

import androidx.compose.runtime.Composable

/**
 * Phase 4-A — 생성형 AI(ChatGPT) 연동의 첫 단계: 메모 텍스트를 외부 AI 도구로 "푸쉬"한다.
 *
 * 설계원칙 3·6에 따라 플랫폼 의존(Android Sharesheet)은 `expect/actual` 뒤로 격리한다.
 * - Android: `ACTION_SEND` Sharesheet 로 ChatGPT 앱 등에 텍스트 전달.
 * - Desktop: Sharesheet 가 없으므로 클립보드에 복사 → 사용자가 ChatGPT 웹/앱에 붙여넣는다.
 *
 * [actionLabel] 은 버튼 문구, [confirmMessage] 는 동작 직후 보여줄 스낵바 문구(없으면 빈 문자열 —
 * Android 는 시트가 떠서 별도 확인이 불필요).
 */
class AiShare(
    val actionLabel: String,
    val confirmMessage: String,
    val share: (String) -> Unit,
)

/**
 * 현재 플랫폼에 맞는 [AiShare] 를 제공한다. 반환된 객체의 [AiShare.share] 에 메모 텍스트를 넘기면
 * Android 는 공유 시트를, Desktop 은 클립보드 복사를 수행한다.
 */
@Composable
expect fun rememberAiShare(): AiShare

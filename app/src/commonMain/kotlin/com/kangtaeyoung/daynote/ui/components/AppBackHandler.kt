package com.kangtaeyoung.daynote.ui.components

import androidx.compose.runtime.Composable

/**
 * 시스템 뒤로가기 가로채기 — Android 전용 API(`androidx.activity.compose.BackHandler`)라
 * expect/actual 로 격리한다(설계원칙 3/6). 데스크톱은 시스템 뒤로가 없어 no-op
 * (데스크톱의 마우스 옆버튼·Alt+← 는 NavHost 쪽 처리를 그대로 탄다).
 * CMP 공용 BackHandler 는 1.9+ 라 현재 버전(1.8.2)에선 쓸 수 없다.
 */
@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)

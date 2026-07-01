package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.runtime.Composable

/**
 * 백업 파일 저장/열기(플랫폼 의존)를 expect/actual 뒤로 격리(설계원칙 3·6).
 * - Android: Storage Access Framework(문서 만들기/열기)로 사용자가 위치를 고른다.
 * - Desktop: AWT FileDialog 로 저장/열기.
 */
class BackupControls(
    /** JSON 을 파일로 저장(사용자가 위치·이름 선택). */
    val exportTo: (json: String) -> Unit,
    /** 파일을 열어 내용을 읽고 onImported 로 전달. */
    val importFrom: () -> Unit,
)

/**
 * [onImported] : 사용자가 고른 파일 내용(문자열)을 전달받아 복원을 시작한다.
 * [onResult]   : 취소·오류 등 상태 메시지.
 */
@Composable
expect fun rememberBackupControls(
    onImported: (String) -> Unit,
    onResult: (String) -> Unit,
): BackupControls

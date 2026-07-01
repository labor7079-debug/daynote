package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Desktop: AWT FileDialog 로 백업 JSON 을 저장/열기(추가 의존성 없음). 모달이라 클릭 스레드에서 처리.
 */
@Composable
actual fun rememberBackupControls(
    onImported: (String) -> Unit,
    onResult: (String) -> Unit,
): BackupControls = remember {
    BackupControls(
        exportTo = { json ->
            val dialog = FileDialog(null as Frame?, "백업 저장", FileDialog.SAVE).apply {
                file = "daynote-backup.json"
                isVisible = true
            }
            val dir = dialog.directory
            val name = dialog.file
            if (dir == null || name == null) {
                onResult("내보내기를 취소했어요.")
            } else {
                runCatching { File(dir, name).writeText(json) }
                    .onSuccess { onResult("백업 파일을 저장했어요.") }
                    .onFailure { onResult("내보내기 실패: ${it.message}") }
            }
        },
        importFrom = {
            val dialog = FileDialog(null as Frame?, "백업 불러오기", FileDialog.LOAD).apply {
                isVisible = true
            }
            val dir = dialog.directory
            val name = dialog.file
            if (dir == null || name == null) {
                onResult("가져오기를 취소했어요.")
            } else {
                runCatching { File(dir, name).readText() }
                    .onSuccess { onImported(it) }
                    .onFailure { onResult("가져오기 실패: ${it.message}") }
            }
        },
    )
}

package com.kangtaeyoung.daynote.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android: Storage Access Framework 로 백업 JSON 을 저장/열기. FileProvider·저장소 권한 불필요.
 * 저장할 JSON 은 문서 생성(Uri) 후에 써야 하므로 pending 홀더에 잠시 담아 둔다.
 */
@Composable
actual fun rememberBackupControls(
    onImported: (String) -> Unit,
    onResult: (String) -> Unit,
): BackupControls {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<String>(1) } // 저장 대기 중인 JSON

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pending[0]
        pending[0] = null
        if (uri == null || json == null) {
            onResult("내보내기를 취소했어요.")
        } else {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.encodeToByteArray()) }
                    ?: error("파일을 열 수 없습니다.")
            }.onSuccess { onResult("백업 파일을 저장했어요.") }
                .onFailure { onResult("내보내기 실패: ${it.message}") }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            onResult("가져오기를 취소했어요.")
        } else {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: error("파일을 읽을 수 없습니다.")
            }.onSuccess { onImported(it) }
                .onFailure { onResult("가져오기 실패: ${it.message}") }
        }
    }

    return remember(createLauncher, openLauncher) {
        BackupControls(
            exportTo = { json ->
                pending[0] = json
                createLauncher.launch("daynote-backup.json")
            },
            importFrom = {
                openLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
        )
    }
}

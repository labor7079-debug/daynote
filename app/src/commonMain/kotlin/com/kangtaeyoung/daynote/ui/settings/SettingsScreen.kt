package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.SyncState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val sync = koinInject<CalendarSyncManager>()
    val settings = koinInject<SettingsRepository>()
    val vm = viewModel { SettingsViewModel(sync, settings) }
    val state by vm.state.collectAsState()
    val enabled by vm.syncEnabled.collectAsState()
    val startGoogleSignIn = rememberGoogleCalendarSignIn()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("구글 캘린더 동기화", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.describe(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!vm.syncAvailable) {
                Text(
                    "이 플랫폼에서는 동기화를 지원하지 않습니다(데스크톱).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("동기화 사용", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = enabled, onCheckedChange = vm::setSyncEnabled)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = startGoogleSignIn, enabled = enabled) { Text("구글 로그인") }
                    OutlinedButton(onClick = vm::syncNow, enabled = enabled) { Text("지금 동기화") }
                }
                OutlinedButton(onClick = vm::signOut, enabled = enabled) { Text("로그아웃") }

                if (state is SyncState.NeedsSetup || state is SyncState.Error) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "안내: 실제 동기화는 Google Cloud Console에서 OAuth 자격증명(패키지 com.kangtaeyoung.daynote + SHA-1)을 설정한 뒤 활성화됩니다. (Phase 3-B)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun SyncState.describe(): String = when (this) {
    SyncState.Unavailable -> "상태: 미지원"
    SyncState.NeedsSetup -> "상태: 구글 로그인 필요 (OAuth 설정 후 사용 가능)"
    SyncState.SignedOut -> "상태: 로그아웃됨"
    is SyncState.SignedIn -> "상태: 로그인됨 ($account)"
    SyncState.Syncing -> "상태: 동기화 중…"
    is SyncState.Synced -> "상태: 동기화 완료"
    is SyncState.Error -> "오류: $message"
}

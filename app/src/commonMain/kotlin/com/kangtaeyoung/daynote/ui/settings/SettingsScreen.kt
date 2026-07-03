package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.data.sync.GoogleCalendarInfo
import com.kangtaeyoung.daynote.data.sync.SyncState
import com.kangtaeyoung.daynote.ui.components.parseHexColor
import com.kangtaeyoung.daynote.ui.theme.BackArrowIcon
import com.kangtaeyoung.daynote.domain.model.ThemeMode
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val sync = koinInject<CalendarSyncManager>()
    val settings = koinInject<SettingsRepository>()
    val apiKeys = koinInject<ApiKeyProvider>()
    val cloud = koinInject<CloudSyncManager>()
    val backup = koinInject<com.kangtaeyoung.daynote.data.backup.BackupManager>()
    val vm = viewModel { SettingsViewModel(sync, settings, apiKeys, cloud, backup) }
    val state by vm.state.collectAsState()
    val enabled by vm.syncEnabled.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    val cloudState by vm.cloudState.collectAsState()
    val cloudEnabled by vm.cloudSyncEnabled.collectAsState()
    val cloudBusy by vm.cloudBusy.collectAsState()
    val supabaseConfig by vm.supabaseConfig.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val autoTitle by vm.autoTitle.collectAsState()
    val remindersEnabled by vm.remindersEnabled.collectAsState()
    val backupMsg by vm.backupMsg.collectAsState()
    val googleCalendars by vm.googleCalendars.collectAsState()
    val visibleCalendarIds by vm.visibleCalendarIds.collectAsState()
    val googleCalendarsLoading by vm.googleCalendarsLoading.collectAsState()
    val googleCalendarsMsg by vm.googleCalendarsMsg.collectAsState()
    val startGoogleSignIn = rememberGoogleCalendarSignIn()
    val backupControls = rememberBackupControls(onImported = vm::importBackup, onResult = vm::setBackupMsg)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(BackArrowIcon, contentDescription = "뒤로") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // 가상 키보드가 뜨면 스크롤 영역을 줄여 입력칸이 가려지지 않게
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeSection(
                mode = themeMode,
                onSelect = vm::setThemeMode,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 할 일 마감 알림 토글(시간 지정 할 일의 마감 시각에 알림).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("할 일 마감 알림", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = remindersEnabled, onCheckedChange = vm::setRemindersEnabled)
            }
            Text(
                "시간을 지정한 할 일의 마감 시각에 알림을 보냅니다. (종일 할 일은 알림 없음)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("구글 캘린더 동기화", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.describe(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!vm.syncAvailable) {
                Text(
                    "데스크톱 OAuth 클라이언트가 설정되지 않았습니다 — keystore.properties 에 " +
                        "googleDesktopClientId/googleDesktopClientSecret 를 추가한 빌드에서 사용할 수 있습니다.",
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

                if (enabled) {
                    GoogleCalendarPickSection(
                        calendars = googleCalendars,
                        visibleIds = visibleCalendarIds,
                        loading = googleCalendarsLoading,
                        message = googleCalendarsMsg,
                        onLoad = vm::loadGoogleCalendars,
                        onToggle = vm::setCalendarVisible,
                    )
                }

                if (state is SyncState.NeedsSetup || state is SyncState.Error) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "안내: 실제 동기화는 Google Cloud Console에서 OAuth 자격증명(패키지 com.kangtaeyoung.daynote + SHA-1)을 설정한 뒤 활성화됩니다. (Phase 3-B)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            CloudSyncSection(
                enabled = cloudEnabled,
                statusText = cloudState.describe(),
                signedIn = cloudState is CloudSyncState.SignedIn ||
                    cloudState is CloudSyncState.Syncing ||
                    cloudState is CloudSyncState.Synced,
                busy = cloudBusy,
                url = supabaseConfig.url,
                anonKey = supabaseConfig.anonKey,
                onToggle = vm::setCloudSyncEnabled,
                onSaveConfig = vm::saveSupabaseConfig,
                onSignIn = vm::cloudSignIn,
                onSignUp = vm::cloudSignUp,
                onSignOut = vm::cloudSignOut,
                onSyncNow = vm::cloudSyncNow,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ApiKeySection(
                hasKey = hasApiKey,
                onSave = vm::saveApiKey,
                onClear = vm::clearApiKey,
                autoTitle = autoTitle,
                onAutoTitleChange = vm::setAutoTitle,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            BackupSection(
                message = backupMsg,
                onExport = { vm.buildExport { json -> backupControls.exportTo(json) } },
                onImport = { backupControls.importFrom() },
            )
        }
    }
}

/**
 * 표시할 구글 캘린더 선택 — 구글 캘린더 사이드바("다른 캘린더")처럼 캘린더별 색 점 + 체크박스.
 * 체크한 캘린더(공유받은 것 포함)의 일정이 앱 달력에 읽기 전용으로 표시된다.
 */
@Composable
private fun GoogleCalendarPickSection(
    calendars: List<GoogleCalendarInfo>,
    visibleIds: Set<String>,
    loading: Boolean,
    message: String?,
    onLoad: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text("표시할 캘린더", style = MaterialTheme.typography.titleSmall)
    Text(
        "공유받은 캘린더를 포함해, 체크한 캘린더의 일정이 달력에 표시됩니다(읽기 전용 — 앱에서 수정되지 않습니다).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onLoad, enabled = !loading) {
        Text(if (loading) "불러오는 중…" else if (calendars.isEmpty()) "캘린더 목록 불러오기" else "목록 새로고침")
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    calendars.forEach { cal ->
        val checked = cal.id in visibleIds
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle(cal.id, it) })
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(cal.colorHex) ?: MaterialTheme.colorScheme.outline),
            )
            Text(
                text = if (cal.primary) "${cal.name} (내 캘린더)" else cal.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** 로컬 백업/복원 — 메모·할 일 전체를 파일로 내보내고, 그 파일로 복원한다(오프라인 안전망). */
@Composable
private fun BackupSection(
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Text("백업 / 복원", style = MaterialTheme.typography.titleMedium)
    Text(
        "메모와 할 일 전체를 파일로 저장하고, 그 파일로 되돌립니다. 기기 이전·실수 삭제 대비용입니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onExport) { Text("내보내기") }
        OutlinedButton(onClick = onImport) { Text("가져오기") }
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 화면 테마 선택(시스템/라이트/다크). 선택은 즉시 영속되어 앱 전체에 반영된다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSection(
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.SYSTEM to "시스템",
        ThemeMode.LIGHT to "라이트",
        ThemeMode.DARK to "다크",
    )

    Text("화면 테마", style = MaterialTheme.typography.titleMedium)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
    Text(
        "‘시스템’은 기기 설정을 따르고, ‘라이트/다크’는 항상 그 테마로 고정합니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 클라우드(Supabase) 동기화 — 멀티기기 데이터 일치(Phase 6). URL·anon key 는 비밀이 아니다(RLS 보호).
 * 6-A 골격: 토글·접속 설정·상태 표시. 실제 push/pull 은 6-B.
 */
@Composable
private fun CloudSyncSection(
    enabled: Boolean,
    statusText: String,
    signedIn: Boolean,
    busy: Boolean,
    url: String,
    anonKey: String,
    onToggle: (Boolean) -> Unit,
    onSaveConfig: (String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
    var urlInput by remember(url) { mutableStateOf(url) }
    var keyInput by remember(anonKey) { mutableStateOf(anonKey) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text("클라우드 동기화 (Supabase)", style = MaterialTheme.typography.titleMedium)
    Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "PC·폰·태블릿의 메모/할 일을 같게 만듭니다. 같은 계정으로 로그인한 기기끼리 동기화됩니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("동기화 사용", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }

    if (enabled) {
        // 접속 설정(URL/anon key)
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Supabase URL (https://xxx.supabase.co)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("anon key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { onSaveConfig(urlInput, keyInput) },
            enabled = urlInput.isNotBlank() && keyInput.isNotBlank(),
        ) { Text("접속 설정 저장") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (signedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSyncNow, enabled = !busy) { Text("지금 동기화") }
                OutlinedButton(onClick = onSignOut, enabled = !busy) { Text("로그아웃") }
            }
        } else {
            // 로그인(이메일+비밀번호)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSignIn(email, password) },
                    enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                ) { Text("로그인") }
                OutlinedButton(
                    onClick = { onSignUp(email, password) },
                    enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                ) { Text("회원가입") }
            }
        }
    }
}

private fun CloudSyncState.describe(): String = when (this) {
    CloudSyncState.Disabled -> "상태: 꺼짐"
    CloudSyncState.NeedsConfig -> "상태: Supabase URL/anon key 입력 필요"
    CloudSyncState.SignedOut -> "상태: 로그인 필요"
    is CloudSyncState.SignedIn -> "상태: 로그인됨"
    CloudSyncState.Syncing -> "상태: 동기화 중…"
    is CloudSyncState.Synced -> "상태: 동기화 완료"
    is CloudSyncState.Error -> "오류: $message"
}

/** OpenAI API 키 입력/삭제(Phase 4-B) + 제목 자동생성 토글. 저장된 키 원문은 노출하지 않는다(보안). */
@Composable
private fun ApiKeySection(
    hasKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    autoTitle: Boolean,
    onAutoTitleChange: (Boolean) -> Unit,
) {
    var keyInput by remember { mutableStateOf("") }

    Text("AI (OpenAI)", style = MaterialTheme.typography.titleMedium)
    Text(
        text = if (hasKey) "상태: API 키 저장됨" else "상태: API 키 미설정",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "platform.openai.com 에서 발급한 키(sk-...)를 입력하세요. 키는 기기 안전 저장소에만 보관됩니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = keyInput,
        onValueChange = { keyInput = it },
        label = { Text("OpenAI API 키") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                onSave(keyInput)
                keyInput = ""
            },
            enabled = keyInput.isNotBlank(),
        ) { Text("저장") }
        OutlinedButton(onClick = onClear, enabled = hasKey) { Text("삭제") }
    }

    // 제목 자동생성 — 저장 시 제목이 비어 있으면 AI(또는 본문 첫 줄)로 자동 채움.
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("제목 자동생성", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = autoTitle, onCheckedChange = onAutoTitleChange)
    }
    Text(
        "제목 없이 저장하면 본문을 근거로 AI 가 제목을 자동 생성합니다(기본 켜짐). 키가 없으면 본문 첫 줄을 씁니다. (에디터의 ‘✨ 제목’ 버튼은 토글과 무관하게 항상 사용 가능)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

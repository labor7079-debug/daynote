# 진행 상황 (체크포인트)

> 내일 이어가기 위한 인수인계 메모. 상세 명세는 [CLAUDE.md](CLAUDE.md), 빌드 절차는 [BUILD.md](BUILD.md).
>
> _최종 업데이트: 2026-06-30_

## 현재 위치: Phase 3-B3b 완료 ✅ (앱→구글캘린더 동기화 + 토글 영속화) → 다음 후보는 아래 "다음 작업"

- **앱이 Android·데스크톱 양쪽에서 빌드됨** — `:app:assembleDebug` + `:app:desktopTest` 모두 **BUILD SUCCESSFUL** (테스트 11건 통과).
- **DB 스키마 버전 3** (v1→v2 tasks.allDay, v2→v3 settings 테이블). 마이그레이션은 데이터 보존.
- **구글 동기화 현황**: 로그인/인가(기기 확인됨) + **앱→캘린더 한 방향 push**(메모·할일, 종일/시간) 동작. **캘린더→앱 pull 은 미구현(보류)**. OAuth 클라이언트 ID·SHA-1 등록 완료(`GoogleAuthConfig.kt`). 액세스 토큰은 단기 만료 → 세션마다 로그인 1회 필요.
- **Phase 1 완료**: 메모 CRUD + 마크다운 편집/렌더 분리 + 체크리스트 할일 + 본문 FTS 검색 + Navigation/MVVM 골격.
- **Phase 2 완료**: **캘린더를 홈으로 승격**(startDestination=calendar). 적응형 — 창 너비 <600dp=주 아젠다 / ≥600dp=월 달력(칸 내 메모 미리보기+"+N개 더"), 두 뷰가 선택 날짜 공유. 날짜 탭 → 그날 메모·할일 조회 + 그 자리 추가(날짜 자동 주입)·삭제. 하단탭 Calendar/Memo/To-Do. 커스텀 캘린더(외부 라이브러리 미사용) + `kotlinx-datetime`.
- Phase 0(Android 골격) ✅ → 앱명/패키지 **DayNote / `com.kangtaeyoung.daynote`** 리브랜딩 ✅ → Phase 1 Step1(Room 스키마)·Step2(DAO) 코드+빌드 검증 ✅.
- **PC를 1급 타깃으로 결정** → Compose Multiplatform 전환. **0.5-A(버전 범프+구조 전환+Hilt 제거)·0.5-B(Room KMP)·0.5-C(Koin DI) 모두 완료 → Phase 0.5 종료.**
- **0.5-B 결과**: Room을 `commonMain`(KMP)로 이전 + **`@Fts4` 데스크톱 JVM 스모크 3건 통과**(영문·한글 토큰·소프트삭제 필터).
- **0.5-C 결과**: **Koin DI 도입**(`databaseModule` + `expect/actual platformModule`), `initKoin()`를 Android `DayNoteApp`·Desktop `main()`에서 호출.

## 다음 작업 (새 세션에서 택1)
1. **3-B3c — 캘린더→앱 pull**(보류했던 것): "앱이 만든 항목(remoteId 있는 것)만 양방향"으로. 구글에서 그 이벤트를 수정/삭제하면 앱에 반영. 충돌은 서버 `updated` 타임스탬프 기준 last-write-wins.
2. **무음 재로그인**: 앱 시작 시(토글 ON이면) Authorization API 무음 인가로 토큰 자동 확보 → 세션마다 로그인 안 해도 되게.
3. **테마/다크모드**: `SettingsRepository`에 테마 설정 추가(이미 settings 테이블 있음) + Material3 다크 테마.
4. **Phase 4 (AI 연동)** 시작: 4-A(Android Sharesheet로 ChatGPT 전송, 데스크톱은 클립보드 대체) → 4-B(Ktor로 OpenAI API). Ktor는 여기서 commonMain 도입.

> 진행 전 권장: 기기에서 **DB v3 마이그레이션이 정상(기존 데이터 보존, 크래시 없음)** 인지 1회 확인.

### ⚠️ 방향 전환 결정 (2026-06-29)
**PC(Windows/macOS/Linux)에서도 쓰는 "진짜 데스크톱 앱"을 목표로 확정** → **Compose Multiplatform(KMP)** 으로 전환한다. 코드 자산이 가장 적은 지금 전환(설계원칙 6, CLAUDE.md Phase 0.5). 핵심 귀결:
- **DI: Hilt → Koin** (Hilt는 Android 전용 · 현재 그래프 비어 있어 전환 비용 최소)
- **DB: Room → Room KMP + `BundledSQLiteDriver`** (엔티티/DAO 대부분 재사용)
- **네트워크: Ktor** (Retrofit 제외)
- **구조: `commonMain`/`androidMain`/`desktopMain`** 멀티모듈

## 확정된 핵심 결정 (CLAUDE.md에 반영됨)

- **타깃**: 갤럭시 탭·폴드(Android) **+ 데스크톱(Windows/macOS/Linux)** — Compose Multiplatform 단일 코드 공유
- **패키지/applicationId**: `com.kangtaeyoung.daynote` (앱명 DayNote) · minSdk 26 / compileSdk·targetSdk 35
- **진입점**: 캘린더가 홈 (설계원칙 5)
- **적응형 레이아웃**: 창 너비(`WindowSizeClass`)로 분기 — 폰=주 아젠다 / 태블릿·폴드·PC=월 달력(칸 내 미리보기)
- **에디터**: 마크다운, 편집=`BasicTextField` / 렌더=**CMP 호환** 라이브러리 **분리** (Markwon 불가)
- **DI**: **Koin** (멀티플랫폼) · **DB**: **Room KMP**(진실의 원천, 오프라인 우선), 동기화는 Phase 6
- **AI**: **ChatGPT(OpenAI) 단일** + `AiRepository` 추상화 · 호출은 **Ktor** — Phase 4

## 빌드 방법 (CLI, 이 PC)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # 번들 JDK 21
.\gradlew.bat :app:assembleDebug --console=plain        # Android APK
.\gradlew.bat :app:compileKotlinDesktop --console=plain # 데스크톱 컴파일 검증
.\gradlew.bat :app:run --console=plain                  # 데스크톱 창 실행(GUI)
```
- 시스템 PATH에 java 없음 → JAVA_HOME 반드시 지정
- SDK: `%LOCALAPPDATA%\Android\Sdk` (Android Studio가 설치/관리)
- ⚠️ 셸 안전성 분류기가 일시 장애("temporarily unavailable")면 잠시 후 재시도하면 복구됨.

## 지난 단계: Phase 0.5 (Compose Multiplatform 전환) — 전체 완료 ✅

작은 단위로, 각 단계마다 **Android·데스크톱 양쪽 빌드** 확인:
- ✅ **0.5-A1 버전 범프** — Kotlin 2.0.21→**2.1.20**, KSP→**2.1.20-1.0.31**, CMP 플러그인 **1.8.2** (AGP 8.7.3 / Gradle 8.11.1 유지). Android 빌드 통과.
- ✅ **0.5-A2 구조 전환** — `:app`을 KMP 멀티모듈(`commonMain`/`androidMain`/`desktopMain`)로 재편, CMP 플러그인 적용, **데스크톱 JVM 타깃** 추가. **Hilt 완전 제거**(`DayNoteApp` 삭제, `@AndroidEntryPoint` 제거). 테마는 Android 전용 다이내믹 컬러 제거 후 `commonMain` 공유. 공유 `App()` 컴포저블을 Android(`MainActivity`)·Desktop(`main.kt`) 양쪽이 호출. **`assembleDebug` + `compileKotlinDesktop` 양쪽 BUILD SUCCESSFUL.** Room은 `androidMain`에 임시 유지(`kspAndroid`).
- ✅ **0.5-B Room KMP** — 엔티티/DAO/`AppDatabase`를 `androidMain`→`commonMain` 이전. Room **2.8.4** + `androidx.sqlite:sqlite-bundled 2.6.2`(`BundledSQLiteDriver`) + `ksp { arg("room.generateKotlin","true") }`. KSP를 `kspAndroid`+`kspDesktop` 양 타겟 구성. `@ConstructedBy(AppDatabaseConstructor)` + `expect object … RoomDatabaseConstructor`(actual은 Room이 타겟별 생성). 공통 `buildDatabase(builder)`(드라이버 연결) + 플랫폼별 `appDatabaseBuilder()`(Android=Context 경로 / Desktop=`~/.daynote`). **`@Fts4` 데스크톱 JVM 스모크 테스트 3건 통과**(영문 MATCH·한글 토큰·소프트삭제 제외) → FTS 정상 동작 확인. coroutines-core를 commonMain에 추가(Flow). **`assembleDebug` + `desktopTest` 양쪽 BUILD SUCCESSFUL.**
  - 자산 위치: `app/src/commonMain/.../data/local/{entity,dao,AppDatabase,DatabaseFactory}` + 플랫폼 빌더 `androidMain/desktopMain/.../data/local/DatabaseBuilder.*.kt`. 스모크 테스트: `app/src/desktopTest/.../data/local/FtsSmokeTest.kt`.
  - 참고: Room KMP의 `expect object` 생성자는 "expect/actual classes are in Beta"(KT-61573) **경고만** 뜬다(빌드 정상). 필요 시 `-Xexpect-actual-classes` 로 억제 가능.
- ✅ **0.5-C Koin** — Koin **4.1.0** 도입(`koin-core`+`koin-compose`→commonMain, `koin-androidx-compose`→androidMain). `di/Koin.kt`에 공유 `databaseModule`(AppDatabase→noteDao/taskDao) + `expect fun platformModule()`(actual에서 플랫폼 빌더 제공) + `initKoin(decl)`. Android는 `DayNoteApp : Application`에서 `initKoin { androidContext(...) }`(매니페스트 `android:name=".DayNoteApp"`), Desktop은 `main()`에서 `initKoin()`. **Koin 그래프 스모크 테스트 1건 통과**(`KoinGraphTest`: 격리 `koinApplication`+임시 DB로 AppDatabase·DAO 주입 후 insert/read 왕복). **`assembleDebug` + `desktopTest`(총 4건) BUILD SUCCESSFUL.**
  - 자산 위치: `app/src/commonMain/.../di/Koin.kt` + `androidMain/.../di/Koin.android.kt` + `desktopMain/.../di/Koin.desktop.kt`. Android 진입부 `androidMain/.../DayNoteApp.kt`. 테스트 `desktopTest/.../di/KoinGraphTest.kt`.
  - ⚠️ **버전 이탈 결정(2026-06-30)**: 명세의 Koin **4.2.2는 Kotlin 2.3.0 메타데이터**(+`kotlin-stdlib 2.3.20`)로 컴파일돼 우리 **Kotlin 2.1.20**과 호환 불가(metadata 2.3.0 > expected 2.1.0). → **Koin 4.1.0**(Kotlin 2.1.20 기반)으로 고정. Kotlin/CMP/Room 매트릭스를 흔들지 않는 선택.
  - ⚠️ **transitive 핀**: Koin의 Android 아티팩트가 `androidx.activity 1.12.4`(=compileSdk 36·AGP 8.9.1 요구)를 끌어옴 → `configurations.all { resolutionStrategy.force(...) }`로 **activity 1.10.1**(SDK 35 호환 최신)로 되돌림. `activityCompose` 카탈로그 값도 1.9.3→**1.10.1**. AGP 8.7.3 / compileSdk 35 유지.

> Hilt는 0.5-A2에서 이미 제거됨. DI는 0.5-C에서 Koin으로 도입 완료.

## 그다음: Phase 1 — 로컬 MVP (commonMain에서 작성)
1. ✅ **Room 스키마** — `NoteEntity`/`TaskEntity` + FTS4(`NoteFtsEntity`), `AppDatabase`(v1), `SyncStatus`. (0.5-B에서 KMP로 이전)
2. ✅ **DAO** — `NoteDao`/`TaskDao`: upsert·소프트삭제/복구·날짜범위·미지정 조회, FTS 검색, 할일 토글. `assembleDebug` **BUILD SUCCESSFUL**(KSP가 FTS rowid 조인·`NOT isDone` 토글·전 컬럼 검증 통과).
3. ✅ **Repository + UseCase** — 도메인 모델(`Note`/`Task`) + 매퍼, `NoteRepository`/`TaskRepository`(인터페이스+impl), UseCase 12개. id·타임스탬프·`syncStatus`는 impl이 채우고, 검색어는 `toFtsMatch`로 접두(`*`) 정제(빈 입력=빈 결과). UUID·시각은 `core/Platform.kt` expect/actual(JVM `UUID`/`currentTimeMillis`). Koin `repositoryModule`+`useCaseModule` 등록. **desktopTest 4건 추가 통과**(총 8건). 양쪽 빌드 성공.
4. ✅ **마크다운 에디터** — 편집은 `BasicTextField`(원문), 렌더는 라이브러리로 **분리**. 렌더러는 **mikepenz multiplatform-markdown-renderer 0.35.0**(`-m3`) — `ui/components/MarkdownText.kt`. 에디터 화면에 편집↔미리보기 토글.
5. ✅ **화면 + Navigation + MVVM** — 메모 목록(홈)·에디터(제목+본문+할일 체크리스트)·검색·할일 4화면 + `DayNoteNavHost`(navigation-compose). ViewModel 4개(`androidx.lifecycle.ViewModel` KMP) + `viewModel{}`/`koinInject` 주입. 하단탭 메모↔할일. **양쪽 빌드 성공**(assembleDebug + desktopTest 8건 유지).
   - ⚠️ **버전(2026-06-30)**: lifecycle/navigation은 CMP 1.8.2 호환쌍 — `org.jetbrains.androidx.lifecycle:*:2.9.0`, `org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta03`. 마크다운 렌더러는 **0.35.0 고정**(0.36.0+는 Kotlin 2.2 메타데이터 → 우리 2.1.20과 비호환, Koin과 같은 함정). ViewModel은 Koin DSL 대신 `viewModel{}`+`koinInject`(koin-compose)로 배선(버전 의존 최소).
   - 자산: `ui/{notes,search,todo,components,navigation}/...`. 에디터 noteId는 라우트 인자 대신 `rememberSaveable` 홀더로 전달(멀티플랫폼 인자 직렬화 회피 — Phase 2에서 타입안전 라우팅으로 개선 가능).
   - 참고: 새 메모는 제목/본문 중 하나라도 있어야 저장됨(빈 메모 방지) → 그 전엔 할 일 추가 비활성.
   - 🐞 **런타임 수정(2026-06-30)**: 저장 시 "Module with the Main dispatcher is missing" — `viewModelScope`가 `Dispatchers.Main`을 쓰는데 데스크톱엔 Main 제공자가 없었음. → `desktopMain`에 `kotlinx-coroutines-swing`, `androidMain`에 `kotlinx-coroutines-android` 추가(둘 다 coroutines-core와 동일 1.10.2). 단위 테스트는 `runBlocking`이라 Main을 안 거쳐 이 버그를 못 잡음 — GUI 실행에서만 드러남.
   - 🐞 **UX 수정(2026-06-30)**: 빈 메모(제목·본문 공백)에서 할 일만 추가하면 메모가 안 만들어져 할 일이 무시되던 문제 → `addNewTask`가 먼저 메모를 자동 생성(빈 메모 방지 가드는 "저장·뒤로"의 명시적 저장에만 적용). `AddNoteUseCase` 가드 제거(판단은 ViewModel로).
   - ✨ **UI 통일(2026-06-30)**: 하단 탭 라벨을 아이콘글자+라벨 중복("메"/"메모") → 단일 **`Memo` / `To-Do`** 로. To-Do 화면 제목도 통일.
   - ✨ **뒤로/앞으로 내비게이션(2026-06-30)**: `AppNavigator`(NavController 래퍼 + 직접 만든 forward 스택). **마우스 옆버튼**(`PointerButton.Back/Forward`), **키보드**(`Alt+←`/`Alt+→`), **안드로이드 시스템 뒤로**(NavHost 기본)로 이동. 새 화면 이동 시 forward 기록 초기화(브라우저 관례). Compose Navigation엔 forward가 없어 직접 구현 — 현재 그래프는 스택에 에디터가 최대 1개라 noteId 단일 상태로 충분.
     - ⚠️ **플랫폼 격리**: `PointerButton`/`PointerEvent.button`은 **데스크톱 전용**(Android·common에 없음) → `Modifier.mouseBackForward` expect/actual로 분리(desktop=구현, android=no-op). 키보드(Key/onPreviewKeyEvent)는 common.

## Phase 2 — 캘린더 통합 (홈 승격 · 적응형) ✅
- **홈 승격**: `DayNoteNavHost.startDestination = calendar`. 하단탭 **Calendar / Memo / To-Do**(`TopDestination`에 Calendar 추가).
- **적응형(`BoxWithConstraints` 창 너비)**: <600dp=주 세로 아젠다(날짜별 메모 미리보기), ≥600dp=월 달력 그리드(6주, 칸 안 메모 미리보기 2개+"+N개 더"). 두 뷰가 `selectedDate` 공유. 분기 기준은 기기 아님 창 너비 → 폴드 접/펼 자연 전환.
- **날짜 동선**: 날짜 탭 → `CalendarViewModel`이 그날 메모(`observeNotesByDate`)·할일(`observeTasksByDueDate`) 조회. 상세에서 "메모 추가"→에디터(선택 날짜 `Note.date` 자동 주입), 할일 인라인 추가(`dueDate` 자동 주입)·토글·삭제, 메모 인라인 삭제.
- **날짜 계산**: `kotlinx-datetime 0.6.2` + `core/DateUtils.kt`(주/월 그리드, dayRange epoch millis 변환). UseCase 추가: `ObserveNotesByDateUseCase`/`ObserveTasksByDateUseCase`/`ObserveUndatedNotesUseCase`. 에디터에 `initialDate` 주입 경로 추가.
- **라이브러리 결정**: Kizitonwose 대신 **커스텀 캘린더**(의존성 버전 위험 회피, 명세 허용). 월 그리드·주 아젠다·날짜별 미리보기 직접 구현.
- 자산: `ui/calendar/{CalendarScreen,CalendarViewModel}.kt`, `core/DateUtils.kt`, `ui/navigation/MouseNav.*`. **assembleDebug + desktopTest(8건) 양쪽 BUILD SUCCESSFUL.**
- 미진/후속: 마스터-디테일 2단(Expanded)·힌지 정밀 처리는 **Phase 5**. 날짜 없는 메모 보조 목록은 현재 Memo 탭(전체 목록)이 겸함.
- 🐞 **Phase 2 후속 수정(2026-06-30)**:
  - **To-Do 탭 누락**: 캘린더에서 만든 할 일(dueDate 있음)이 To-Do 탭에 안 보이던 문제 — To-Do 탭이 `observeUndated`(dueDate null만) → **`observeStandalone`(noteId null = 메모 미소속 전부, 날짜 무관)** 로 변경. 메모 내 체크리스트(noteId 있음)는 제외. 회귀 테스트 추가(RepositoryTest 5건).
  - **Calendar 라벨 영문화**: 상세의 `메모`→`Memo`, `할 일`→`To-Do`(헤더·버튼·placeholder).
  - **칸 미리보기 바로 열기**: 월 달력 칸/주 아젠다의 메모 미리보기 클릭 시 해당 메모를 곧바로 에디터로 연다(`onOpenNote` 를 `MonthGrid`/`DayCell`/`WeekAgenda` 에 전달). 날짜/빈칸 클릭은 기존대로 그 날짜 선택.

## Phase 3 — 구글 캘린더 동기화 (진행 중)
### 3-A 골격 ✅ (2026-06-30)
- **추상화**: `data/sync/CalendarSyncManager`(인터페이스) + `SyncState`(Unavailable/NeedsSetup/SignedOut/SignedIn/Syncing/Synced/Error). 본체·UI는 이 인터페이스만 본다(설계원칙 4).
- **플랫폼 격리**: `androidMain/.../AndroidCalendarSyncManager`(isAvailable=true, 현재 `NeedsSetup` 스텁), `desktopMain/.../DesktopCalendarSyncManager`(isAvailable=false, `Unavailable`). Koin `platformModule` 에 각각 등록.
- **설정 화면**: `ui/settings/{SettingsViewModel,SettingsScreen}`. 동기화 토글 + 상태 표시 + 로그인/로그아웃/지금 동기화 버튼. Calendar 상단 `Settings` 액션 → `Routes.SETTINGS`.
- **상태**: 실제 로그인/동기화는 스텁 — 누르면 "OAuth 자격증명 설정 후 사용" 안내. 데스크톱은 "미지원". **assembleDebug + desktopTest(9건) 양쪽 BUILD SUCCESSFUL.**

### 3-B 실제 연동 (진행 중 — 콘솔 설정 완료됨)
- ✅ **콘솔 설정 완료(2026-06-30)**: Android OAuth 클라이언트 ID 발급됨.
  - 클라이언트 ID(비밀 아님, SHA-1로 보호): `10882027046-lo01dp4ulcmcepe1a1vsvmc3fg2vrudt.apps.googleusercontent.com`
  - 디버그 SHA-1: `EB:77:ED:51:C0:AA:F5:27:17:2A:44:21:CB:69:56:9D:2D:AC:35:04` (이 PC `~/.android/debug.keystore`. 다른 PC·릴리스 키는 별도 등록 필요)
  - 코드 저장: `androidMain/.../data/sync/GoogleAuthConfig.kt`
- **인증 방식 결정**: Credential Manager(ID토큰)만으론 캘린더 접근 불가(Web 클라이언트 필요) → **Authorization API(`Identity.getAuthorizationClient`)로 `calendar.events` scope 액세스 토큰** 획득. Android 클라이언트(패키지+SHA-1)로 동작, Web 클라이언트 ID 불필요.
- ✅ **3-B1 로그인/인가 완료(2026-06-30)**: `play-services-auth 21.6.0`(Java AAR — Kotlin 메타데이터 위험 없음). Authorization API(`Identity.getAuthorizationClient`)로 `calendar.events` scope 액세스 토큰 요청. 동의 UI는 Activity 필요 → `expect/actual` 컴포저블 `rememberGoogleCalendarSignIn`(android=`rememberLauncherForActivityResult`+`StartIntentSenderForResult`, desktop=no-op). 토큰은 `AndroidCalendarSyncManager.onAccessToken`로 받아 상태 `SignedIn`. Web 클라이언트 ID 불필요(패키지+SHA-1 매칭). **assembleDebug + desktopTest(9건) 양쪽 BUILD SUCCESSFUL.**
  - ⚠️ **기기 검증 필요**: 실제 구글 로그인/동의 플로우는 CLI로 확인 불가 → 개발자가 `app-debug.apk`를 갤럭시에 설치해 Settings→동기화 사용 ON→구글 로그인으로 확인. (디버그 SHA-1 등록된 계정/테스트 사용자여야 함)
- ✅ **3-B1 기기 검증(2026-06-30)**: 갤럭시에서 로그인→동의→"로그인됨" 확인. (막혔던 `403 access_denied`는 코드 아님 — OAuth 동의화면 테스트모드 + 테스트 사용자 미등록이 원인. `labor7079@gmail.com`을 테스트 사용자로 추가해 해결.)
- ✅ **3-B2 한 방향 push 완료(2026-06-30)**: 날짜 있는 메모 → 구글 캘린더 **종일 이벤트** 생성/수정/삭제.
  - **의존성 없이**: Android 내장 `HttpURLConnection` + `org.json` (`androidMain/.../data/sync/CalendarApi.kt`). Ktor는 Phase 4(AI·commonMain)로 미룸.
  - **동기화 준비 수정**: `updateNote`가 remoteId/createdAt 보존 + `syncStatus=PENDING`(기존 매퍼는 remoteId를 null로 만들어 동기화가 깨졌음 — 회귀 테스트 추가). NoteDao 동기화 쿼리(`getNotesToPush`/`getDeletedNotesWithRemote`/`markSynced`/`clearRemote`).
  - `AndroidCalendarSyncManager`가 NoteDao 주입받아 `syncNow()`: 삭제 반영 → 신규(insert)/수정(update) push → `SYNCED` 표시. 401(토큰 만료) 시 재로그인 안내. AndroidManifest에 `INTERNET` 권한 추가.
  - **assembleDebug + desktopTest(10건) 양쪽 BUILD SUCCESSFUL.** APK `app/build/outputs/apk/debug/app-debug.apk`.
- ✅ **3-B3a 할 일 동기화 + 시간/종일(2026-06-30)**: 마감일 있는 할 일도 캘린더로 push. `TaskEntity.allDay`(@ColumnInfo defaultValue="1") 추가 → **DB v1→v2 마이그레이션**(`Migrations.kt`, `ALTER TABLE tasks ADD COLUMN allDay`, 데이터 보존). 종일=종일 이벤트 / 시간 지정=시간 이벤트(RFC3339). `CalendarApi.insertEvent/updateEvent(allDay)` 통합. `TaskDao` 동기화 쿼리 + `updateTask` remoteId 보존(메모와 동일 수정). 캘린더 상세 To-Do 추가에 **종일 토글 + 시간 선택(Material3 TimePicker)**. `addTask`/`AddTaskUseCase`/`addTaskForSelectedDate`에 allDay 전파.
- ✅ **에디터 저장/뒤로 분리(2026-06-30)**: 상단 좌측 `뒤로`(이동만), **하단 바 `저장` 버튼**(`vm.save()`). (기존 "저장·뒤로" 통합 제거 — 뒤로는 더 이상 자동 저장 안 함.)
- ✅ **UI 후속 수정(2026-06-30)**:
  - 시간 입력: 시계 다이얼(Material3 TimePicker) → **시/분 인라인 드롭다운(`NumberDropdown`, 목록 선택 + 숫자 직접 입력)** 으로 교체. (`종일` 끄면 노출)
  - 저장 버튼이 시스템 내비게이션 바와 겹치던 문제 → 에디터 하단 저장 버튼에 `navigationBarsPadding()`+`imePadding()` 적용(내비바/키보드 위로).
- ✅ **3-B3b 일부(2026-06-30)**:
  - **할 일 행 시간 표시**: `TaskRow` 가 시간 지정 할 일에 `HH:mm` 표시(`Long.toHourMinuteLabel`).
  - **동기화 토글 영속화**: DataStore 대신 **Room `settings` 테이블**(새 의존성 0개) — `AppSettingEntity`/`SettingDao`, DB **v2→v3 마이그레이션**, `SettingsRepository`(commonMain). 토글이 재시작 후에도 유지. `syncEnabled`를 매니저에서 빼서 `SettingsRepository`로 이전(매니저는 인증/동기화만). 끄면 자동 로그아웃(토큰 정리). 영속화 테스트 추가(RepositoryTest 7건, 총 11건).
  - ⚠️ 액세스 토큰은 단기 만료라 **매 세션 로그인 1회 필요**(추후 무음 재인가로 개선 가능).
- **3-B3c (다음, 보류)**: 캘린더→앱 **pull** — 사용자가 보류 결정. 추후 "앱이 만든 항목만 양방향" 방식으로 진행 예정(서버 updated 기준 last-write-wins).
- **Claude 작업(Android 전용)**: Credential Manager(`androidx.credentials` + `googleid`)로 로그인 → 액세스 토큰 → Google Calendar API 읽기/쓰기 → Note.date/Task.dueDate ↔ 캘린더 이벤트 매핑 → 충돌(last-write-wins, **서버 타임스탬프 기준**) → 토큰/상태 저장. 토글 영속화는 DataStore(KMP) 도입 시 함께.
- **검증**: OAuth 로그인은 CLI 빌드만으론 확인 불가 — 개발자가 실기기/에뮬레이터에서 실제 로그인 필요.

## 데스크톱 실행파일(.exe) 빌드 — 메모
- **빌드/실행 분리**: `:app:run`·`assembleDebug`·`desktopTest`는 JBR로 OK. 하지만 **배포물(`createDistributable`/`packageMsi`)은 `jpackage` 필요** → JBR엔 없음.
- 이 PC엔 **Eclipse Adoptium JDK 25**(`C:\Users\admin\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.3.9-hotspot`) 설치됨. Gradle 8.11.1은 JDK 25 전체 실행이 불안정하므로 **빌드는 JBR, jpackage만 JDK 25**로 분리.
- `build.gradle.kts`의 `compose.desktop.application.javaHome` ← `-Pdaynote.jpackage.jdk` 속성으로 받음(경로 하드코딩 안 함).
- 명령: `$env:JAVA_HOME=JBR; .\gradlew.bat :app:createDistributable "-Pdaynote.jpackage.jdk=<JDK25경로>"` → 산출물 `app\build\compose\binaries\main\app\DayNote\DayNote.exe`(번들 JRE, 129MB).
- ⚠️ 재패키징 전 **실행 중인 DayNote.exe 종료** 필요(폴더 잠금).

## 재개 명령 예시 (내일 이걸로 시작)

```
PROGRESS.md 읽고 이어서 진행해줘.
현재: Phase 1·2 완료. Phase 3 동기화는 앱→구글캘린더 한 방향(메모·할일, 종일/시간) + 토글 영속화까지 됨.
위 "다음 작업" 후보 중 하나를 진행: ① pull(앱이 만든 항목만 양방향) ② 무음 재로그인
③ 테마/다크모드 ④ Phase 4 AI 연동. 무엇부터 할지 먼저 물어보고 시작해줘.
빌드: $env:JAVA_HOME=JBR 로 :app:assembleDebug + :app:desktopTest.
.exe: createDistributable "-Pdaynote.jpackage.jdk=<JDK25경로>" (실행 중인 DayNote.exe 먼저 종료).
```

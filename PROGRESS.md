# 진행 상황 (체크포인트)

> 내일 이어가기 위한 인수인계 메모. 상세 명세는 [CLAUDE.md](CLAUDE.md), 빌드 절차는 [BUILD.md](BUILD.md).
>
> _최종 업데이트: 2026-07-01_

## 현재 위치: Phase 5-A/5-C + 기기 피드백 수정 완료 → 갤럭시탭/폴드7 재검증 대기

- **이번 세션 요약(2026-07-01)**: 자동 동기화·테마/다크모드·「Quiet Cadence」 팔레트/디자인 자산·Phase 5-A(2단)·5-C1/2(S펜 필기 캔버스) 완료 후, **갤럭시탭·폴드7 기기 피드백 1차 수정**까지.
- **기기 확인됨**: 테마 전환/유지 ✓, 색감 ✓, 수동 동기화 ✓, Phase 4(공유·요약/확장/교정) ✓, 구글캘린더 생성 ✓.
- **이번에 고친 것**(아래 "기기 피드백 1차 수정" 참조): 🐞월 달력 겹침(MonthGrid Column 누락) · ✨스와이프 슬라이드 · ✨빈날짜 탭→메모 · 🐞에디터 커서(focusProperties) · ✨AI 자유질문 인라인.
- **재검증 대기(개발자 실기기, 새 APK)**: ① 월 달력이 7열 그리드로 뜨는지(폴드 단일/탭 2단) ② 에디터 커서 위/아래 정상 ③ AI 자유질문 인라인 ④ 폰↔PC 자동 동기화 양방향 ⑤ S펜 필압/뒤집기.

- **앱이 Android·데스크톱 양쪽에서 빌드됨** — `:app:assembleDebug` + `:app:desktopTest` 모두 **BUILD SUCCESSFUL** (테스트 20건 통과).
- **DB 스키마 버전 4** (v1→v2 tasks.allDay, v2→v3 settings, v3→v4 ai_results 테이블). 마이그레이션은 데이터 보존.
- **완료 기능 요약**: Phase 1(로컬 MVP)·2(캘린더 홈)·3-B(앱→구글캘린더 push)·**4-A(AI 공유)·4-B(OpenAI 요약/확장/교정)·6(Supabase 멀티기기 동기화)** 코드 완료. 최신 APK: `app/build/outputs/apk/debug/app-debug.apk`.
- **구글 동기화 현황**: 로그인/인가(기기 확인됨) + **앱→캘린더 한 방향 push**(메모·할일, 종일/시간) 동작. **캘린더→앱 pull 은 미구현(보류)**. OAuth 클라이언트 ID·SHA-1 등록 완료(`GoogleAuthConfig.kt`). 액세스 토큰은 단기 만료 → 세션마다 로그인 1회 필요.
- **Phase 1 완료**: 메모 CRUD + 마크다운 편집/렌더 분리 + 체크리스트 할일 + 본문 FTS 검색 + Navigation/MVVM 골격.
- **Phase 2 완료**: **캘린더를 홈으로 승격**(startDestination=calendar). 적응형 — 창 너비 <600dp=주 아젠다 / ≥600dp=월 달력(칸 내 메모 미리보기+"+N개 더"), 두 뷰가 선택 날짜 공유. 날짜 탭 → 그날 메모·할일 조회 + 그 자리 추가(날짜 자동 주입)·삭제. 하단탭 Calendar/Memo/To-Do. 커스텀 캘린더(외부 라이브러리 미사용) + `kotlinx-datetime`.
- Phase 0(Android 골격) ✅ → 앱명/패키지 **DayNote / `com.kangtaeyoung.daynote`** 리브랜딩 ✅ → Phase 1 Step1(Room 스키마)·Step2(DAO) 코드+빌드 검증 ✅.
- **PC를 1급 타깃으로 결정** → Compose Multiplatform 전환. **0.5-A(버전 범프+구조 전환+Hilt 제거)·0.5-B(Room KMP)·0.5-C(Koin DI) 모두 완료 → Phase 0.5 종료.**
- **0.5-B 결과**: Room을 `commonMain`(KMP)로 이전 + **`@Fts4` 데스크톱 JVM 스모크 3건 통과**(영문·한글 토큰·소프트삭제 필터).
- **0.5-C 결과**: **Koin DI 도입**(`databaseModule` + `expect/actual platformModule`), `initKoin()`를 Android `DayNoteApp`·Desktop `main()`에서 호출.

## 확정된 진행 순서 (2026-07-01)
> AI 검색/자료수집(아래 4번)은 **"메모검색으로 마무리" 결정 → 드롭**. 기존 FTS 메모검색으로 충분, RAG/웹검색 안 만듦.
> **순서: ① 자동 동기화 ✅ → ② 폰 기기 검증(동기화·AI 한 번에) → ③ 테마/다크모드 → ④ Phase 5 갤럭시 폴리시.**

## 다음 작업 (새 세션에서 택1 — 권장 순서대로)
1. ✅ **자동 동기화 완료(2026-07-01)**: 앱 시작 시 1회 + 로컬 메모/할일 변경 시(디바운스 2.5s) `syncNow()` 자동 호출.
   - **설계원칙 4 유지**: 레포지토리는 동기화 존재를 모름 — `LocalChangeNotifier`(SharedFlow, `tryEmit`라 쓰기 비차단)에 "변경됨"만 발행. `AutoSyncCoordinator`가 구독→`debounce`→`cloud.syncNow()`.
   - **배선**: `NoteRepositoryImpl`/`TaskRepositoryImpl` 모든 쓰기 후 `notifyChanged()`. `App()`에서 `koinInject<AutoSyncCoordinator>()` + `LaunchedEffect{ start(this) }`(스코프=UI 수명). Koin `repositoryModule`에 `LocalChangeNotifier`·`AutoSyncCoordinator` 등록.
   - **중첩 방지**: `syncNow()`에 `Mutex.withLock` 추가(자동+수동 동시 실행 시 이중 push 차단). 게이팅(토글/설정/세션)은 syncNow 내부가 처리 → 코디네이터는 호출만.
   - **루프 없음**: pull 시 원격 반영은 `noteDao.upsert` 직접 호출(레포 경유 X)이라 `notifyChanged` 안 탐. 워터마크는 SettingDao라 무관.
   - 자산: `data/sync/{LocalChangeNotifier,AutoSyncCoordinator}.kt`. **assembleDebug + desktopTest(20건) 양쪽 BUILD SUCCESSFUL.**
   - ⚠️ 실제 멀티기기 자동 반영은 **폰 기기 검증(2번)에서 확인**.
2. **폰↔PC 동기화 기기 검증**: 갤럭시에 새 APK 설치 → 같은 계정 로그인 → 지금 동기화 → PC에서 쓴 메모가 폰에 뜨는지(양방향). (PC 단방향 push 는 확인됨)
3. **Phase 4-B 기기 검증**: 갤럭시에서 OpenAI 키 입력 → AI 칩(요약/확장/교정) 실제 호출 확인. DB v3→v4 마이그레이션 보존 확인.
4. ~~**AI 검색/자료수집**~~ **드롭(2026-07-01)**: "그냥 메모검색으로만 마무리" 결정 → RAG/웹검색 안 만듦. 기존 FTS 메모검색(Phase 1)으로 충분.
5. ✅ **테마/다크모드 완료(2026-07-01)**: 설정에서 **시스템/라이트/다크** 선택(SegmentedButton), 즉시 영속·앱 전체 반영.
   - `ThemeMode` enum(`domain/model`) + `SettingsRepository.observeThemeMode/setThemeMode`(settings 테이블 재사용, **새 의존성/마이그레이션 0**). 키 `theme_mode`.
   - `App()`이 테마 관찰 → SYSTEM=`isSystemInDarkTheme()`, LIGHT/DARK=강제 → `DayNoteTheme(darkTheme=…)`. 기존 light/darkColorScheme 그대로 활용.
   - 설정 화면 최상단 `ThemeSection`(SegmentedButton 3택). 자산: `domain/model/ThemeMode.kt`, `ui/settings/*` 수정. **assembleDebug + desktopTest(20건) 양쪽 BUILD SUCCESSFUL.**
   - ✨ **팔레트 교체(2026-07-01, 「Quiet Cadence」 디자인)**: 기본 보라 팔레트 → 정제된 웜 모노크롬. `Color.kt`/`Theme.kt` 전면 교체(라이트/다크 풀 롤). **매핑 원칙: 슬레이트(#3A4E62)=primary(상호작용 전반), 클레이(#AA422D)=tertiary(오늘·중요·핀만 절제 사용)** — "화려함 배제" 준수. 배경=웜 본(#F0ECE3), 다크=웜 차콜(#1A1815). 디자인 자산: `design/quiet-cadence.md`(철학) + `design/quiet-cadence-*.png`/`.pdf`(3-plate 아틀라스: Time·Text·Task). assembleDebug+compileKotlinDesktop 양쪽 BUILD SUCCESSFUL.
6. **AI 결과 이력 UI**: `ObserveAiResultsUseCase`는 배선됨 — 메모별 과거 AI 결과 목록 표시(현재 미사용).
7. **3-B3c 캘린더→앱 pull**(보류): "앱이 만든 항목(remoteId 있는 것)만 양방향". 충돌 서버 `updated` 기준.
8. **무음 재로그인**(구글 캘린더): 앱 시작 시(토글 ON) Authorization API 무음 인가로 토큰 자동 확보.
9. **Phase 5 갤럭시 폴리시** (진행 중 — 작은 단위로):
   - ✅ **5-A 마스터-디테일 2단(2026-07-01)**: Expanded(창 너비 ≥840dp — 탭 가로·폴드 펼침·PC)에서 **좌 월 달력 + 우 상세**를 `Row`로 동시 표시. 두 pane이 같은 `selectedDate` 공유(칸 탭 → 우측 상세 즉시 갱신). <840dp는 기존 1단(스택) 유지. 캘린더/상세를 `@Composable` 람다로 추출해 1단·2단이 공유(중복 0). commonMain(창 너비 기반)이라 플랫폼 API 무관 → Android·데스크톱 자동. 데스크톱 기본 창 1180×780(2단 기본 노출). `CalendarScreen.kt`·`main.kt` 수정. **assembleDebug + compileKotlinDesktop 양쪽 BUILD SUCCESSFUL.**
   - **5-B 폴드 감지(다음)**: WindowManager(`androidx.window`) Android 전용 → `expect/actual`(데스크톱 no-op). 힌지·접힘/플렉스 자세 감지(현재 창너비 분기로 접/펼 전환은 이미 자연 동작 — 5-B는 힌지 회피·테이블탑 등 정밀 처리).
   - ✅ **5-C1/5-C2 S펜 필기 캔버스(2026-07-01)**: Compose Canvas 자유 필기 — **commonMain 공유, 신규 의존성 0**. 마우스·손가락·S펜 공용. **필압**(S펜 pressure로 굵기 변조), **S펜 뒤집기=지우개**(`PointerType.Eraser` 자동 감지), 펜/지우개 토글·색 4종(테마색)·굵기 3단·실행취소·전체지우기. 자산: `ui/ink/{InkCanvas,InkScreen}.kt`. 네비 `Routes.INK`+`openInk()`, **에디터 상단바 "필기" 버튼**으로 진입. `awaitEachGesture`로 포인터 캡처(id 추적). **assembleDebug + compileKotlinDesktop 양쪽 BUILD SUCCESSFUL.**
   - **5-C3 ML Kit 텍스트 변환(다음, Android 전용)**: 잉크 획 → ML Kit Digital Ink 인식 → 텍스트를 메모 본문에 반영. `expect/actual`(Android=ML Kit, Desktop=미지원/대체). 필기 영속화도 함께 검토.
   - ⚠️ **기기 검증 권장**: 갤럭시에서 S펜 필압·뒤집기 지우개가 실제로 동작하는지(에디터→필기) 1회 확인. (CLI/데스크톱은 마우스만 검증 가능)

### 기기 피드백 1차 수정 (2026-07-01, 갤럭시탭)
> 확인됨: 테마 전환·유지 ✓, 색감 ✓, 수동 동기화 ✓, Phase 4 전부 ✓, 구글캘린더 생성 ✓.
- 🐞 **태블릿/폴드 월 달력이 "한 줄"로 겹침** — **실제 원인 확정(2026-07-01)**: 창너비 감지는 정상이었음(진단 결과 탭 w=1204·compact=false·twoPane=true, 폴드 w=749·compact=false). 진짜 버그는 **`MonthGrid` 내부에 `Column` 래퍼가 없어서**, 부모 `Box`(스와이프 Box→`AnimatedContent` 내부도 Box)가 요일헤더+6주 Row 7개를 **세로로 안 쌓고 겹쳐** 한 줄처럼 보인 것. (WeekAgenda는 Column으로 감싸 있어 compact 화면은 멀쩡했음 → 지금껏 월 달력을 넓은 화면에서 제대로 본 적이 없어 못 잡힘.) **수정: `MonthGrid`를 `Column`으로 감쌈.** (레터박싱 아님 — 매니페스트 `resizeableActivity`는 무해하게 유지.)
- ✨ **캘린더 전환 애니메이션**: 이전/다음(버튼·스와이프)에 가로 슬라이드(`AnimatedContent`, 방향 반영).
- ✨ **빈 날짜 탭→메모 추가**: "이 날의 Memo가 없습니다. 탭하여 추가하세요." 영역 클릭 시 새 메모(+Memo 버튼과 동일).
- 🐞 **에디터 커서 화살표**: 본문 첫 줄에서 위 화살표가 제목으로 점프 → `focusProperties{ up/down=Cancel }`로 포커스 이탈만 취소(커서 이동은 유지). **기기 테스트 요망**: 여러 줄에서 위/아래 커서 이동 정상 + 첫 줄 위 화살표가 제목으로 안 넘어가는지.
- ✅ **AI 자유 질문 인라인(2026-07-01)**: 에디터 AI 패널에 **"AI에게 질문" 입력창** 추가 → 메모를 맥락으로 OpenAI 에 자유 질문하고 답을 그 자리에 표시(앱 전환 없음, 입력한 키 재사용). `AiAction.ASK` 추가(칩에선 제외), `AiRepository.ask()`/`AskAiUseCase`/`AiViewModel.ask()` 배선. 결과도 Room `ai_results` 저장. ("AI로 보내기" 공유 버튼은 그대로 둠 — 다른 앱으로 내보내는 용도.) **assembleDebug + desktopTest(20건) 양쪽 BUILD SUCCESSFUL.**
10. **Phase 6 배포**: 앱 아이콘·스플래시, 데스크톱 `.exe` 정식 패키징(아래 메모), Play Console 내부 테스트.

> 진행 전 권장: 기기에서 **DB v4 마이그레이션이 정상(기존 데이터 보존, 크래시 없음)** 인지 1회 확인.

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

## Phase 4 — 생성형 AI 연동 (ChatGPT / OpenAI 단일)
### 4-A 메모 → AI 공유 ✅ (2026-06-30)
- **추상화/격리**: `ui/ai/AiShare.kt`(commonMain) — `class AiShare(actionLabel, confirmMessage, share)` + `@Composable expect fun rememberAiShare()`. 플랫폼 의존을 expect/actual 뒤로(설계원칙 3·6). **새 의존성 0개.**
- **Android**(`ui/ai/AiShare.android.kt`): `ACTION_SEND` 표준 Sharesheet(`Intent.createChooser`)로 텍스트 공유 → 사용자가 시트에서 ChatGPT 등 선택. `actionLabel="AI로 보내기"`, 확인 스낵바 없음(시트가 뜸).
- **Desktop**(`ui/ai/AiShare.desktop.kt`): Sharesheet 없음 → AWT 클립보드 복사(`actionLabel="AI용으로 복사"`, 스낵바 "클립보드에 복사했어요…").
- **연결**: `NoteEditorScreen` 상단바에 `aiShare.actionLabel` 버튼 추가 → 제목+본문 합쳐(`buildAiShareText`) 공유. Scaffold `snackbarHost` 추가(데스크톱 복사 확인용). 보낼 내용 없으면 "보낼 내용이 없어요." 스낵바.
- **assembleDebug + desktopTest(11건) 양쪽 BUILD SUCCESSFUL.**
- ⚠️ **기기 검증 권장**: Android 공유 시트가 실제로 뜨고 ChatGPT 앱이 후보에 보이는지(기기에 ChatGPT 설치 시) 1회 확인.

### 4-B OpenAI REST API ✅ (2026-06-30)
- **의존성(commonMain Ktor)**: `ktor 3.1.3`(client-core/content-negotiation/serialization-kotlinx-json) + `kotlinx-serialization-json 1.8.0` + serialization 플러그인. 엔진은 플랫폼별 — **Android=`ktor-client-okhttp`, Desktop=`ktor-client-cio`**(클래스패스 자동 선택, `HttpClient{}` 엔진 미지정). API 키 암호화용 **`androidx.security:security-crypto 1.1.0-alpha06`**(androidMain). ⚠️ **버전 함정 회피**: Ktor 3.1.3·serialization 1.8.0 모두 Kotlin 2.1.x 메타데이터 → 우리 2.1.20과 호환(Koin 4.2.2/마크다운 0.36 같은 함정 아님).
- **데이터 계층(설계원칙 4)**: `domain/model/Ai.kt`(`AiAction` 요약/확장/교정 — systemPrompt+instruction 내장, `AiResult`). `data/remote/openai/OpenAiClient.kt`(Ktor, Chat Completions 단일, 401/429 메시지 처리). `data/repository/AiRepository.kt`(인터페이스+`AiRepositoryImpl`, **구현체 1개**, `gpt-4o-mini`) — UI는 OpenAI를 전혀 모름. 결과는 **Room `ai_results` 테이블에 저장**(진실의 원천 → 오프라인·재시작 유지).
- **API 키 보안(하드코딩 금지)**: `data/security/ApiKeyProvider`(commonMain 인터페이스) + **Android=`EncryptedSharedPreferences`(AES256-GCM, Keystore)**, **Desktop=`~/.daynote/openai.key`(소유자 전용 권한, 평문 — 데스크톱 한계 문서화)**. `platformModule`에 각각 등록.
- **DB v3→v4 마이그레이션**: `ai_results` 테이블 + noteId/createdAt 인덱스 추가(`MIGRATION_3_4`, 데이터 보존). AppDatabase v4 + `aiResultDao()`.
- **화면**: `ui/ai/{AiViewModel,AiPanel}.kt` — 동작 칩(요약·확장·교정)→Loading→Success(결과 카드+"메모에 반영"/"닫기")/Error. 에디터 하단에 `AiPanel`(본문을 소스로, 결과는 본문 끝에 덧붙임). **설정 화면에 OpenAI 키 입력/삭제 섹션**(`PasswordVisualTransformation`, 원문 비노출, 저장 여부만 표시).
- **Koin 배선**: `OpenAiClient`·`AiRepository`(repositoryModule), `RunAiActionUseCase`/`ObserveAiResultsUseCase`(useCaseModule), `aiResultDao`(databaseModule), `ApiKeyProvider`(platformModule).
- **테스트**: `AiRepositoryTest`(desktopTest) 2건 — DAO 왕복+최신순 정렬, 키 미설정 시 안전 실패(DB 미기록). **assembleDebug + desktopTest(13건) 양쪽 BUILD SUCCESSFUL.**
- ⚠️ **기기 검증 필요**(CLI 불가): OpenAI 키 발급(개발자) → 설정에서 입력 → 메모 에디터 AI 칩으로 실제 호출/응답 확인. DB v4 마이그레이션 보존도 1회 확인.

## Android UX 수정 (2026-06-30, 기기 피드백)
- **설정 화면 키보드 가림**: 가상 키보드가 뜨면 입력칸이 가려지던 문제 → `SettingsScreen` 스크롤 Column 에 `imePadding()` 추가(MainActivity 가 `enableEdgeToEdge` 라 Compose IME 인셋 동작). 키보드 높이만큼 스크롤 영역이 줄어 나머지 내용이 계속 보이고 스크롤 가능.
- **캘린더 스와이프**: 이전/다음 달(월 그리드)·주(주 아젠다) 이동이 버튼으로만 됐음 → 달력 영역에 `detectHorizontalDragGestures` 추가(왼쪽 스와이프=다음, 오른쪽=이전, 임계값 56dp). 버튼도 그대로 동작. `onPrev/onNext` 로직을 `goPrev/goNext` 로 추출해 버튼·스와이프 공용. `assembleDebug + desktopTest(20건)` 양쪽 BUILD SUCCESSFUL.

## Phase 6 — 동기화 & 배포 (Supabase 선택)
> 멀티기기 데이터 일치 목표. **방식 결정(2026-06-30): Supabase** — Ktor(commonMain)라 PC·Android 동일 코드, 레코드 단위 실시간, Postgres/SQL 친화. (구글 드라이브는 Android 중심+파일단위라 제외)

### 6-A 골격 ✅ (2026-06-30)
- **추상화(설계원칙 4)**: `data/sync/CloudSyncManager`(인터페이스) + `CloudSyncState`(Disabled/NeedsConfig/Idle/Syncing/Synced/Error). 구글 캘린더(`CalendarSyncManager`/`SyncState`)와 **별개**. 본체·UI는 인터페이스만 본다.
- **양 플랫폼 공유**: `SupabaseCloudSyncManager`(commonMain) — Ktor라 Android·Desktop 같은 코드(구글 캘린더와 달리 플랫폼 격리 불필요). Koin `appModules`(repositoryModule)에 등록 — platformModule 아님.
- **설정 영속**: `SupabaseConfig`(url+anonKey, 비밀 아님—RLS가 보호). `SettingsRepository`에 `cloudSyncEnabled` 토글 + `supabaseConfig`(url/anon key) 추가(settings 테이블 키, **새 의존성/마이그레이션 0**).
- **충돌 해소 순수함수**: `resolveByUpdatedAt(local,remote)` = last-write-wins(동률=로컬 유지). ⚠️ updatedAt은 클라 시계라 clock skew 취약 → 6-B에서 서버 타임스탬프 기준으로 개선 예정(CLAUDE.md §6).
- **설정 UI**: 설정 화면에 "클라우드 동기화 (Supabase)" 섹션 — 토글 + URL/anon key 입력(영속) + 상태 + "지금 동기화". 화면이 길어져 `verticalScroll` 추가.
- **6-A 한계(의도적)**: `syncNow()`는 토글/설정 확인까지만(설정되면 `Idle`). **실제 Auth·push/pull은 6-B.** 골격이라 아직 기기 간 데이터가 실제로 합쳐지진 않음.
- **테스트**: `SyncConflictTest`(desktopTest) 4건 — last-write-wins 3 + config 유효성 1. **assembleDebug + desktopTest(17건) 양쪽 BUILD SUCCESSFUL.**

### 6-B 실연동 ✅ 코드 완료 (2026-06-30) — 기기 검증 대기
- **개발자 콘솔(완료)**: Supabase 프로젝트 생성 + `notes`/`tasks` 테이블 + RLS(auth.uid 기반) SQL 적용함. 인증=이메일+비밀번호.
- **델타 동기화 방식**: syncStatus 가 캘린더(Phase 3)와 공유돼 간섭 위험 → **워터마크(updatedAt > lastSync) 델타**로 독립 구현. push 대상 = `getNotesModifiedSince`/`getTasksModifiedSince`(삭제 tombstone 포함, deletedAt 필터 없음). 워터마크는 settings `cloud_last_sync`. echo 방지: 처리한 행들의 max updatedAt 으로 전진 + 쿼리는 strict `>`.
- **충돌**: `resolveByUpdatedAt`(last-write-wins, 동률=로컬). ⚠️ 클라 시계 기준이라 추후 서버 타임스탬프로 개선 여지(CLAUDE.md §6).
- **자산**:
  - `data/sync/supabase/SupabaseDtos.kt`(AuthRequest/Response, NoteRow/TaskRow snake_case @SerialName, AuthSession), `SupabaseSyncClient.kt`(Ktor: signIn/signUp/refresh + notes/tasks push(upsert `Prefer: merge-duplicates`, `on_conflict=id`)/pull(`updated_at=gt`)), `RowMappers.kt`(엔티티↔행, **pull 시 캘린더 메타 remoteId/syncStatus 보존**).
  - `data/sync/SupabaseCloudSyncManager.kt`(commonMain, 양 플랫폼): 로그인/로그아웃/refreshState/syncNow. **401 시 refresh 토큰으로 1회 자동 갱신 후 재시도**, 실패하면 SignedOut. push→pull→워터마크 전진.
  - `data/security/SecureStore.kt`(인터페이스) + Android(`EncryptedSharedPreferences`)/Desktop(`~/.daynote/secure.properties` 권한제한) — access/refresh/userId 토큰 저장.
  - `CloudSyncState` 확장(SignedOut/SignedIn 추가). `SettingsRepository` 에 워터마크 get/set 추가.
  - DAO: `getNotesModifiedSince`/`getTasksModifiedSince` + `getByIdRaw`(소프트삭제 포함 조회, 충돌 비교용).
  - 설정 UI: 토글 ON 시 URL/anon key 입력 → "접속 설정 저장" → 이메일/비밀번호 **로그인/회원가입** → 로그인되면 "지금 동기화"/"로그아웃". `cloudBusy` 진행표시.
  - Koin: `SupabaseSyncClient`+`CloudSyncManager`(5인자) commonMain, `SecureStore` platformModule.
- **테스트**: `SupabaseMappingTest`(desktopTest) 3건 — push/pull 라운드트립, pull 신규=remoteId 없음, 삭제 tombstone 전파. **assembleDebug + desktopTest(20건) 양쪽 BUILD SUCCESSFUL.**
- ⚠️ **한계/후속**: (1) **자동 동기화 아직 없음** — 앱 시작/변경 시 자동 호출은 미구현, 현재는 설정에서 "지금 동기화" 또는 로그인 시 1회. (2) 실제 다중기기 동기화는 **개발자가 두 기기에서 확인** 필요(CLI 불가). (3) 액세스 토큰 만료는 refresh 로 자동 처리(refresh 토큰까지 만료면 재로그인).
- **검증 절차**: ① PC DayNote 설정 → 동기화 ON → URL/anon key 입력 → 접속 설정 저장 → 회원가입(또는 로그인) → 지금 동기화. ② 갤럭시 APK 동일 계정 로그인 → 지금 동기화 → 메모가 양쪽에 보이는지 확인.
- ✅ **PC 동작 확인(2026-06-30)**: 데스크톱에서 로그인 + "지금 동기화" → **"동기화 완료"** 확인(Supabase notes 테이블에 push 성공). 폰↔PC 양방향은 갤럭시 APK 설치 후 확인 예정.
- 🐞 **런타임 수정(2026-06-30, 기기 테스트에서 발견)**:
  - **URL 정규화**: 사용자가 Supabase URL에 `/rest/v1/`까지 붙여 넣어 `PGRST125 Invalid path`(404) 발생 → `SupabaseSyncClient.base()`가 끝의 `/rest/v1`·`/auth/v1`를 자동 제거하도록 보완.
  - **encodeDefaults**: 일괄 upsert 시 기본값(null/false) 필드가 생략돼 객체마다 키가 달라 `PGRST102 All object keys must match`(400) 발생 → 클라이언트 Json에 `encodeDefaults=true`+`explicitNulls=true` 설정해 모든 행의 키 집합 통일.

## 데스크톱 실행파일(.exe) 빌드 — 메모
- **빌드/실행 분리**: `:app:run`·`assembleDebug`·`desktopTest`는 JBR로 OK. 하지만 **배포물(`createDistributable`/`packageMsi`)은 `jpackage` 필요** → JBR엔 없음.
- 이 PC엔 **Eclipse Adoptium JDK 25**(`C:\Users\admin\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.3.9-hotspot`) 설치됨. Gradle 8.11.1은 JDK 25 전체 실행이 불안정하므로 **빌드는 JBR, jpackage만 JDK 25**로 분리.
- `build.gradle.kts`의 `compose.desktop.application.javaHome` ← `-Pdaynote.jpackage.jdk` 속성으로 받음(경로 하드코딩 안 함).
- 명령: `$env:JAVA_HOME=JBR; .\gradlew.bat :app:createDistributable "-Pdaynote.jpackage.jdk=<JDK25경로>"` → 산출물 `app\build\compose\binaries\main\app\DayNote\DayNote.exe`(번들 JRE, 129MB).
- ⚠️ 재패키징 전 **실행 중인 DayNote.exe 종료** 필요(폴더 잠금).

## 재개 명령 예시 (다음 세션에서 이걸로 시작)

```
PROGRESS.md 읽고 이어서 진행해줘.
현재: Phase 1·2·3-B·4·5-A·5-C1/2·6 코드 완료 + 테마/다크·Quiet Cadence 디자인 완료.
직전 세션에서 갤럭시탭/폴드7 기기 피드백 1차 수정(월달력 겹침·스와이프·빈칸탭·커서·AI자유질문).
→ 먼저 개발자 재검증 결과를 물어봐줘: ①월 달력 7열 그리드(폴드 단일/탭 2단) ②에디터 커서 ③AI 자유질문
  ④폰↔PC 자동 동기화 양방향 ⑤S펜 필압/뒤집기.  이상 없으면 다음 작업(아래) 중 택1.
빌드: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" 로
     :app:assembleDebug + :app:desktopTest (현재 테스트 20건).
데스크톱 실행(GUI 확인): :app:run  (창 1180×780=2단 기본, 창 닫으면 종료).
.exe: createDistributable "-Pdaynote.jpackage.jdk=<JDK25경로>" (실행 중인 DayNote.exe 먼저 종료).

남은 작업(우선순위): (A)기기 재검증→커밋  (B)5-C3 ML Kit 텍스트변환/필기 영속화
(C)AI 결과 이력 UI  (D)배포: 아이콘·스플래시·.exe 패키징·Play Console  (E)보류: 구글 pull·무음 재로그인.
```

### 확정된 우선순위 (2026-07-01)
- **AI 검색/자료수집**: 드롭 — "메모검색으로 마무리"(기존 FTS로 충분). (다음 작업 4번 참조)
- **순서**: ① 자동 동기화 ✅ → ② 폰 기기 검증(동기화·AI 한 번에, **개발자 실기기**) → ③ 테마/다크모드 ✅ → ④ Phase 5 → 배포.

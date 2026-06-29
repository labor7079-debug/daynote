# 진행 상황 (체크포인트)

> 내일 이어가기 위한 인수인계 메모. 상세 명세는 [CLAUDE.md](CLAUDE.md), 빌드 절차는 [BUILD.md](BUILD.md).
>
> _최종 업데이트: 2026-06-29_

## 현재 위치: Phase 0.5-A 완료 ✅ → 다음은 0.5-B (Room KMP)

- **앱이 Android·데스크톱 양쪽에서 빌드됨** — `:app:assembleDebug` + `:app:compileKotlinDesktop` 모두 **BUILD SUCCESSFUL**.
- Phase 0(Android 골격) ✅ → 앱명/패키지 **DayNote / `com.kangtaeyoung.daynote`** 리브랜딩 ✅ → Phase 1 Step1(Room 스키마)·Step2(DAO) 코드+빌드 검증 ✅.
- **PC를 1급 타깃으로 결정** → Compose Multiplatform 전환 시작. **0.5-A1(버전 범프)·0.5-A2(구조 전환+Hilt 제거) 완료.**
- 다음 작업: **0.5-B** — Room을 `androidMain` → `commonMain`(Room KMP)로 이전 + `@Fts4` 데스크톱 스모크 테스트. (아래 "다음 할 일" 참조)

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

## 다음 할 일: Phase 0.5 — Compose Multiplatform 전환 (먼저!)

작은 단위로, 각 단계마다 **Android·데스크톱 양쪽 빌드** 확인:
- ✅ **0.5-A1 버전 범프** — Kotlin 2.0.21→**2.1.20**, KSP→**2.1.20-1.0.31**, CMP 플러그인 **1.8.2** (AGP 8.7.3 / Gradle 8.11.1 유지). Android 빌드 통과.
- ✅ **0.5-A2 구조 전환** — `:app`을 KMP 멀티모듈(`commonMain`/`androidMain`/`desktopMain`)로 재편, CMP 플러그인 적용, **데스크톱 JVM 타깃** 추가. **Hilt 완전 제거**(`DayNoteApp` 삭제, `@AndroidEntryPoint` 제거). 테마는 Android 전용 다이내믹 컬러 제거 후 `commonMain` 공유. 공유 `App()` 컴포저블을 Android(`MainActivity`)·Desktop(`main.kt`) 양쪽이 호출. **`assembleDebug` + `compileKotlinDesktop` 양쪽 BUILD SUCCESSFUL.** Room은 `androidMain`에 임시 유지(`kspAndroid`).
- ⬅ **0.5-B Room KMP** (다음) — `androidMain`의 엔티티/DAO/`AppDatabase`를 `commonMain`으로 이전 + Room **2.8.4** + `androidx.sqlite:sqlite-bundled 2.6.2`(`BundledSQLiteDriver`) + `room.generateKotlin=true`. KSP를 `kspAndroid`+`kspDesktop`로 구성. **`@Fts4`를 데스크톱 JVM에서 런타임 스모크 테스트**(공식 보증 없음 — 동작 확인 필수).
- **0.5-C Koin** — Koin BOM **4.2.2** 추가(`koin-core`+`koin-compose`→commonMain, `koin-androidx-compose`→androidMain), DB 제공 모듈 작성. Koin 시작용 Application(또는 MainActivity에서 startKoin) 재도입.

> Hilt는 0.5-A2에서 이미 제거됨(원래 C 단계였으나 구조 전환에 흡수). DI는 0.5-B에서 Room DB를 주입할 때 Koin으로 도입.
> 자산 위치: 현재 `app/src/androidMain/kotlin/.../data/local/{entity,dao,AppDatabase}` → 0.5-B에서 `commonMain`으로 이동.

## 그다음: Phase 1 — 로컬 MVP (commonMain에서 작성)
1. ✅ **Room 스키마** — `NoteEntity`/`TaskEntity` + FTS4(`NoteFtsEntity`), `AppDatabase`(v1), `SyncStatus`. (0.5-B에서 KMP로 이전)
2. ✅ **DAO** — `NoteDao`/`TaskDao`: upsert·소프트삭제/복구·날짜범위·미지정 조회, FTS 검색, 할일 토글. `assembleDebug` **BUILD SUCCESSFUL**(KSP가 FTS rowid 조인·`NOT isDone` 토글·전 컬럼 검증 통과).
3. **Repository + UseCase** — `NoteRepository`/`TaskRepository` 인터페이스+구현, GetNotes/UpsertNote/ToggleTask 등
4. **마크다운 에디터** — 편집(`BasicTextField`)·렌더링(CMP 라이브러리) **분리**
5. **화면** — 목록·상세, 체크리스트 할 일, 본문 검색, Navigation 라우팅(MVVM)

## 재개 명령 예시 (내일 이걸로 시작)

```
PROGRESS.md 읽고 Phase 0.5-B를 진행해줘.
androidMain의 Room 엔티티/DAO/AppDatabase를 commonMain으로 옮기고,
Room 2.8.4 + sqlite-bundled 2.6.2(BundledSQLiteDriver) + room.generateKotlin=true로
KMP 전환(KSP는 kspAndroid+kspDesktop). 그다음 @Fts4가 데스크톱 JVM에서 실제로 도는지
검색 쿼리로 스모크 테스트하고, Android·데스크톱 양쪽 빌드 확인해줘.
```

# 진행 상황 (체크포인트)

> 내일 이어가기 위한 인수인계 메모. 상세 명세는 [CLAUDE.md](CLAUDE.md), 빌드 절차는 [BUILD.md](BUILD.md).
>
> _최종 업데이트: 2026-06-29_

## 현재 위치: Phase 0 완료 ✅

- Android 프로젝트 골격 생성 + Gradle/Compose/Hilt/Room 의존성 구성
- `./gradlew assembleDebug` **BUILD SUCCESSFUL** 검증 완료 → `app/build/outputs/apk/debug/app-debug.apk`
- 빈 화면("Phase 0 — 빈 화면이 빌드되었습니다") 표시까지 코드 준비됨
- (선택) 실기기/에뮬레이터 실행 확인은 미진행 — 빌드는 이미 통과라 진행에 지장 없음

## 확정된 핵심 결정 (CLAUDE.md에 반영됨)

- **패키지/applicationId**: `com.eastarjet.galaxymemo` · minSdk 26 / compileSdk·targetSdk 35
- **진입점**: 캘린더가 홈 (설계원칙 5)
- **적응형 레이아웃**: 창 너비(`WindowSizeClass`)로 분기 — 폰=주 아젠다 / 태블릿·폴드·PC=월 달력(칸 내 미리보기)
- **에디터**: 마크다운, 편집=`BasicTextField` / 렌더=라이브러리 **분리**
- **AI**: **ChatGPT(OpenAI) 단일** + `AiRepository` 추상화(구현체 1개) — Phase 4 참고 코드 있음
- **저장소**: Room이 진실의 원천(오프라인 우선), 동기화는 Phase 6

## 빌드 방법 (CLI, 이 PC)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # 번들 JDK 21
.\gradlew.bat assembleDebug --console=plain
```
- 시스템 PATH에 java 없음 → JAVA_HOME 반드시 지정
- SDK: `%LOCALAPPDATA%\Android\Sdk` (Android Studio가 설치/관리)

## 다음 할 일: Phase 1 — 로컬 MVP (메모 + 할 일)

작은 단위로 끊어서 진행 (각 단계 후 빌드 확인):
1. **Room 스키마** — `NoteEntity`/`TaskEntity`(동기화 메타 + `@Index`) + FTS 가상 테이블, `AppDatabase`, Converters
2. **DAO** — Note/Task CRUD + 소프트 삭제(`deletedAt`) 쿼리 + 본문 검색(FTS)
3. **Repository + UseCase** — `NoteRepository`/`TaskRepository` 인터페이스+구현, GetNotes/UpsertNote/ToggleTask 등
4. **마크다운 에디터** — 편집(`BasicTextField`)·렌더링(라이브러리) **분리** 구현
5. **화면** — 목록·상세, 체크리스트 할 일, 본문 검색, Navigation 라우팅(MVVM)

> 주의: 이 단계 목록 화면은 임시 골격. Phase 2에서 캘린더 홈으로 승격되므로 Navigation으로 느슨하게.

## 재개 명령 예시

```
PROGRESS.md 읽고 Phase 1을 1번(Room 스키마)부터 시작해줘.
CLAUDE.md 5장 데이터 모델대로 동기화 메타필드·@Index·FTS 포함해서.
```

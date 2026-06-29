# 갤럭시 메모 앱 — 개발 명세서 & 로드맵

> **캘린더를 기점으로** 메모를 추가·삭제·관리하는, To-Do + 생성형 AI 연동을 결합한 정제된 갤럭시 전용 노트 앱
>
> **이 문서는 Claude Code로 개발을 진행하기 위한 프로젝트 명세서입니다.** 프로젝트 루트에 두고, Phase 단위로 하나씩 구현을 지시하는 기준 문서로 사용합니다.
>
> _최종 업데이트: 2026-06-29_

---

## 0. 이 문서 사용법 (Claude Code 협업 모델)

이 프로젝트는 **Claude Code가 코드와 빌드를 담당하고, 개발자가 기기 실행·외부 콘솔 설정을 담당**하는 협업 모델로 진행합니다.

| Claude Code가 담당 | 개발자가 직접 (Claude Code 범위 밖) |
|---|---|
| Kotlin / Compose 코드 작성·수정 | Android Studio GUI 일부 작업 |
| Gradle 설정, 의존성 추가 | 실제 갤럭시 탭/폴드 USB 연결 및 빌드 실행 |
| 파일·패키지 구조 생성 | 에뮬레이터 GUI로 동작 확인 |
| `./gradlew build` 등 빌드 명령 | Google Cloud Console OAuth 설정(웹 콘솔) |
| 단위 테스트 작성·실행, 디버깅 | Play Console 배포 |

**진행 방식**: 한 번에 한 Phase, 한 Phase 안에서도 작은 작업 단위로 끊어서 지시 → 빌드 통과 확인 → 다음 단계. 큰 덩어리를 한 번에 맡기지 말 것.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 목표 | 노션식 메모를 단순화하고, 캘린더·할 일·AI 푸쉬를 자연스럽게 결합한 개인용 노트 앱 |
| 핵심 동선 | **캘린더가 홈 화면이자 기본 진입점** — 날짜를 탭해 그날의 메모·할 일을 추가·삭제·조회한다 |
| 타깃 기기 | 갤럭시 탭 / 갤럭시 폴드 (안드로이드 단일 타깃) |
| 디자인 원칙 | 화려함 배제, 정제된 여백 중심 UI, 빠른 입력 |
| 데이터 원칙 | 오프라인 우선 — 네트워크 없이도 메모·할 일 완전 동작 |
| 개발 형태 | 1인 개발 (본업 병행, 저녁·주말 작업 가정) |

---

## 2. 핵심 설계 원칙 (변경 금지)

이 다섯 가지는 프로젝트 전체에서 절대 흔들리지 않는 기준입니다.

1. **노션식 블록 에디터를 만들지 않는다.** 마크다운 기반으로 가고, "블록"은 헤딩·체크리스트·인용 정도로 좁힌다. 이 한 가지가 프로젝트 성패를 가른다. 블록 드래그·중첩·인라인 위지윅을 구현하려는 충동이 들면 멈춘다.
2. **오프라인 우선 — 로컬(Room)이 진실의 원천(Source of Truth)이다.** 모든 읽기·쓰기는 먼저 로컬 DB로 간다. 클라우드는 그 위에 얹는 동기화 계층일 뿐, 끊겨도 앱 본체는 정상 동작한다.
3. **갤럭시 특화는 맨 나중에.** 폴드·S펜·대화면은 핵심 기능 완성 후 얹는 폴리시 단계로 분리한다.
4. **데이터 계층은 Repository로 추상화한다.** UI·도메인은 데이터가 로컬에서 오는지 클라우드에서 오는지 몰라야 한다. 그래야 나중에 동기화 방식(드라이브 / Supabase / Firebase)을 코드 한 구석만 바꿔 교체할 수 있다.
5. **캘린더가 기본 진입점이다.** 메모·할 일의 추가·삭제·조회는 "캘린더에서 날짜를 선택한다"를 1차 동선으로 한다. 앱의 중심 은유는 **"날짜 위에 메모가 얹힌다"**이며, 평면적 목록은 보조 수단일 뿐이다. 날짜를 탭하면 그날의 항목이 펼쳐지고, 그 자리에서 바로 추가·삭제한다. 단, 날짜 없는(`date`/`dueDate`가 null인) 메모도 허용하되 기본 작성 흐름은 항상 특정 날짜에서 출발한다. **표현은 적응형이다 — 창 너비(`WindowSizeClass`)로 분기**: Compact(폰·폴드 접힘)는 주 단위 세로 아젠다(날짜마다 메모 미리보기), Medium·Expanded(태블릿·폴드 펼침·PC)는 월 달력 칸 안에 메모 미리보기. **기기 종류가 아니라 창 너비**로 판단해 폴드 접고 펼 때 자연 전환되게 한다.

---

## 3. 확정 기술 스택

| 영역 | 기술 | 메모 |
|---|---|---|
| 언어 | Kotlin (최신 안정 버전) | — |
| UI | Jetpack Compose + Material 3 (Compose BOM) | 선언형, 적응형 레이아웃 내장 |
| 화면 전환 | Jetpack Navigation Compose | 캘린더(홈)·노트·투두·검색·AI·설정 라우팅 |
| 적응형 분기 | `WindowSizeClass` (`material3-adaptive`) | **창 너비로 분기** — Compact=주 아젠다 / Medium·Expanded=월 달력 |
| 대화면 | Compose Adaptive (`material3-adaptive`) | 월 달력 칸 내 메모 미리보기 + 마스터-디테일 2단 |
| 폴더블 | Jetpack WindowManager (`androidx.window`) | 접힘·플렉스·힌지 감지 (접힘=Compact) |
| 로컬 DB | Room (SQLite) | 오프라인 우선 저장소 · 진실의 원천 |
| 검색 | Room FTS4/FTS5 | 메모 전문 검색 (Phase 1~2) |
| 설정 저장 | DataStore (Preferences) | 테마·동기화 토글 등 키-값 환경설정 |
| 에디터 | 마크다운 — **편집은 `BasicTextField`, 렌더링은 라이브러리** | ⚠️ 블록 에디터 금지 · Markwon은 View 기반 **렌더 전용**(편집 불가), AndroidView interop 또는 Compose 마크다운 라이브러리 사용 |
| 캘린더 UI | Kizitonwose Calendar Compose | 성숙한 오픈소스 |
| 구글 캘린더 | Google Calendar API + Credential Manager | OAuth 표준 (Phase 3) |
| AI (1단계) | Android Sharesheet (Intent) | **ChatGPT 앱**으로 메모 텍스트 푸쉬 (단일) |
| AI (2단계) | Ktor 또는 Retrofit | **OpenAI REST API 단일** · `AiRepository`로 추상화(구현체 1개) |
| S펜 필기 | ML Kit Digital Ink (변환 시) | 단순 잉크는 Compose Canvas |
| 아키텍처 | MVVM + Repository | ViewModel · Coroutines · Flow |
| DI | Hilt | 의존성 주입 |
| 빌드 | Android Studio + Gradle (Kotlin DSL) | — |

> 버전은 Claude Code가 빌드 시점의 최신 안정 버전을 사용하되, Compose는 BOM으로 버전을 묶어 관리한다.

---

## 4. 아키텍처

### 레이어 구조
```
UI (Compose)  ──┐  화면: 캘린더(홈) · 노트 · 투두 · 검색 · AI · 설정 + 적응형 레이아웃
                │  ↕ State (ViewModel · Flow)
Domain        ──┤  UseCase · Model
                │  ↕
Data (Repository)┘  로컬 우선, 동기화는 별도 레인
   ├── Local:  Room (SQLite)  ← 진실의 원천
   └── Remote: (Phase 6) 동기화 계층 · Phase 3 Google Calendar · Phase 4 AI API
```

핵심: 모든 데이터가 Local(Room)을 먼저 거치므로 오프라인에서 메모·투두가 완전 동작하고, Remote는 격리되어 외부 연동에 문제가 생겨도 앱 본체에 영향이 없다. S펜·폴더블 감지는 UI 레이어로만 입력된다.

### 제안 패키지 구조
```
com.<owner>.galaxymemo/
├── data/
│   ├── local/          # Room: entities, daos, AppDatabase, Converters
│   ├── remote/         # (Phase 3~6) Calendar API, AI API, Sync
│   └── repository/     # NoteRepository, TaskRepository (인터페이스 + 구현)
├── domain/
│   ├── model/          # Note, Task (UI/도메인용 순수 모델)
│   └── usecase/        # GetNotes, UpsertNote, ToggleTask ...
├── ui/
│   ├── calendar/       # 캘린더 뷰 (홈 · 기본 진입점)
│   ├── notes/          # 목록 + 에디터
│   ├── todo/           # 할 일
│   ├── search/         # 메모 전문 검색 (Room FTS)
│   ├── ai/             # AI 푸쉬/응답 (Phase 4)
│   ├── settings/       # 설정
│   ├── navigation/     # NavHost · 화면 라우트 정의
│   ├── components/     # 공용 컴포저블
│   └── theme/          # Material 3 테마 (정제된 톤)
├── di/                 # Hilt 모듈
└── platform/           # (Phase 5) WindowManager 폴드, S펜
```

---

## 5. 데이터 모델 (초안)

Phase 1에서 구현. **동기화 메타필드를 처음부터 넣어** Phase 6에서 마이그레이션을 피한다. 삭제는 즉시 삭제가 아니라 `deletedAt` 소프트 삭제로 처리해야 동기화가 깨지지 않는다.

```kotlin
// Room Entity 초안 — Claude Code가 실제 구현 시 조정 가능

@Entity(
    tableName = "notes",
    indices = [Index("date"), Index("deletedAt")]  // 날짜별 조회·소프트삭제 필터 인덱스
)
data class NoteEntity(
    @PrimaryKey val id: String,          // UUID (동기화 대비 클라이언트 생성)
    val title: String,
    val content: String,                 // 마크다운 원문
    val date: Long? = null,              // 캘린더 연결용 (epoch millis, nullable) — 기본 작성 동선의 핵심 키
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    // --- 동기화 메타 (Phase 6 대비, 미리 포함) ---
    val remoteId: String? = null,
    val syncStatus: String = "LOCAL_ONLY", // LOCAL_ONLY | PENDING | SYNCED
    val deletedAt: Long? = null            // 소프트 삭제
)

@Entity(
    tableName = "tasks",
    indices = [Index("dueDate"), Index("noteId"), Index("deletedAt")]
)
data class TaskEntity(
    @PrimaryKey val id: String,          // UUID
    val noteId: String? = null,          // 특정 메모 소속이면 FK, 독립이면 null
    val text: String,
    val isDone: Boolean = false,
    val dueDate: Long? = null,           // 캘린더 연결용 (nullable)
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    // --- 동기화 메타 ---
    val remoteId: String? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val deletedAt: Long? = null
)
```

캘린더 연결(기본 동선): 별도 연결 테이블 없이 `Note.date` / `Task.dueDate`로 단순하게 처리한다. **캘린더에서 날짜를 탭하면 그날의 메모·할 일을 한 번에 조회하고, 그 자리에서 추가(해당 날짜를 `date`로 미리 채움)·삭제(소프트 삭제)한다.** 날짜 없는 메모는 `date = null`로 두고 별도 보조 목록에서 관리한다.

검색: 메모 본문 전문 검색은 Room **FTS(가상 테이블)**로 처리한다. `notes` 원본 테이블은 그대로 두고 `content`(필요 시 `title`)를 인덱싱하는 FTS 그림자 테이블을 추가해, 본문 모델을 건드리지 않고 검색만 얹는다.

---

## 6. 저장소 & 동기화 전략

### 결정: 단계 분리
- **Phase 1~5 동안은 Room 로컬만 사용한다.** 동기화 없이도 매일 쓸 수 있는 앱을 먼저 완성한다. 개발 시작을 막지 않기 위함.
- **Phase 6에서 클라우드 동기화를 추가**하되, 데이터 계층이 Repository로 추상화되어 있으므로 본체 코드 수정은 최소화된다.

### 중요 전제: "로컬을 통째로 클라우드로 대체"하지 않는다
메모 앱에서 모든 읽기·쓰기를 클라우드로 보내면 오프라인에서 멈추고, 입력마다 네트워크 왕복으로 느려지며, 배터리를 소모한다. 정답은 항상 **"로컬 우선 + 클라우드 동기화"** 하이브리드다. 아래 옵션들도 대부분 내부적으로 기기 로컬 DB를 두고 백그라운드 동기화하는 구조다.

### 클라우드 옵션 비교

| 옵션 | 방식 | 개인 프로젝트 비용 | 추천 상황 |
|---|---|---|---|
| **구글 드라이브 앱 폴더** | DB/메모 파일 백업·동기화 | 무료 | 백업·기기 이전이 주목적, 최소 부담 |
| **Firebase Firestore** | NoSQL BaaS, 실시간, 오프라인 캐시 내장 | 무료 티어로 충분* | 구글 생태계 시너지, 실시간 멀티기기 |
| **Supabase** | Postgres BaaS, SQL, 오픈소스 | 무료 티어로 충분* | SQL 선호, 데이터 이식성, self-host 옵션 |
| **PocketBase** | 단일 Go 바이너리 self-host (SQLite) | VM 비용만 (월 몇 천 원대) | Docker로 직접 운영, 데이터 소유 |
| PowerSync / Turso | 오프라인 우선 양방향 동기화 엔진 | 무료~소액 | 본격 오프라인 우선이 핵심 가치일 때 |

\* 개인용 트래픽은 무료 한도로 충분. 유료 구간(월 수만 달러 규모 사용자)은 개인 프로젝트와 무관.

### 권장 순위 (이 프로젝트 기준)
1. **구글 드라이브 앱 데이터 폴더** — Room 유지 + 백업/기기 동기화만 얹음. 구글 캘린더 연동으로 OAuth를 이미 구현하므로 추가 부담 최소, 비용 0. "탭의 메모를 폴드에서도 본다" 수준이면 충분.
2. **Supabase 또는 Firebase** — 실시간 멀티기기 동기화가 필요할 때. SQL 친화성·이식성이면 Supabase, 구글 생태계 통일이면 Firebase.
3. **PocketBase** — 데이터를 본인 서버에 두고 Docker로 직접 굴리고 싶을 때.

PowerSync·Turso는 기술적으로 가장 우아하나 개인 메모 앱엔 인프라 부담이 과해 초기 권장에서 제외.

### 충돌 처리 원칙
같은 메모를 두 기기에서 고쳤을 때의 문제. **처음에는 "마지막 수정 우선(last-write-wins)"으로 단순하게 시작**하고, 사용자가 실제로 같은 레코드를 동시 편집하는 경우에만 필드 단위 병합으로 확장한다.

⚠️ **시계 오차 주의**: last-write-wins는 `updatedAt` 비교에 의존하므로 기기 간 시계 오차(clock skew)에 취약하다. 동기화 도입 시 비교 기준을 **클라이언트 로컬 시각이 아니라 서버 타임스탬프 또는 논리적 버전(단조 증가 카운터)**으로 삼아 "느린 시계의 기기가 최신 편집을 덮어쓰는" 사고를 막는다.

---

## 7. 단계별 로드맵

> 기간은 본업 병행(주당 약 8~12시간) 기준 대략치. Kotlin/Compose 숙련도에 따라 변동.

### Phase 0 — 환경 구축 & 프로젝트 골격
- **Claude Code 작업**: Android 프로젝트 생성, Gradle(Kotlin DSL) 설정, Compose·Hilt·Room 의존성 추가, 위 패키지 구조 골격 생성, 빈 화면이 빌드·실행되게.
- **완료 기준**: `./gradlew assembleDebug` 성공, 실제 갤럭시 기기에서 빈 앱 실행 확인.
- **난이도**: 중 / **예상**: 2~4주(학습 포함)

### Phase 1 — 로컬 MVP (메모 + 할 일)
- **Claude Code 작업**: Room 스키마(Note/Task + FTS) 구현, DAO·Repository·UseCase, 메모 CRUD, **마크다운 편집(`BasicTextField`)·렌더링(라이브러리) 분리 구현**, 체크리스트형 할 일, 메모 전문 검색, Navigation Compose 라우팅, 목록·상세 화면, MVVM 골격.
- **완료 기준**: 메모 작성·수정·삭제 후 앱 재시작에도 유지. 할 일 체크/해제, 본문 검색 동작.
- **난이도**: 중 / **예상**: 3~5주
- **주의**: 에디터를 단순하게 유지(마크다운). 이 단계에서 블록 에디터 욕심 금지.
- **참고**: 이 단계의 목록 화면은 임시 골격이다. **기본 진입점은 Phase 2에서 캘린더로 승격**되므로, 화면 구조를 Navigation으로 느슨하게 잡아 진입점 교체가 쉽게 만든다.

### Phase 2 — 캘린더 통합 (기본 진입점 승격 · 적응형)
- **Claude Code 작업**: Kizitonwose 캘린더를 **홈 화면(기본 진입점)으로 승격**, `Note.date`/`Task.dueDate` 연결, 날짜 선택 → 그날의 메모·할 일 **조회 + 그 자리에서 추가·삭제**(추가 시 선택 날짜를 `date`/`dueDate`에 자동 주입), 날짜 없는 메모용 보조 목록, 마감일 정렬. **적응형 표현은 `WindowSizeClass`로 분기** — Compact(폰·폴드 접힘)=주 단위 세로 아젠다(날짜마다 메모 미리보기), Medium·Expanded(태블릿·폴드 펼침·PC)=월 달력(칸 안에 메모 미리보기, 넘치면 "+N개 더"). 두 뷰는 **같은 선택 날짜 상태를 공유**한다.
- **완료 기준**: 캘린더가 앱의 첫 화면. 특정 날짜 탭 → 그날의 메모·할 일 조회 및 그 자리에서 추가·삭제 동작. 창 너비에 따라 폰=주 아젠다 / 태블릿=월 달력으로 자연 전환.
- **난이도**: 하~중 / **예상**: 2~3주
- **주의**: 분기 기준은 **기기 종류가 아니라 창 너비**(`WindowSizeClass`). 그래야 폴드를 접고 펼 때 주 아젠다↔월 달력이 깨지지 않는다. 2단 master-detail·힌지 정밀 처리는 Phase 5로 미룬다.

### Phase 3 — 구글 캘린더 동기화
- **Claude Code 작업**: Calendar API 연동 코드, Credential Manager 로그인, 읽기/쓰기, 동기화 충돌 처리, 동기화 토글·상태 표시.
- **개발자 작업**: Google Cloud Console에서 프로젝트·OAuth 동의화면·자격증명 설정.
- **완료 기준**: 앱 일정 ↔ 구글 캘린더 양방향 반영.
- **난이도**: 중 / **예상**: 3~4주

### Phase 4 — 생성형 AI 연동 (ChatGPT / OpenAI 단일)
> AI 도구는 **ChatGPT(OpenAI) 하나로 단일화**한다. 여러 제공자(제미나이·클로드)를 동시에 붙이지 않는다. 단, 데이터 계층은 `AiRepository` 인터페이스로 추상화해 **구현체는 OpenAI 하나만** 두고, 나중에 교체가 필요하면 그 구현체만 바꾼다.
- **4-A (먼저, 쉬움)**: Android Sharesheet로 메모 텍스트를 **ChatGPT 앱**에 전달. API 키 불필요.
- **4-B (나중, 중간)**: Ktor/Retrofit으로 **OpenAI REST API** 호출(요약·확장), 응답을 파싱→Room 저장→앱 내 표시. **API 키는 코드에 하드코딩 금지** — 안전한 저장소(예: EncryptedSharedPreferences) 사용.
- **완료 기준**: (4-A) 메모를 ChatGPT 앱으로 한 번에 전송. (4-B) 앱 내에서 OpenAI 요약·확장 결과를 메모에 반영.
- **난이도**: 4-A 하 / 4-B 중 / **예상**: 1~2주 + 2~3주

#### 참고 코드 — 4-B 데이터 계층 골격 (OpenAI 단일)

> 실제 구현 시 조정 가능. 핵심은 **UI·도메인이 `AiRepository`만 보고**, OpenAI 호출·파싱·저장은 구현체 한 곳에 가둔다는 것. 패키지는 `com.<owner>.galaxymemo`. REST는 Retrofit + kotlinx.serialization 기준(필요 의존성: `retrofit`, `retrofit2-kotlinx-serialization-converter`, `kotlinx-serialization-json`, `androidx.security:security-crypto`).

```kotlin
// domain/model — AI 동작은 프롬프트 템플릿을 함께 들고 있다
enum class AiAction(val systemPrompt: String, val instruction: String) {
    SUMMARIZE("너는 한국어 메모 비서다. 핵심만 간결하게 답한다.", "다음 메모를 3줄 이내로 요약해줘."),
    EXPAND("너는 한국어 글쓰기 보조다.", "다음 메모를 자연스럽게 확장해줘."),
    FIX_GRAMMAR("너는 한국어 교정기다. 의미는 유지한다.", "다음 메모의 맞춤법과 문장을 다듬어줘."),
}
data class AiResult(
    val id: String, val noteId: String?, val action: AiAction,
    val sourceText: String, val resultText: String, val model: String, val createdAt: Long,
)

// data/remote/openai — OpenAI Chat Completions 전용 (Retrofit)
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Header("Authorization") bearer: String, @Body body: ChatRequest): ChatResponse
}
@Serializable data class ChatRequest(
    val model: String, val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024, val temperature: Double = 0.7,
)
@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ChatResponse(val choices: List<Choice> = emptyList()) {
    @Serializable data class Choice(val message: ChatMessage)
}

// API 키는 코드 하드코딩 금지 — EncryptedSharedPreferences 뒤로 추상화
interface ApiKeyProvider { fun openAiKey(): String?; fun setOpenAiKey(key: String) }

// data/local — 결과를 Room(진실의 원천)에 저장
@Entity(tableName = "ai_results", indices = [Index("noteId"), Index("createdAt")])
data class AiResultEntity(
    @PrimaryKey val id: String, val noteId: String?, val action: String,
    val sourceText: String, val resultText: String, val model: String, val createdAt: Long,
)
@Dao interface AiResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: AiResultEntity)
    @Query("SELECT * FROM ai_results WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeForNote(noteId: String): Flow<List<AiResultEntity>>
}

// data/repository — UI·도메인은 이 인터페이스만 본다 (구현체 1개)
interface AiRepository {
    suspend fun run(action: AiAction, sourceText: String, noteId: String?): Result<AiResult>
    fun observeResults(noteId: String): Flow<List<AiResult>>
}

class AiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi, private val keys: ApiKeyProvider,
    private val dao: AiResultDao, private val model: String = "gpt-4o-mini", // 교체 가능
) : AiRepository {
    override suspend fun run(action: AiAction, sourceText: String, noteId: String?) = runCatching {
        val key = keys.openAiKey() ?: error("OpenAI API 키 미설정")
        // ① 보내기: 도메인 동작 → OpenAI 메시지
        val res = api.chat("Bearer $key", ChatRequest(model, listOf(
            ChatMessage("system", action.systemPrompt),
            ChatMessage("user", "${action.instruction}\n\n$sourceText"),
        )))
        // ② 받기: 응답 파싱
        val text = res.choices.firstOrNull()?.message?.content?.trim() ?: error("빈 응답")
        // ③ 저장: Room 기록 후 도메인 모델 반환 → Flow로 화면 자동 갱신
        AiResult(UUID.randomUUID().toString(), noteId, action, sourceText, text, model,
            System.currentTimeMillis()).also { dao.upsert(it.toEntity()) }
    }
    override fun observeResults(noteId: String) =
        dao.observeForNote(noteId).map { l -> l.map { it.toDomain() } }
}

// domain/usecase
class RunAiActionUseCase @Inject constructor(private val repo: AiRepository) {
    suspend operator fun invoke(action: AiAction, text: String, noteId: String?) =
        repo.run(action, text, noteId)
}
```

> 단일화 핵심: 제공자 분기·전략 패턴 없이 `AiRepositoryImpl`이 OpenAI를 직접 호출한다. 다른 도구로 바꿀 일이 생기면 이 구현체 한 파일만 교체한다. 응답을 화면에 직접 쓰지 않고 Room을 거치므로 AI 결과도 오프라인·재시작에 유지된다.

**ui/ai — ViewModel·화면 골격** (위 데이터 계층을 화면에 연결)

```kotlin
// ui/ai — 화면 상태는 한 번에 하나
sealed interface AiUiState {
    data object Idle : AiUiState
    data class Loading(val action: AiAction) : AiUiState
    data class Success(val result: AiResult) : AiUiState
    data class Error(val message: String) : AiUiState
}

@HiltViewModel
class AiViewModel @Inject constructor(
    private val runAiAction: RunAiActionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun run(action: AiAction, sourceText: String, noteId: String?) {
        if (_uiState.value is AiUiState.Loading) return   // 중복 호출 방지
        _uiState.value = AiUiState.Loading(action)
        viewModelScope.launch {
            runAiAction(action, sourceText, noteId)
                .onSuccess { _uiState.value = AiUiState.Success(it) }
                .onFailure { _uiState.value = AiUiState.Error(it.message ?: "AI 호출 실패") }
        }
    }
    fun reset() { _uiState.value = AiUiState.Idle }
}

// 화면: 동작 칩(요약·확장·교정) → Loading → Success/Error.
// "메모에 반영"은 결과를 노트 본문에 쓰는 별도 콜백(onApplyToNote).
@Composable
fun AiPanel(
    sourceText: String, noteId: String?,
    onApplyToNote: (String) -> Unit,
    viewModel: AiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 동작 칩 onClick → viewModel.run(action, sourceText, noteId)
    // Success → 결과 카드 + "메모에 반영"(onApplyToNote) / 복사 / 다시(reset)
}
```

> ViewModel은 `RunAiActionUseCase`만 호출하고 OpenAI를 전혀 모른다. 결과는 Repository가 이미 Room에 저장했으므로 "메모에 반영"은 노트 본문 갱신만 담당한다.

### Phase 5 — 갤럭시 폴리시 (폴드 · S펜 · 대화면)
- **Claude Code 작업**: WindowManager로 폴드 상태 감지(접힘/플렉스 레이아웃), Expanded에서 **월 달력 + 우측 상세 마스터-디테일 2단**(칸 탭 → 그날 펼침), S펜 필기(Compose Canvas → 필요 시 ML Kit 텍스트 변환), S펜 버튼 단축(선택).
- **완료 기준**: 폴드 접고 펼 때 **주 아젠다↔월 달력** 자연 전환, 탭/폴드 대화면에서 좌측 달력·우측 상세 동시 표시.
- **난이도**: 중 / **예상**: 3~5주

### Phase 6 — 동기화 & 배포
- **Claude Code 작업**: 6장에서 택1한 동기화(드라이브/Supabase/Firebase/PocketBase) Repository 구현, 앱 아이콘·스플래시·설정 정리.
- **개발자 작업**: (BaaS 선택 시) 콘솔 설정, 내부 테스트 배포.
- **완료 기준**: 탭과 폴드에서 같은 데이터 표시.
- **난이도**: 중~상 / **예상**: 3~6주(범위에 따라 변동)

---

## 8. 리스크 & 의사결정 포인트

| 리스크 / 결정 | 권장 방향 |
|---|---|
| **블록 에디터 함정** | 마크다운으로 우회. 노션 완전 복제 금지 (최대 리스크) |
| **마크다운 편집/렌더 혼동** | 편집=`BasicTextField`, 렌더=라이브러리로 역할 분리. Markwon은 렌더 전용(편집 불가) |
| **캘린더 진입점 전환** | Phase 1 화면을 Navigation으로 느슨히 구성 → Phase 2에서 홈을 캘린더로 교체 쉽게 |
| **적응형 레이아웃 분기** | 기기 종류가 아니라 창 너비(`WindowSizeClass`)로 분기. 폴드 접힘=Compact=주 아젠다, 펼침=월 달력(칸 내 미리보기) |
| **AI 연동 방식** | **ChatGPT(OpenAI) 단일** · 공유(4-A)로 시작 → 필요 시 API(4-B). 다중 제공자 금지 |
| **동기화 방식** | Phase 6에서 결정. 1순위 구글 드라이브 → 2순위 Supabase/Firebase |
| **동기화 충돌** | last-write-wins로 시작, 필요 시 필드 병합 |
| **동기화 시계 오차** | last-write-wins는 `updatedAt` 의존 → 동기화 시 서버/논리적 타임스탬프 기준 사용 |
| **번아웃** | Phase 1까지만 완성해도 "매일 쓸 수 있는 앱". 단계별 완성을 작은 목표로 |

---

## 9. 첫 시작 — Claude Code 명령 예시

프로젝트 루트에 이 문서를 두고(파일명을 `CLAUDE.md`로 바꾸면 Claude Code가 자동 참조), 아래처럼 시작:

```
# 첫 명령 예시
이 명세서(@CLAUDE.md)를 읽고 Phase 0를 진행해줘.
Jetpack Compose + Hilt + Room 의존성을 포함한 Android 프로젝트 골격을 만들고,
명세서 4장의 패키지 구조대로 디렉토리를 잡고, 빈 화면이 빌드되게 해줘.
빌드는 ./gradlew assembleDebug 로 확인해줘.
```

```
# Phase 1 진입 예시 (Phase 0 빌드 통과 후)
명세서 5장의 데이터 모델대로 Room 스키마(Note, Task)와 DAO,
그리고 NoteRepository 인터페이스+구현을 만들어줘. 동기화 메타필드와 @Index,
본문 검색용 FTS 가상 테이블도 포함. 마크다운은 편집(BasicTextField)과
렌더링(라이브러리)을 분리해서 구현해줘.
```

**팁**: 한 번에 한 단계씩. 각 단계 후 빌드 통과를 확인하고 다음으로. Claude Code가 빌드 에러를 만나면 그 자리에서 고치게 두되, 명세서의 4가지 설계 원칙(2장)에서 벗어나는 변경은 막을 것.

---

## 10. 학습 리소스 방향
- Android 공식 문서의 Jetpack Compose 입문 + Now in Android 샘플 앱 구조
- Room(+ FTS) / Navigation Compose / DataStore / WindowManager / Calendar API 각 공식 가이드
- Kizitonwose Calendar, Markwon(렌더 전용) 등 라이브러리 README 및 샘플
- (필요 시) Supabase / Firebase Android SDK 공식 문서

---

_본 명세서는 1인 개발·본업 병행을 가정한 로드맵입니다. 각 Phase는 독립적으로 "쓸 수 있는 결과물"을 남기도록 설계되어, 중간에 멈춰도 손해가 적습니다. Phase 1까지만으로도 충분히 실용적인 앱이 됩니다._

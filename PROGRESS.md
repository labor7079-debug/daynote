# 진행 상황 (체크포인트)

> 내일 이어가기 위한 인수인계 메모. 상세 명세는 [CLAUDE.md](CLAUDE.md), 빌드 절차는 [BUILD.md](BUILD.md).
>
> _최종 업데이트: 2026-07-03_

## 현재 위치: **v0.5.1 업데이트 업로드 대기(2026-07-03)** — PC 구글 캘린더 동기화(데스크톱 OAuth) + 드래그 메모↔To-Do 전환 포함 전체 산출물 빌드 완료

### 🔼 v0.5.1 — PC(데스크톱) 구글 캘린더 동기화 + 드래그 전환 (2026-07-03)
- **데스크톱 OAuth(PKCE 루프백)**: 설정의 "구글 로그인" → 시스템 브라우저 동의 → `http://127.0.0.1:{임시포트}` 콜백 → 토큰 교환. 리프레시 토큰을 `~/.daynote/secure.properties`(DesktopSecureStore)에 저장해 **재시작에도 무음 갱신**(브라우저 재동의 불필요). 새 의존성 0(ServerSocket + HttpURLConnection + kotlinx-serialization). `DesktopGoogleAuth.kt`(신규).
- **동기화 본체 commonMain 승격**: androidMain `CalendarApi` → 공용 `GoogleCalendarApi`(Ktor, 양 플랫폼) + push/공유 캘린더 pull 로직 → `GoogleCalendarSyncCore`(공용). Android/Desktop 매니저는 **인증(토큰)만 담당**하고 본체는 코어에 위임 — PC 에서도 push·공유 캘린더 pull·"표시할 캘린더" 전부 동작. 스코프 상수도 공용(`GOOGLE_CALENDAR_SCOPES`).
- **클라이언트 주입**: `keystore.properties` 의 `googleDesktopClientId`/`googleDesktopClientSecret` → 빌드 태스크(`generateDesktopOAuthProps`)가 데스크톱 리소스로 주입 → 런타임 로드(`DesktopOAuthConfig`). 값이 없으면 데스크톱 동기화 비활성(설정에 안내문). **발급값 추가 완료(2026-07-03) — 산출물에 주입 확인.**
- **드래그로 메모↔To-Do 전환**(사용자 요청): 캘린더 상세에서 각 행 왼쪽의 **핸들(⋮⋮)을 끌어** 반대 섹션에 놓으면 전환 — 마우스·터치 공용. 끄는 동안 포인터를 따라다니는 고스트 칩("제목 → TO-DO")과 대상 섹션 하이라이트 표시. 메모→To-Do 는 제목+본문을 할 일 텍스트로(첫 줄이 표시 제목), To-Do→메모는 첫 줄=제목·나머지=본문. 원본은 소프트 삭제(동기화 안전). 행 본문의 탭/길게 누르기(이동·복사 메뉴)와 충돌하지 않도록 **핸들에서만 드래그 시작**. `CalendarViewModel.convertNoteToTask/convertTaskToNote` + `DragConvertRow`(CalendarScreen) + `DragHandleIcon`.
- **버전**: versionCode **10** / versionName **0.5.1** / MSI **1.4.2**.
- **산출물**: AAB `app/build/outputs/bundle/release/app-release.aab`(30.6MB, 서명) · MSI `app/build/compose/binaries/main/msi/DayNote-1.4.2.msi` · 폴더형 `.../app/DayNote/`.
- **개발자 할 일**: ① Play Console → 내부 테스트 → `10 (0.5.1)` AAB 업로드(4~9 건너뜀). ② PC 는 MSI 1.4.2 설치 → 설정 → 구글 캘린더 동기화 → 동기화 사용 ON → **"구글 로그인"(브라우저 동의 1회)** → "캘린더 목록 불러오기" → 공유 캘린더 체크 → 달력에 표시 확인. ③ 재시작 후 로그인 없이 자동 동기화되는지 확인(무음 갱신). ④ 캘린더 상세에서 메모/할 일 행의 ⋮⋮ 핸들을 끌어 반대 섹션에 놓아 전환 확인(마우스·터치).

## (이전) v0.4.1 — 구글 공유 캘린더 표시 + 자동 동기화 + 아이콘 UI + AI 이력(브랜치 병합) + v0.3.x(위젯·시각 입력·세리프) 포함

### 🔼 v0.4.1 업데이트 (2026-07-03 — 업로드 대기, v0.3.1~v0.4.0 미업로드분 포함)
- **구글 캘린더 "표시할 캘린더"(공유받은 캘린더 포함, 읽기 전용)** — 구글 캘린더 사이드바("다른 캘린더") 컨셉:
  - 설정 → 구글 캘린더 동기화 → **표시할 캘린더**: "캘린더 목록 불러오기" → 캘린더별 **색 점 + 체크박스**(공유 캘린더 포함, `calendarList.list`). 체크 즉시 영속(`google_visible_calendars`) + 동기화로 반영.
  - **pull**: `syncNow()` 가 push 후 체크된 캘린더의 이벤트를 오늘 −60일~+180일 창으로 읽어(`events.list`, singleEvents) 로컬 캐시 `external_events`(Room **v6**, `MIGRATION_5_6`)에 캘린더 단위 통째 교체. 체크 해제 시 캐시 삭제. 오프라인에선 마지막 pull 결과 표시.
  - **표시**: 월 달력 칸·주 아젠다에 **캘린더 색 점 + 제목** 줄(펼침/"+N개 더" 연동), 상세엔 **GOOGLE 섹션**(색 점·제목·시각·캘린더명). 읽기 전용 — 앱에서 수정/삭제 불가, 앱 메모/할 일과 분리.
  - **스코프 추가**: `calendar.calendarlist.readonly` (기존 `calendar.events` 에 더해). ⚠️ **기존 로그인 계정은 "구글 로그인"을 한 번 다시 눌러 재동의 필요**(403 시 안내 문구 표시). 데스크톱은 기존대로 미지원(빈 캐시).
  - 새 파일: `ExternalEventEntity/Dao`, `ExternalEvent`(domain), `ExternalEventRepository`, `ObserveExternalEventsByDateUseCase`, `ui/components/ColorExt.kt`. 수정: `CalendarApi`(listCalendars/listEvents), `AndroidCalendarSyncManager`(pullExternal), `CalendarSyncManager`(listCalendars 기본 구현), Settings 화면/VM, Calendar 화면/VM.
- **구글 캘린더 자동 동기화**: 메모/To-Do 를 추가·수정·삭제(체크 포함)하면 **수동 "지금 동기화" 없이도** 자동 반영 — 기존 `AutoSyncCoordinator`(앱 시작 1회 + 로컬 변경 2.5초 디바운스, 클라우드용)에 `CalendarSyncManager.syncNow()` 를 함께 연결(토글 켜짐 + Android 일 때만). push 와 공유 캘린더 pull 이 같은 경로로 돈다. 모든 쓰기 경로는 이미 `LocalChangeNotifier` 를 발행하고 있어 조율기 연결만으로 완성.
- **한글 텍스트 버튼 → 표준 아이콘**(사용자 요청): `AppIcons`(의존성 0 커스텀 벡터)에 ←·＋·휴지통·✕·돋보기·펜 추가. 뒤로(상단바 4곳)·추가(4곳, primary 색)·삭제(에디터)·검색(메모 목록)·필기(에디터)·닫기(AI 카드)·지우기(기간/종료일) 교체. 다이얼로그 취소/저장/나가기·하단 저장·"오늘" 등은 텍스트 유지. 전부 contentDescription(한글) 포함.
- **AI 결과 이력 UI 병합**(과거 클라우드 세션 브랜치 `claude/session-history-review-jtdzf6` 를 main 위로 리베이스 후 ff 병합): 메모별 "지난 AI 결과 N" 접기/펼치기 — 항목마다 타임스탬프("M월 D일 HH:mm")·질문(Q.) 표시·"메모에 반영"·삭제(`DeleteAiResultUseCase`·`AiRepository.deleteResult`, 테스트 포함). 남은 작업의 「AI 이력("이전 결과 보기")」 해결.
- **CI**: PR 자동 검증 잡(컴파일 + 데스크톱 단위 테스트, `.github/workflows/build.yml`) + gradlew 실행 권한(100755) — 같은 브랜치에서 병합.
- **버전**: versionCode **8** / versionName **0.4.1** / MSI **1.3.1**.
- **산출물**: AAB `app/build/outputs/bundle/release/app-release.aab`(서명) · MSI `app/build/compose/binaries/main/msi/DayNote-1.3.1.msi` · 폴더형 `.../app/DayNote/`.
- **개발자 할 일**: ① Play Console → 내부 테스트 → 새 버전(`8 (0.4.1)`) AAB 업로드(4~7 은 건너뜀). ② PC 는 MSI 1.3.1 실행(제자리 업그레이드). ③ (필요 시) Google Cloud Console → OAuth 동의화면 → 범위에 `calendar.calendarlist.readonly` 추가 — 테스트/개인 사용은 앱 내 재동의만으로 대체로 동작. ④ 기기 검증: 설정에서 목록 불러오기 → 공유 캘린더 체크 → 지금 동기화 → 달력 칸/상세에 색 점과 함께 표시·체크 해제 시 사라짐 · v0.3.x 항목(위젯 채움+위쪽 정렬·시:분·세리프) 회귀 확인.

## (이전) v0.3.2 — 위젯 가득 채움(항목은 위쪽 정렬)·시각 입력 '분' 잘림 수정·세리프 글씨체 전앱 통일(Noto Serif KR 내장)

### 🔼 v0.3.2 업데이트 (2026-07-03 — 업로드 대기, v0.3.0·v0.3.1 미업로드분 포함)
- **위젯 내용 가득 채움**(기기 스크린샷 피드백): 두 위젯 모두 내용이 위에 몰리고 아래가 비던 문제 →
  ① 월 위젯 미니 달력: 그리드가 남은 높이 전체 차지 + 6주 행이 weight 균등 분할(`widget_month.xml`·`widget_month_row.xml`) — 기본 크기에서 잘리던 마지막 주도 항상 표시, 위젯을 늘리면 달력도 같이 늘어남.
  ② 항목 줄(하루 6줄·월 우측 4줄): **위쪽부터 차곡차곡 쌓임**(2차 피드백 — 처음 weight 슬롯 균등 분할로 만들었더니 "중간 정렬로 보인다" → 위쪽 정렬로 회귀). 빈 날 안내문만 남은 영역 세로 중앙.
- **To-Do 시각 입력 '분' 잘림 수정**: `NumberDropdown` 컴팩트화 — 드롭다운 화살표 제거+숫자 가운데 정렬, 폭 92/96→64dp. "종료 시각" 줄(라벨+토글+시:분)이 폰 다이얼로그 폭에 들어감. 탭=드롭다운·직접 입력 그대로. (EditTaskDialog·캘린더 상세 추가란 공용)
- **세리프 글씨체 전앱 통일**(사용자 요청 — 초기 Warm Journal 세리프로 회귀·확대): v0.3.0 에서 "기본체 통일"로 세리프를 제거했던 방향을 뒤집어, **Noto Serif KR(OFL) 3종(Regular/Medium/Bold)을 내장**(`app/src/commonMain/composeResources/font/`, +약 22MB)하고 Material3 타입 스케일 15종 전부에 적용(`Type.kt` `dayNoteTypography()`). PC(Windows)는 시스템 세리프가 한글 미지원(맑은 고딕 폴백)이라 내장이 필수. Android 홈 위젯도 `android:fontFamily="serif"`(시스템 Noto Serif) 통일. `compose.resources.packageOfResClass = com.kangtaeyoung.daynote.resources`. 라이선스 고지: `THIRD_PARTY_LICENSES.md`.
- **버전**: versionCode **6** / versionName **0.3.2** / MSI **1.2.1**(위쪽 정렬 수정은 Android 위젯 전용이라 PC 는 1.2.1 그대로 최신 — MSI 재빌드 불필요).
- **산출물**: AAB `app/build/outputs/bundle/release/app-release.aab`(서명, versionCode 6, 30.6MB — 폰트 내장으로 증가) · MSI `app/build/compose/binaries/main/msi/DayNote-1.2.1.msi`(85.3MB) · 폴더형 `.../app/DayNote/`.
- **개발자 할 일**: ① Play Console → 내부 테스트 → 새 버전(`6 (0.3.2)`) AAB 업로드(4·5 는 건너뜀). ② PC 는 MSI 1.2.1 실행(제자리 업그레이드) 또는 폴더형 실행 — 이미 했다면 재설치 불필요. ③ 기기 검증: 위젯 두 종(달력 가득 채움·항목 위쪽 정렬·세리프 — 기존 배치분은 삭제 후 재배치 권장)·할 일 수정 다이얼로그 시:분 표시·앱 전체 세리프·v0.3.0 항목(수정 다이얼로그·우클릭·휠 내비) 회귀 확인.

## (이전) v0.3.0 — To-Do 수정·종료시각·우클릭 메뉴·휠 내비, AAB(versionCode 4)·MSI 1.2.0 빌드됨(미업로드, v0.3.1 에 포함)

### 🔼 v0.3.0 업데이트 (2026-07-02 저녁 — 업로드 대기, v0.2.1 은 미업로드 상태에서 통합됨)
- **To-Do 수정 기능**: 길게 누름/우클릭 메뉴에 **"수정"** — `ui/components/EditTaskDialog.kt`(신규). 내용·시작 날짜·종일/시작 시각·**종료 시각**·**종료일**(하루 초과 기간)을 한 번에 수정. 날짜 없는 할 일은 일정을 안 건드리면 계속 날짜 없음(scheduleTouched 가드). 캘린더 상세·To-Do 탭·메모 에디터 3곳 전부 연결(NoteEditorViewModel 에 UpdateTaskUseCase 추가).
- **마우스 우클릭 = 길게 누름 메뉴**(PC): `ui/components/PointerExt.kt`(신규) `Modifier.onRightClick` — 공용 코드(PointerEventType.Press + buttons.isSecondaryPressed), 터치는 자연 no-op. TaskRow 텍스트에 적용.
- **할 일 추가란 종료 시각**: 캘린더 상세 TaskQuickAdd 에 "종료 시각" 스위치+시/분 — 종료일 없이 종료 시각만 주면 **같은 날 시각 범위**(예 14:00~16:00). `addTaskForSelectedDate(..., endHour, endMinute)`.
- **endDate 의미 확장(스키마 변경 없음, DB v5 유지)**: 날짜만=자정 millis(기존), **시각까지=그 시각 millis**(신규). `Task.spansDays()`(EditTaskDialog.kt)로 "하루 초과"만 캘린더 bar — 같은 날 시각 범위는 칩("14:00~16:00" 표기, TaskRow·TaskLineChip). `core/DateUtils`: isMidnight/hourOfDay/minuteOfHour 추가. NumberDropdown 은 `ui/components/NumberDropdown.kt` 로 승격(공용).
- **휠 스크롤 캘린더 내비**(PC·태블릿): 캘린더 영역에서 휠 아래=다음 주/월, 위=이전(navDir 슬라이드 애니 그대로). Scroll 이벤트 consume 으로 2단 레이아웃 세로 스크롤과 충돌 없음.
- **버전**: versionCode **4** / versionName **0.3.0** / MSI **1.2.0**.
- **산출물(17:59)**: AAB `app/build/outputs/bundle/release/app-release.aab`(13.4MB, 서명) · MSI `app/build/compose/binaries/main/msi/DayNote-1.2.0.msi`(68.1MB) · 폴더형 `.../app/DayNote/`.
- **개발자 할 일**: ① Play Console → 내부 테스트 → 새 버전 만들기 → AAB 업로드(`4 (0.3.0)`) → 출시 노트 → 저장 및 출시. ② PC 는 MSI 실행(제자리 업그레이드). ③ 기기 검증: 수정 다이얼로그(시각 범위·기간)·우클릭 메뉴(PC)·종료 시각 추가·휠 내비·기존 기간 bar 회귀 확인.

### ✅ Play 내부 테스트 첫 배포 완료 (2026-07-02 오전)
- 개발자 계정 승인 → 앱 생성 → **AAB(versionCode 1) 내부 테스트 게시** → 테스터 등록 → **갤럭시 기기에 스토어 경유 설치 완료**.
- **Play 앱 서명 SHA-1 → Google Cloud OAuth Android 클라이언트 추가**(스토어 설치본 캘린더 로그인용. 이제 3개: 디버그/릴리스/Play서명). **OAuth 동의화면 프로덕션 게시.**
- **앱 콘텐츠 11/11 + 스토어 등록정보 완료** — 전부 1회성, 업데이트 때 재작업 없음. 핵심 답: 앱 액세스="제한 없음(아니오)" · 데이터보안=계정생성 안 함/외부 계정 로그인 예/삭제 예(방침 URL) · 유형 3개(이메일·캘린더 일정·기타 사용자 생성 콘텐츠) 수집+공유·앱 기능·선택.
- ⚠️ 프로덕션(정식 공개)은 개인 계정 요건: **비공개 테스트 테스터 20명·14일** 선행. 본인용은 내부 테스트로 충분(무기한, 한 번 참여한 테스터는 자동 업데이트).

### 🔼 v0.2.1 업데이트 (2026-07-02 저녁 — 업로드 대기, v0.2.0 미업로드분 포함)
- **포함**: v0.2.0 내용 전부 — 기간 할 일 seamless bar(`e8edf16`) + UI 5종(`62b719d`) — 에 더해:
  기간 bar 시작·종료 끝 칸 안쪽 마감(타일 밖으로 안 삐져나옴) · 월 달력 칸·주 아젠다 "+N개 더" 탭 → 드롭다운 펼침/"접기" · 홈 위젯 기본 크기 절반(하루 4x2→2x2 정사각형, 월 4x3→4x2 가로형, 최소 크기 완화).
- **버전**: versionCode **3** / versionName **0.2.1** / 데스크톱 packageVersion **1.1.1**(MSI 제자리 업그레이드용 — 앞으로 업데이트마다 함께 범프).
- **산출물**: AAB `app/build/outputs/bundle/release/app-release.aab`(서명) · **MSI `app/build/compose/binaries/main/msi/DayNote-1.1.1.msi`** · 폴더형 `.../app/DayNote/`.
- **개발자 할 일**: ① Play Console → 내부 테스트 → 새 버전 만들기 → AAB 업로드(`3 (0.2.1)` 자동 인식) → 출시 노트 → 저장 및 출시. (v0.2.0 AAB 는 업로드하지 않음 — 건너뜀) ② PC는 MSI 실행(기존 설치 위에 업그레이드, SmartScreen "추가 정보→실행"). ③ 기기 검증 체크리스트(아래 ①) + v0.2.0·v0.2.1 신규 항목 확인. ④ 위젯은 기존 배치분 크기가 자동으로 안 바뀜 — 길게 눌러 크기 조절(이제 2x2/4x2 까지 줄어듦) 또는 삭제 후 재배치.

### 🌿 브랜치 `feature/multi-ai-provider` (main 미병합, 5bbe47c)
- AI 를 OpenAI 호환 다중 제공자(baseUrl·모델 설정화, Gemini/Groq/OpenRouter/로컬)로 확장한 작업. 빌드·테스트 통과. 병합 여부 사용자 결정 대기. (이 커밋에 당시 PROGRESS/CLAUDE.md 배포 기록도 포함됨)

## 🚦 다음 세션 시작 가이드 (2026-07-03 마감 기준)

**상태**: 작업트리 clean(카카오톡 스크린샷 2장만 의도적 미커밋), `dd792ae` 푸시됨. **assembleDebug + desktopTest 통과.** DB 스키마 **v6**(v5→v6: `external_events` — 구글 공유 캘린더 캐시). **v0.5.1(versionCode 10, MSI 1.4.2) 산출물 빌드 완료 — 업로드 대기.**

**① 기기·PC 검증 대기(개발자 — 새 버전 설치 후 한 번에)**:
- [ ] 마이그레이션: 업데이트 설치 후 기존 메모·할 일 유지 (v5→v6)
- [ ] **PC 구글 로그인(신규)**: 설정 → 동기화 사용 ON → "구글 로그인" → 브라우저 동의 → "로그인 완료" 페이지 → 앱 상태 "로그인됨" · **재시작 후 로그인 없이 동기화**(무음 갱신)
- [ ] **공유 캘린더 표시(신규, 폰+PC)**: "캘린더 목록 불러오기" → 공유 캘린더 체크 → 월 달력 칸·주 아젠다에 색 점+제목, 상세에 GOOGLE 섹션 · 체크 해제 시 사라짐 · ⚠️ 기존 Android 로그인 계정은 재동의 1회 필요(calendarlist 스코프)
- [ ] **자동 동기화(신규)**: 메모/To-Do 추가·수정·삭제 → 수동 버튼 없이 몇 초 내 구글 캘린더 반영
- [ ] **드래그 전환(신규)**: 캘린더 상세에서 행의 ⋮⋮ 핸들을 끌어 반대 섹션에 놓기 → 메모↔To-Do 전환(마우스·터치) · 고스트 칩·섹션 하이라이트 · 기존 탭/길게 누르기 무충돌
- [ ] **세리프 글씨체**(전앱 Noto Serif KR) · **아이콘 버튼**(←·＋·휴지통·돋보기·펜·✕) · **AI 이력**("지난 AI 결과 N" 펼침·삭제·Q. 표시)
- [ ] 위젯 2종: 달력 가득 채움·항목 위쪽 정렬·세리프(기존 배치분은 삭제 후 재배치 권장)
- [ ] 할 일 수정 다이얼로그: 종료 시각 시:분 잘림 없이 표시

**② 다음 작업 후보(우선순위 제안)**:
1. **Supabase `end_date` 컬럼 추가**(개발자: 대시보드에서 `ALTER TABLE tasks ADD COLUMN end_date bigint;`) → 클라이언트 RowMappers 반영 — 기간 할 일 멀티기기 동기화
2. **반복 일정**(DB v6→v7 + Supabase 컬럼 + UI) — 세션급
3. **구글 캘린더 pull(내 primary 캘린더 양방향)** — 공유 캘린더 read-only pull 은 완료, 내 캘린더 이벤트를 앱 항목으로 가져오는 양방향은 미구현(3-B3c)
4. 잔여 폴리시: 5-B 폴드 힌지 정밀 · 5-C3 ML Kit 필기 변환 · 데스크톱 .exe 아이콘

**③ 브랜치 정리 상태**:
- `feature/multi-ai-provider`(로컬, 0574a05): AI 다중 제공자 실험 — **병합 여부 사용자 결정 대기**(CLAUDE.md "OpenAI 단일" 원칙과 상충). main 과 크게 벌어져 병합 시 리베이스 필요.
- `origin/claude/session-history-review-jtdzf6`: **내용 전부 main 에 병합 완료(리베이스)** — 원격 브랜치는 삭제해도 됨.

**④ 참고**:
- 공휴일 테이블(`domain/holiday/KoreanHolidays.kt`) 2025~2027 내장 — 매년 6월 말 월력요항 발표 시 한 해치 추가.
- **릴리스 절차(관례)**: versionCode·versionName·MSI packageVersion 3종 함께 범프 → `bundleRelease` + `packageMsi`/`createDistributable`("-Pdaynote.jpackage.jdk=<JDK25>") → PROGRESS 갱신 → 커밋·push. 사용자가 "버전 N"이라 하면 versionCode=N. createDistributable 전엔 실행 중인 DayNote.exe 정상 종료 필요.
- **데스크톱 OAuth**: keystore.properties 의 `googleDesktopClientId/Secret` → 빌드 태스크가 리소스 주입. 값 변경 시 데스크톱 배포물 재빌드.

**오늘(2026-07-02) 한 것 — 사용자 피드백 반영**: 🐞**캘린더 미표시 버그 수정**(에디터 To-Do가 메모 날짜를 dueDate로 상속 + 새 메모는 날짜 없으면 오늘 자동 주입) · **구글 캘린더 무음 재로그인**(세션마다 로그인 불필요) · **저장/추가 스낵바 피드백** · **당겨서 새로고침 → 동기화** · **비슷한 메모 실시간 추천**(로컬 FTS) · **AI 제목 자동생성 기본 켜짐**(`f18f6e6`) · **메모·To-Do 목록 날짜 표시+일자별 그룹+기간 필터**(전체/오늘/7일/30일 — 공용 `ui/components/DateGrouping.kt`(Period·PeriodFilterRow·DateGroupHeader)로 추출해 메모/To-Do 탭 공유, `NoteListItem` 날짜 라벨은 검색 화면에도 적용) · **미니 캘린더 팝업 + 길게 누름 이동/복사/삭제**(`ui/components/ItemActions.kt`: `WithItemActions`+`MiniCalendarDialog` — To-Do 텍스트/메모 날짜라벨 탭=해당 일자 강조 팝업, 길게 누름=캘린더 보기·다른 날짜로 이동·복사·삭제 메뉴. 메모탭·검색·To-Do탭·캘린더 상세·에디터 전부 적용, 시간 지정 할 일은 이동 시 시:분 유지, 신규 `UpdateTaskUseCase`·`core.movedToDate`) · **공휴일 빨간 표시(2025~2027)**(`domain/holiday/KoreanHolidays.kt` 정적 테이블 — 「관공서의 공휴일에 관한 규정」+우주항공청 월력요항 기준, 월달력 칸 공휴일명·주 아젠다·상세 헤더·미니 캘린더 팝업에 클레이 표시. 2026 제헌절 재지정·노동절 신설 등 최신 개정 반영, **매년 6월 말 월력요항 발표 시 한 해치 수동 추가 필요**. 테스트 +2) · **달력 칸 할 일 내용 박스 표기**(`TaskLineChip` — "할 일 N개" 카운트 대신 내용을 옅은 회색 라운드 박스로, 월 달력·주 아젠다 표기 통일, 칸당 2개+"+N개 더", 완료=취소선. AI 요약은 렌더마다 호출하면 오프라인·비용 문제라 첫 줄 말줄임으로 대체 — 필요 시 저장 시점 요약 캐시로 확장 여지) · **홈 화면 위젯**(Android, `widget/DayNoteWidgetProvider` — 오늘 날짜+공휴일명+메모·할 일 6줄(할 일=옅은 박스·완료 ✓ 흐림)+"+N개 더"+⊕(앱 열기). RemoteViews·**신규 의존성 0**, 웜 본 배경+values-night 다크 대응. 갱신=30분 주기+`DayNoteApp` 이 LocalChangeNotifier 구독(디바운스 1.5s)→즉시+시간/시간대 변경. ⚠️기기에서 위젯 배치·갱신 확인 필요) · **월 그리드 위젯**(`widget/DayNoteMonthWidgetProvider`, 4x3 — 좌 미니 월달력(오늘=슬레이트 마커·일요일/공휴일=클레이·항목 있는 날=밑줄+굵게·`RemoteViews.addView`로 6주 그리드 구성) + 우 오늘 항목 4줄+⊕. 위젯 목록에 "DayNote"/"DayNote 월 달력" 2종) · **종일 할 일 기본시각 알림**(`TaskDao.getAllDayUpcoming`+`ReminderCoordinator` — 종일 할 일도 마감일 오전 9시 알림, 지난 시각 건너뜀. 테스트 +1) · **AI 결과 이력 UI**(AiPanel 하단 "이전 결과 N개 보기" — 메모별 과거 결과 열람·펼침·메모에 반영, `ObserveAiResultsUseCase` 드디어 사용). **남은 후속: 반복 일정·구글 캘린더 pull**(DB/Supabase 스키마 변경+기기 검증 필요 → 별도 세션 권장) · **추가 4건(사용자 요청)**: ① 상세 영역 좌우 스와이프=전날/다음날 이동(1단·2단 공통, `detailSwipe`) ② 주 헤더 ISO 연중 주차 병기 "6월 29일 주 (W27)"(`core.isoWeekNumber`, 테스트 +1) ③ 공휴일 칸에 주말과 같은 음영 타일(월달력·주아젠다) ④ **기간 할 일**(DB **v4→v5** `tasks.endDate` 추가·데이터 보존 마이그레이션 — 상세 할일 추가란 "기간: 종료일 지정"(미니캘린더), 월달력에 슬레이트 **bar 가 시작~종료에 걸쳐** 표시(`TaskBarSegment`, 시작칸만 제목·양끝만 라운드·완료=반투명), DAO 날짜조회를 겹침(COALESCE) 의미로 변경, TaskRow "9/14~9/16" 라벨, 이동/복사 시 기간 길이 유지, 백업 포함, 월 위젯 밑줄도 기간 전체. ⚠️**Supabase 는 endDate 미동기화**(서버 `tasks.end_date` 컬럼 추가 후 TaskRow 반영 필요 — pull 이 로컬 endDate 를 지우지 않게 보존 처리됨). 테스트 +2). 상세는 아래 "사용자 피드백 2차 수정" 참조.

### 🔧 사용자 피드백 2차 수정 ✅ (2026-07-02)
> 기기 사용 피드백 6건. **assembleDebug + desktopTest(27건, 신규 2건 포함) 모두 BUILD SUCCESSFUL.**

- 🐞 **① 메모·To-Do가 캘린더에 안 뜸(가장 중요)** — 근본 원인 2개: (a) 에디터의 "할 일 추가"가 `dueDate=null`로 저장 → `observeByDueDateRange`(dueDate 범위 쿼리)에 영원히 안 걸림. (b) 메모 탭에서 만든 새 메모가 `date=null` → 캘린더 조회 제외. **수정**: `NoteEditorViewModel.noteDate()`(기존 메모=저장된 date, 캘린더 진입=initialDate, 그 외=오늘) 도입 → `addNewTask`가 이 날짜를 dueDate(allDay)로 상속, 새 메모 `persist()`도 이 날짜를 항상 주입. 설계원칙 5("날짜 위에 메모가 얹힌다") 정합. 날짜 탭 → 하단(1단)/우측(2단) 상세(DayDetail)에 즉시 표출됨(이 표시 UI 자체는 원래 있었고 데이터가 안 잡힌 것).
- ✨ **② 구글 캘린더 무음 재로그인** — 토큰이 메모리에만 있어 재시작마다 로그인 필요했음. `AndroidCalendarSyncManager`에 **silent authorize**(`Identity.getAuthorizationClient(...).authorize()`, 동의 불필요 시 토큰 즉시 반환) 추가: `syncNow()`가 토큰 없으면 무음 확보, **401(만료) 시에도 무음 갱신 후 1회 재시도**. 명시적 로그아웃(`signOut`) 시엔 `signedOut` 플래그로 무음 재인가 차단(로그아웃 존중). 생성자에 `context` 추가(Koin.android 배선). ※ Supabase 쪽은 원래 SecureStore 세션 영속+refresh 갱신이 있어 문제 없음.
- ✨ **③ 저장/추가 피드백** — 에디터 저장 버튼 → **"저장되었습니다 ✓"** 스낵바(빈 메모면 "저장할 내용이 없어요."), 에디터·캘린더 상세의 할 일 추가 → **"할 일이 추가되었습니다 ✓"**(빈 입력 안내 포함). `NoteEditorViewModel.save(onSaved)` 콜백화, 캘린더 Scaffold에 SnackbarHost 신설.
- ✨ **④ 당겨서 새로고침 → 동기화** — 캘린더 홈을 `PullToRefreshBox`(M3 공식, commonMain)로 감쌈: 당기면 **클라우드(Supabase) syncNow + (토글 ON이면) 구글 캘린더 push** 실행, 결과를 스낵바로("동기화 완료 ✓"/실패/꺼짐 안내). 1단·2단 배치 모두 적용.
- ✨ **⑤ 비슷한 메모 실시간 추천** — 에디터 입력(제목+본문)을 450ms 디바운스 → **로컬 FTS OR 매칭**(`toFtsMatchAny`: 토큰화→빈도·길이 상위 8개→`키워드* OR ...`)으로 **다른 날의 유사 메모 최대 5건**을 "비슷한 메모" 섹션에 표시, 탭하면 그 메모로 이동. 자기 자신·같은 날짜 제외. 오프라인 우선·API 비용 0(OpenAI 임베딩 없이 즉시 동작 — 필요 시 추후 업그레이드 여지). 자산: `NoteRepository.searchRelated` + `FindRelatedNotesUseCase` + 에디터 `relatedNotes` StateFlow + `RelatedNoteRow`. 테스트 +2(`RelatedNotesTest`: OR 매칭 정제·자기/같은날 제외).
- **변경 파일**: `ui/notes/{NoteEditorScreen,NoteEditorViewModel}.kt` · `ui/calendar/CalendarScreen.kt` · `ui/navigation/DayNoteNavHost.kt`(에디터 onOpenNote 배선) · `data/repository/{NoteRepository,NoteRepositoryImpl}.kt` · `domain/usecase/NoteUseCases.kt` · `di/{Koin,Koin.android}.kt` · `androidMain/data/sync/AndroidCalendarSyncManager.kt` · `desktopTest/.../RelatedNotesTest.kt`(신규).
- ⚠️ **기기 검증 권장**: ① 메모 탭에서 새 메모+할 일 작성 → 캘린더 오늘 칸/상세에 표시 확인 ② 앱 재시작 후 로그인 없이 "지금 동기화"/당겨서 새로고침 동작 확인(최초 1회 동의는 여전히 필요) ③ 폰에서 당겨서 새로고침 제스처 ④ 에디터에서 몇 단어 입력 시 "비슷한 메모" 노출.

**막힘 없음. 기다리는 것 = Play 개발자 계정 승인뿐.** 코드/자산은 제출 준비 끝.

### 🎨 캘린더 재설계 「Warm Journal」(방향 B) 적용 ✅ (2026-07-01)
- **결정**: 두 제안(A 아틀라스 도판 / B 따뜻한 다이어리) 중 **방향 B 채택** — 부드러운 라운드 타일 + 밀도 점 + 제목 텍스트 유지. (사용자 확인: "제목 미리보기는 유지" → 점+글자 병행안으로 착수.)
- **월 달력(`DayCell`)**: 라운드 13dp 웜 타일(주말=secondaryContainer 틴트, 이번달 밖=투명). 상단에 **날짜 + 밀도 점**(중요=tertiary·할일=primary·메모=빈 원, 최대 4), 그 아래 **메모 제목 최대 2줄**(핀=SemiBold onSurface) + "+N개 더". **오늘=슬레이트 원**(primary 배경), 선택=primaryContainer+primary 테두리, 일요일 숫자=tertiary.
- **주 아젠다(`WeekAgenda`)**: 같은 웜 타일·오늘 원·주말 틴트·일요일 클레이. 제목 3줄 유지 + "할 일 N개" + 우측 밀도 점.
- **헤더**: 세리프 월 제목(`FontFamily.Serif`) + **클레이 스와시 밑줄**, 이모지 ◀▶ → 얇은 `‹ 오늘 ›`.
- **데이터**: 점의 "할일"을 위해 `CalendarViewModel.tasksByDate`(visibleRange 기반) 추가 → `MonthGrid`/`WeekAgenda`로 전달. 새 의존성 0, **원생 hex 0**(전부 `MaterialTheme.colorScheme` 롤), 라이트/다크 자동.
- **자산**: `ui/calendar/{CalendarScreen,CalendarViewModel}.kt`. 신규 private 컴포저블 `DayNumber`/`DensityDots`/`Dot`(+`DotKind`). **assembleDebug + compileKotlinDesktop + desktopTest(22건) 모두 BUILD SUCCESSFUL.**
- ⚠️ **기기 시각 검증 권장**(CLI는 컴파일까지만): 폴드/탭에서 웜 타일·오늘 원·점+제목·다크모드 색 실제 확인.

### 🎨 상세·에디터 톤 확장 + 앱 아이콘 ✅ (2026-07-01)
- **상세(`DayDetail`)**: 날짜 헤더 **세리프 + 클레이 스와시**(캘린더 헤더와 통일). 섹션 라벨 `MEMO`/`TO-DO`를 **자간 넓힌 클리니컬 캡션**(`SectionLabel`, letterSpacing 2sp). 메모는 **웜 라운드 타일**(`NoteDetailRow`, 핀=굵게). 삭제 `TextButton("삭제")` → **절제된 ✕**(`DeleteX`, onSurfaceVariant).
- **공유 `TaskRow`**: 삭제 "삭제" → **✕**(상세·에디터·To-Do 탭 전부 반영). 체크박스는 기존 primary(슬레이트).
- **에디터(`NoteEditorScreen`)**: 상단바 제목 **세리프**, "할 일" → `TO-DO` 캡션 톤.
- **🖼 앱 실행 아이콘 「리본 저널」(컨셉 A) 적용**: 그동안 매니페스트에 `android:icon` 부재 → **기본 안드로이드 아이콘**이 떴음. **Android 적응형 아이콘** 신설(벡터, **새 의존성 0**): `res/drawable/ic_launcher_foreground.xml`(노트 페이지+슬레이트 선+클레이 북마크), `ic_launcher_monochrome.xml`(테마 아이콘), `mipmap-anydpi-v26/ic_launcher(.round).xml`, `values/colors.xml`(배경=웜 본 #F0ECE3). 매니페스트에 `android:icon`/`roundIcon` 추가. minSdk 26이라 적응형만으로 충분(레거시 PNG 불필요). **데스크톱**: 공용 `ui/theme/DayNoteLogo.kt`(ImageVector, 같은 형상) → `main.kt` `Window(icon=…)` 창/작업표시줄 아이콘.
- **자산**: `ui/calendar/CalendarScreen.kt`·`ui/components/TaskRow.kt`·`ui/notes/NoteEditorScreen.kt`·`ui/theme/DayNoteLogo.kt`·`androidMain/res/**`·`androidMain/AndroidManifest.xml`·`desktopMain/.../main.kt`. **assembleDebug + compileKotlinDesktop + desktopTest(22건) 모두 BUILD SUCCESSFUL.**
- ⚠️ **후속/미완**: (1) 데스크톱 **.exe 파일 아이콘**은 `nativeDistributions.windows.iconFile`(.ico) 별도 필요 — 창 아이콘만 적용됨. (2) 기기에서 런처 아이콘·다크 테마 아이콘 실제 확인 권장.

### 🐞 기기 피드백 — 하단바 보라색 + 설정 아이콘 (2026-07-01)
- **하단바가 라벤더로 튐(근본 원인)**: `lightColorScheme`에 **톤 서피스 계열(`surfaceContainer*`/`surfaceBright`/`surfaceDim`) 미지정** → M3 기본 라벤더가 새어 `NavigationBar` 기본 배경(=surfaceContainer)이 보라로 떴음. **웜 톤으로 전 롤 명시**(Color.kt/Theme.kt, 라이트+다크) → 근본 차단(메뉴·드롭다운 등 다른 곳의 보라 누수도 함께 방지). **추가로** `DayNoteBottomBar`를 **투명 컨테이너 + 옅은 상단 헤어라인**으로 바꿔 앱 바탕(Paper)에 녹임(튀지 않음). 선택 표시는 웜 secondaryContainer.
- **우측 상단 Settings 텍스트 → 톱니바퀴**: material-icons 의존성 없음 → **`SettingsGearIcon` ImageVector 직접 정의**(`ui/theme/AppIcons.kt`, PathParser로 표준 gear 경로, 새 의존성 0). `CalendarScreen` 상단바 `IconButton`.
- 자산: `ui/theme/{Color,Theme,AppIcons}.kt`·`ui/components/DayNoteBottomBar.kt`·`ui/calendar/CalendarScreen.kt`. **assembleDebug + compileKotlinDesktop + desktopTest(22건) 모두 BUILD SUCCESSFUL.** ✅ 기기 확인됨(하단바 웜톤·톱니 아이콘).

### 🧰 개인용 안정성 기능 (2026-07-01, 수석 검토 후 P0)
> 개인용 "매일 안 깨지고 데이터 안 날아감" 목적 검토 결과 P0 2건 구현.

- ✅ **A. 로컬 백업/복원**(내보내기·가져오기): 메모·할 일 전체를 **JSON 파일로 저장/복원**(클라우드 없이 데이터 안전망). `data/backup/{BackupModels,BackupManager}`(DAO 직접, format 필수 필드로 잘못된 파일 거부, id upsert 라 중복 없음), `NoteDao/TaskDao.getAllRaw`, `ui/settings/BackupIO` expect/actual(Android=SAF, Desktop=AWT FileDialog), 설정 "백업/복원" 섹션. 테스트 +2. 커밋 `8361db5`.
- ✅ **B. 할 일 마감 리마인더**(알림): **시간 지정 할 일**의 마감 시각에 알림. 종일 할 일은 알림 없음(v1). `notification/{TaskReminderScheduler(expect via platformModule),ReminderCoordinator}`(앱 시작+변경 디바운스로 재예약, 발화 시점 재검증), Android=`AlarmManager`+`ReminderReceiver`(완료·삭제 필터)+`BootReceiver`(재부팅 재예약), Desktop=no-op. `TaskDao.getTimedUpcoming`, 설정 "할 일 마감 알림" 토글(기본 켜짐). 매니페스트 권한 `POST_NOTIFICATIONS`/`USE_EXACT_ALARM`/`RECEIVE_BOOT_COMPLETED` + 리시버 2개, MainActivity 알림권한 요청(13+). 테스트 +1(getTimedUpcoming 의미). **assembleDebug + compileKotlinDesktop + desktopTest(25건) 모두 BUILD SUCCESSFUL.**
  - ⚠️ **기기 검증 권장**: 시간 지정 할 일 만들고 마감 시각에 알림 오는지 1회. ⚠️ **Play**: `USE_EXACT_ALARM`은 캘린더/알람 앱에 허용되나, 데이터 보안/권한 선언에서 "정시 알림(캘린더)" 사유 필요할 수 있음.
  - 후속(P1): 종일 할 일 기본시각 알림, 반복 일정, 홈 위젯, 구글 캘린더 pull/무음 재로그인.

- **이번 세션 요약(2026-07-01)**: 자동 동기화·테마/다크모드·「Quiet Cadence」 팔레트/디자인 자산·Phase 5-A(2단)·5-C1/2(S펜 필기 캔버스) 완료 후, **갤럭시탭·폴드7 기기 피드백 1차 수정** → **개발자 실기기 재검증 전부 정상 확인**.
- **기기 확인됨**: 테마 전환/유지 ✓, 색감 ✓, 수동 동기화 ✓, Phase 4(공유·요약/확장/교정) ✓, 구글캘린더 생성 ✓.
- **이번에 고친 것**(아래 "기기 피드백 1차 수정" 참조): 🐞월 달력 겹침(MonthGrid Column 누락) · ✨스와이프 슬라이드 · ✨빈날짜 탭→메모 · 🐞에디터 커서(focusProperties) · ✨AI 자유질문 인라인. **모두 `88ba0a7`에 커밋됨.**
- ✅ **재검증 전부 통과(2026-07-01, 개발자 실기기)**: ① 월 달력 7열 그리드(폴드 단일/탭 2단) ✓ ② 에디터 커서 위/아래 ✓ ③ AI 자유질문 인라인 ✓ ④ 폰↔PC 자동 동기화 양방향 ✓ ⑤ S펜 필압/뒤집기 ✓. → **더 이상 대기 항목 없음.**

### 🎨 방향 A「Quiet Cadence 도판」 UI 재설계안 (제안됨, **미채택** — 캘린더는 방향 B 선택. 참고 보관)
> 정밀·클리니컬 대안. `Type.kt` 타이포 스펙은 방향과 무관하게 유효 → 나중에 적용 여지. 넓은 PC 월 달력에 부분 채용도 고려 가능.
> 포스터/철학(`design/quiet-cadence*`)은 완성됐으나 **(재설계 전) `CalendarScreen.kt`는 평범한 Material3**(라운드 8dp 셀·`primaryContainer` 채움 선택·이모지 화살표). 포스터의 "과학 아틀라스 도판" 언어를 앱으로 번역하는 게 가장 임팩트 큰 작업. **새 의존성 0, `MaterialTheme.colorScheme` 롤만 사용(원생 hex 금지).**
- **핵심 이동**: 라운드 셀→**헤어라인 사각 격자**(FaintLine 1px) · 선택=채움→**슬레이트 1px 아웃라인**(+5% 틴트) · 오늘=볼드숫자→**슬레이트 원 마커** · 메모 미리보기 텍스트→**클레이 엔트리 tick-line**(탭 시 제목 노출) · **열01–07/행01–06 좌표 눈금** + 요일(일요일만 클레이) · 헤더=**세리프 "7월" + 모노 캡션 + 코너 등록 눈금** · 하단 **범례(— ENTRY ○ TODAY · VACANT)**.
- **2번째 지렛대 — 타이포**: `Type.kt`가 **아직 placeholder**. 디스플레이=세리프(월제목·날짜헤더), 클리니컬 라벨=모노 `letterSpacing 0.18~0.22em`(요일·좌표·범례·시각), 본문=현 sans+여유 lineHeight. **이게 아틀라스 질감의 절반.**
- **확장**: DayDetail(삭제 텍스트→아이콘·헤어라인 구분), 에디터 상단바 규칙선화·편집/미리보기 모노 세그먼트, 다크는 격자=DarkOutlineVariant·엔트리선=DarkTertiary(#D98366, 팔레트에 이미 있음).
- **변경 파일(예상)**: `ui/theme/Type.kt` + `ui/calendar/CalendarScreen.kt`(DayCell/MonthGrid/CalendarHeader) 중심.
- **착수 시**: 라이트·다크 양쪽 + assembleDebug/desktopTest 양쪽 빌드 확인.

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
6. ✅ **AI 결과 이력 UI 완료(2026-07-01)**: 에디터 AI 패널 하단에 **"지난 AI 결과 N" 접이식 목록** 추가 — 메모별 과거 결과(요약·확장·교정·질문)를 최신순 표시. 각 항목: 동작 라벨 + "M월 D일 HH:mm" + 결과 텍스트(질문은 `Q. …` 함께), **"메모에 반영"(재사용) · "삭제"**. Room 이 진실의 원천이라 오프라인·재시작 유지, 새 결과·삭제 자동 반영. 배선: `AiResultDao.deleteById`(행 삭제뿐, **마이그레이션 0**) → `AiRepository.deleteResult` → `DeleteAiResultUseCase`(Koin 등록) → `AiViewModel.history`(noteId 없으면 빈 목록)·`deleteHistory`. `ObserveAiResultsUseCase`가 드디어 사용됨. `DateUtils.toShortDateTimeLabel` 추가. 테스트 +1(deleteResult 격리). 신규 의존성 0.
   - ⚠️ **빌드 검증 못 함(이 클라우드 컨테이너 한정)**: egress 정책이 `dl.google.com`(Android Gradle Plugin) 차단 → Gradle 태스크 실행 불가. **개발자 환경에서 `assembleDebug`+`desktopTest` 1회 확인 필요.** 코드 리뷰로는 정합성 확인됨.
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

### 4-C AI 제목 자동생성 ✅ (2026-07-01)
- **동작**: 메모 본문을 근거로 짧은 제목(18자 내외)을 OpenAI 로 생성. **트리거 2가지**: ① 에디터 제목칸 옆 **✨ 제목 버튼**(본문 있을 때 활성, 토글 무관 항상 사용 가능) ② **설정 "제목 자동생성" 토글**(기본 꺼짐) — 켜면 제목 비운 채 저장 시 자동 생성.
- **오프라인 폴백**: 키 없음·호출 실패·오프라인이면 **본문 첫 줄**(마크다운 앞머리 기호 `#·-·*·>` 제거, 40자 컷)을 제목으로. → 오프라인에서도 버튼이 쓸모 있음(설계원칙 2).
- **설계원칙 4 준수**: 기존 AI 인프라 재사용, **새 의존성 0 · DB 스키마 변경 0**. `AiRepository.suggestTitle(text): Result<String>` 추가 — 응답을 한 줄로 정제(따옴표/마침표/줄바꿈 제거)하고 **`ai_results`에 저장 안 함**(제목은 필드 채우기지 이력 결과가 아님 → DB 오염 0). `AiAction.TITLE`(칩 제외), `SuggestTitleUseCase`, `NoteEditorViewModel.suggestTitleNow()`/`maybeAutoTitle()`(저장 시), `SettingsRepository.observeAutoTitle/setAutoTitle`(settings 테이블 키 `auto_title`, 마이그레이션 0).
- **자산**: `domain/model/Ai.kt`·`data/repository/AiRepository.kt`·`domain/usecase/AiUseCases.kt`·`data/repository/SettingsRepository.kt`·`di/Koin.kt`·`ui/notes/{NoteEditorViewModel,NoteEditorScreen}.kt`·`ui/settings/{SettingsViewModel,SettingsScreen}.kt`. 테스트 `AiRepositoryTest` +2건(무-키·빈내용 안전 실패). **assembleDebug + desktopTest(22건) 양쪽 BUILD SUCCESSFUL.**
- ⚠️ **기기 검증 권장**(CLI 불가): 키 입력 후 실제 제목 생성 품질 1회 확인 + 키 없이 ✨ 눌러 본문 첫 줄 폴백 확인.

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

## 배포 설정 (2026-07-01)
> "다른 PC/안드로이드 배포" 요청으로 준비. 코드 3종 배선 완료(빌드 검증됨). 산출물은 커밋 제외.

### 안드로이드 release 서명 배선 ✅
- `build.gradle.kts`: 루트 `keystore.properties`(=git 제외)에서 읽는 `signingConfigs.release` + release 빌드타입에 적용. **파일 없으면 미서명**(디버그·CI 안전) — `assembleDebug` 통과 확인.
- `keystore.properties.example`(템플릿, keytool 생성·SHA-1 확인 명령 포함), `.gitignore`에 `*.jks`·`keystore.properties` 추가.
- **개발자 할 일**: ① `keytool -genkeypair`로 키스토어 생성(분실 금지) → `keystore.properties` 작성 → `:app:bundleRelease`(AAB)/`:app:assembleRelease`(APK). ② ⚠️ **릴리스 SHA-1 + Play 앱서명 SHA-1을 Google Cloud OAuth 클라이언트에 등록**(안 하면 캘린더 로그인 깨짐), OAuth 동의화면 테스트→프로덕션.

### 3-OS 자동 빌드 CI ✅
- `.github/workflows/build.yml`: `workflow_dispatch`/`v*` 태그 → **desktop 매트릭스(win/mac/linux) `createDistributable`**(app-image, WiX 등 외부도구 불필요) + **android `bundleRelease`(AAB)**. temurin 21(jpackage 포함). 안드로이드 서명은 저장소 Secrets(`KEYSTORE_BASE64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`) 있으면 자동 적용.

### 데스크톱 배포 요약
- **Windows 다른 PC**: `createDistributable` 산출 `app\build\compose\binaries\main\app\DayNote\` **폴더째 복사** → `DayNote.exe`(자바 번들, ~133MB). 또는 `:app:packageMsi`로 .msi. ⚠️ 미서명이라 SmartScreen "알 수 없는 게시자"("추가 정보→실행"). 코드서명 인증서(유료)로 제거 가능.
- **Mac/Linux**: jpackage 크로스빌드 불가 → 그 OS에서 `:app:packageDmg`/`:app:packageDeb`(또는 위 CI).

## 데스크톱 실행파일(.exe) 빌드 — 메모
- **빌드/실행 분리**: `:app:run`·`assembleDebug`·`desktopTest`는 JBR로 OK. 하지만 **배포물(`createDistributable`/`packageMsi`)은 `jpackage` 필요** → JBR엔 없음.
- 이 PC엔 **Eclipse Adoptium JDK 25**(`C:\Users\admin\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.3.9-hotspot`) 설치됨. Gradle 8.11.1은 JDK 25 전체 실행이 불안정하므로 **빌드는 JBR, jpackage만 JDK 25**로 분리.
- `build.gradle.kts`의 `compose.desktop.application.javaHome` ← `-Pdaynote.jpackage.jdk` 속성으로 받음(경로 하드코딩 안 함).
- 명령: `$env:JAVA_HOME=JBR; .\gradlew.bat :app:createDistributable "-Pdaynote.jpackage.jdk=<JDK25경로>"` → 산출물 `app\build\compose\binaries\main\app\DayNote\DayNote.exe`(번들 JRE, 129MB).
- ⚠️ 재패키징 전 **실행 중인 DayNote.exe 종료** 필요(폴더 잠금).

## 재개 명령 예시 (다음 세션에서 이걸로 시작)

```
PROGRESS.md 읽고 이어서 진행해줘.
현재: 기능·디자인·배포자산 전부 준비 완료. 작업트리 clean, main 최신(b3ebb1e) 푸시됨.
기다리는 것 = Play 개발자 계정 승인뿐. 승인 나면 아래 "Play 업로드 체크리스트"부터.
빌드: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  APK   : .\gradlew.bat :app:assembleDebug        (산출물 app\build\outputs\apk\debug\app-debug.apk)
  AAB   : .\gradlew.bat :app:bundleRelease         (서명됨 — keystore.properties 있음, app-release.aab)
  테스트 : .\gradlew.bat :app:desktopTest            (현재 25건)
  데스크톱 GUI: :app:run  /  .exe·msi: createDistributable·packageMsi "-Pdaynote.jpackage.jdk=<JDK25>"
```

### ⏳ Play 업로드 체크리스트 (개발자 계정 승인 후 — 순서대로)
1. **서명 AAB 빌드**: `:app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
2. Play Console에서 앱 생성 → **AAB 업로드**(내부 테스트 트랙 권장).
3. ⚠️ **Play 앱서명 SHA-1 등록**: 업로드 후 Play Console "앱 무결성 → 앱 서명"의 SHA-1을, Google Cloud Console OAuth에 **Android 클라이언트 하나 더**(패키지 `com.kangtaeyoung.daynote` + 그 SHA-1)로 추가. (릴리스 키스토어 SHA-1 `7C:D0:FC:…:E9:4A`는 이미 등록됨)
4. **OAuth 동의화면 테스트→프로덕션** 게시.
5. **앱 콘텐츠 입력**: 개인정보처리방침 URL(`https://labor7079-debug.github.io/daynote/privacy-policy.html`) · 데이터 보안 양식([docs/play-data-safety.md] 참고) · 앱 액세스=심사원 안내([docs/play-reviewer-notes.md]) · 콘텐츠 등급.
6. **스토어 등록정보**: 문구([docs/play-store-listing.md]) · 아이콘 [docs/play-icon-512.png] · 피처그래픽 [docs/play-feature-1024x500.png] · **폰 스크린샷 최소 2장(캡처 완료됨)**.
7. 검토용 출시 → 심사.

### 상태/자산 요약 (승인 대기 중 참고)
- **릴리스 서명**: `daynote-release.jks`(루트, gitignore) + `keystore.properties`(비번은 대화 이력·기기 보관). 분실 금지.
- **릴리스 SHA-1**: `7C:D0:FC:10:5A:99:74:41:C5:1C:4D:A1:12:3B:99:7D:0A:B4:E9:4A` — OAuth 등록 완료.
- **버전**: versionCode 1 / versionName 0.1.0. 다음 업로드마다 versionCode 증가.
- **기기 확인됨**: 하단바 웜톤·톱니 아이콘·Warm Journal 재설계. **미확인(새 APK 권장)**: 리마인더 실제 알림, 백업/복원 왕복.
- **데스크톱 배포물**: DayNote.exe(폴더형) + DayNote-1.0.0.msi 생성됨(`app\build\compose\binaries\main\`).

### 남은 개선(P1, 급하지 않음 — 출시 후 업데이트로)
- 종일 할 일 기본시각 알림 · 반복 일정/할 일 · 홈 화면 위젯 · 구글 캘린더 pull(양방향)·무음 재로그인 · 앱 잠금(생체인증).

### 확정된 우선순위 (이전 세션)
- **AI 검색/자료수집**: 드롭 — 기존 FTS 메모검색으로 충분.

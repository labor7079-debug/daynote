# 빌드·실행 안내 (Phase 0)

이 PC에는 JDK·Gradle·Android SDK가 없어 Claude Code가 **프로젝트 골격만** 생성했습니다.
실제 빌드·기기 실행은 개발자가 진행합니다(명세서 0장 협업 모델).

## 1. 사전 준비 (한 번만)
- **Android Studio 최신 안정판** 설치 (JDK 17이 함께 번들됨).
- Android Studio → SDK Manager에서 **Android SDK Platform 35** + 빌드 도구 설치.

## 2. 프로젝트 열기
1. Android Studio → **Open** → 이 폴더(`AI_note`) 선택.
2. 첫 실행 시 Android Studio가 **Gradle 래퍼(gradlew, gradle-wrapper.jar)를 자동 생성**하고
   `gradle-wrapper.properties`에 지정된 Gradle 8.11.1을 내려받습니다.
3. **Gradle Sync**가 끝나면 의존성(Compose·Hilt·Room 등)이 모두 받아집니다.

## 3. 빌드 확인 (Phase 0 완료 기준)
- Android Studio 터미널에서:
  ```
  ./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
  ```
  → `BUILD SUCCESSFUL` 이면 Phase 0의 빌드 기준 충족.
- **실기기 실행**: 갤럭시 탭/폴드를 USB로 연결(개발자 옵션 → USB 디버깅) →
  Android Studio ▶ Run → "DayNote" 빈 화면에 *"Phase 0 — 빈 화면이 빌드되었습니다"* 표시되면 완료.

## 참고
- 패키지/applicationId: `com.kangtaeyoung.daynote`
- minSdk 26 / compileSdk·targetSdk 35
- 빈 패키지에는 `.gitkeep`만 있고 Phase 1부터 채웁니다.
- Sync 중 버전 경고가 나오면 Android Studio가 최신 안정 버전을 제안합니다 — 그대로 반영해도 됩니다.

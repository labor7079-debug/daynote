# Play 심사원용 안내 (App access / Instructions)

> Play Console → **앱 콘텐츠(App content) → 앱 액세스(App access)**. 로그인 없이 핵심 기능이 전부 동작하므로, 심사원이 별도 계정 없이 앱을 평가할 수 있습니다. 아래를 그대로 붙여넣으면 됩니다.

## 앱 액세스 선택
- **"모든 기능을 특별한 액세스 없이 사용할 수 있음"이 아니라 "일부 기능이 제한됨"**을 선택하고, 아래 안내를 첨부하는 것을 권장합니다. (핵심 기능은 열려 있지만, 선택적 동기화·AI는 사용자 본인 계정/키가 필요해 테스트 계정을 제공할 수 없기 때문입니다.)

---

## 안내문 (한국어)
```
이 앱은 로그인 없이 핵심 기능(캘린더, 메모, 할 일)이 모두 동작합니다.
설치 후 별도 계정이나 로그인 없이 즉시 사용·평가할 수 있습니다.
데이터는 기본적으로 기기 내부에 저장되는 오프라인 우선 앱입니다.

아래 기능은 '선택 사항'이며 기본적으로 꺼져 있습니다. 각각 사용자 본인의
계정/키가 필요하므로 테스트 계정을 제공하지 않으며, 앱 평가에 필수가 아닙니다:
- 구글 캘린더 동기화: 사용자 본인의 Google 계정 로그인 필요
- AI 기능: 사용자 본인의 OpenAI API 키 입력 필요
- 클라우드 동기화: 사용자 본인의 Supabase 프로젝트/계정 필요

위 선택 기능은 모두 앱 내 '설정' 화면에서 켜고, 사용자가 직접 정보를 입력한
경우에만 외부로 데이터가 전송됩니다. (자세한 내용은 개인정보처리방침 참고)
```

## Notes for reviewers (English)
```
All core features (Calendar, Memo, To-Do) work without any sign-in.
The app is fully usable and testable immediately after install, with no
account required. It is offline-first: data is stored locally on the device
by default.

The following features are OPTIONAL and OFF by default. Each requires the
user's OWN account/key, so no test account is provided and they are not
required to evaluate the app:
- Google Calendar sync: requires the user's own Google account
- AI features: require the user's own OpenAI API key
- Cloud sync: require the user's own Supabase project/account

These optional features are enabled from the in-app Settings screen, and data
is sent to third parties only when the user turns them on and enters their own
credentials. See the privacy policy for details.
```

---

## 참고 (제출 시 함께 확인)
- **로그인 필요 없음**을 명확히 적어두면 "기능을 확인할 수 없음"으로 인한 반려를 예방할 수 있습니다.
- 구글 캘린더 로그인을 심사원이 시도할 가능성에 대비해, **OAuth 동의화면을 '프로덕션'으로 게시**하고 릴리스/Play 앱서명 SHA-1을 등록해 두면 로그인 시도도 정상 동작합니다.

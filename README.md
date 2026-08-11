# pptnzblog

페퍼톤스(PPTNZ) 공식 블로그(peppertones.net) 새 글을
감지해서 알려주고, iOS/Android 앱 + 위젯으로 모아 볼 수 있게 하는 개인 프로젝트.

## 구성

- **크롤러** (`crawl_pptnz_blog.py`, `.github/workflows/crawl.yml`)
  GitHub Actions가 주기적으로 블로그 목록을 파싱해 `posts.json`으로 커밋합니다.
  목록/새 글 감지용 가벼운 데이터(id·제목·날짜·본문 미리보기·링크)만 담당하고,
  이미지·댓글·영상 같은 리치 콘텐츠와 알림은 각 앱이 직접 처리합니다.

- **iOS 앱 + 위젯** (`ios/`) — SwiftUI + WidgetKit. 자세한 기능은 아래 참고.
- **Android 앱 + 위젯** (`android/`) — Jetpack Compose + Glance. iOS와 기능 1:1 대응.

두 앱 모두 같은 `posts.json`을 데이터 소스로 쓰고, 글 상세는 각자 원문 페이지를 실시간으로
파싱해서 보여줍니다(서버/백엔드 없음).

## 앱 기능 (iOS / Android 공통)

- **글 목록**: 연도별로 묶어서 보여주고, 연도 헤더를 탭하면 접고 펼칠 수 있습니다.
  제목/본문 키워드로 검색하고, 화면 우하단 버튼으로 맨 위/맨 아래로 스크롤합니다.
- **글 상세**: 진입 시 원문 페이지를 실시간으로 가져와 파싱합니다
  (iOS: `PostDetailLoader` + SwiftSoup / Android: `PostDetailLoader` + Jsoup).
  - 본문 텍스트 + 이미지를 원문 순서 그대로 보여주고, GIF는 애니메이션으로 재생합니다.
  - 옛 글의 구형 Flash `<object>/<embed>` 유튜브 임베드까지 인식해서 인앱(WKWebView / WebView)으로
    재생하고, 재생이 안 되는 경우를 대비해 "유튜브에서 보기" 링크도 함께 보여줍니다.
  - 댓글과 첨부/참고 링크도 함께 파싱해서 보여줍니다.
  - 오프라인이거나 파싱에 실패하면 `posts.json`에 저장된 요약 텍스트로 자동 폴백합니다.
- **북마크**: 글 상세에서 북마크 토글, 별도 화면에서 북마크 목록을 모아 봅니다.
- **인스타그램 스토리 공유**: 글 상세에서 공유하면 제목/본문 미리보기/작성일을 담은 카드 이미지를
  생성해 인스타그램 스토리로 바로 올릴 수 있습니다(미설치 시 표준 공유 시트로 폴백).
- **알림**: 서버/외부 웹훅 없이 앱 자체 메커니즘만 사용합니다.
  - *새 글 알림*: 백그라운드에서 주기적으로 `posts.json`을 확인해서 새 글이 있으면
    로컬 알림(제목 "새로운 글이 올라왔어요!", 본문은 글 원문 미리보기)을 띄웁니다.
  - *매일 랜덤 글 알림*: 설정에서 켜고 끌 수 있고, 하루 최대 3개 시각을 등록할 수 있습니다.
    각 시각마다 랜덤으로 고른 글 1개를 제목=글 제목, 본문=글 원문 미리보기로 예약 발송합니다.
  - 알림을 탭하면 앱이 해당 글 상세로 바로 이동합니다.
- **위젯**: 홈 화면 위젯에서 랜덤 글 또는 북마크함 중 노출 방식을 고를 수 있고,
  위젯을 탭하면 해당 글 상세로 딥링크 이동합니다.
- **테마**: 페퍼톤스 공식 블로그 스킨(`pptnz8`) CSS에서 추출한 배경/텍스트/포인트 컬러를 그대로 사용합니다.
- 두 플랫폼 모두 세로 화면 전용입니다.

## 데이터 흐름

```
GitHub Actions (주기적)
  └─ crawl_pptnz_blog.py: 블로그 목록 HTML 파싱
       └─ posts.json 커밋 (repo 루트)

iOS / Android 앱 · 위젯
  ├─ raw.githubusercontent.com/.../posts.json 직접 fetch (목록/위젯)
  │    └─ 로컬 캐시에 저장 (iOS: App Group / Android: DataStore, 앱↔위젯 공유)
  ├─ 글 상세 진입 시 원문 페이지(blog/{id})를 실시간 fetch + 파싱
  │    (본문 텍스트/이미지/GIF/유튜브 임베드/댓글/첨부 링크)
  └─ 백그라운드 새로고침: posts.json과 마지막 확인 id 비교
       ├─ 새 글이면 로컬 알림
       └─ 매일 설정된 시각에 랜덤 글 알림 재예약
```

## iOS 빌드

Xcode 프로젝트는 [XcodeGen](https://github.com/yonaskolb/XcodeGen)으로 `project.yml`에서
생성합니다. 처음 여는 방법은 [ios/SETUP.md](ios/SETUP.md) 참고.

## Android 빌드

```bash
cd android
./gradlew assembleDebug    # 디버그 APK: app/build/outputs/apk/debug/app-debug.apk
```

릴리즈(서명된) APK를 만들려면:

1. keystore 생성: `keytool -genkeypair -v -keystore ~/pptnzblog-release.jks -alias pptnzblog -keyalg RSA -keysize 2048 -validity 10000`
2. `android/local.properties`에 아래 값 추가 (이 파일은 gitignore되어 커밋되지 않음):
   ```properties
   RELEASE_STORE_FILE=/절대/경로/pptnzblog-release.jks
   RELEASE_STORE_PASSWORD=...
   RELEASE_KEY_ALIAS=pptnzblog
   RELEASE_KEY_PASSWORD=...
   ```
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`

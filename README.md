# pptnzblog

페퍼톤스(PPTNZ) 공식 블로그(peppertones.net) 새 글을
감지해서 알려주고, iOS 앱/위젯으로 모아 볼 수 있게 하는 개인 프로젝트.

## 구성

- **크롤러** (`crawl_pptnz_blog.py`, `.github/workflows/crawl.yml`)
  GitHub Actions가 20분마다 블로그 목록을 파싱해 `posts.json`으로 커밋하고,
  새 글이 있으면 [Pushcut](https://pushcut.io) 웹훅으로 iOS 알림을 보냅니다.
  최초 1회, 리포지토리 Settings → Secrets and variables → Actions에
  `PUSHCUT_WEBHOOK_URL` secret 등록이 필요합니다.

- **iOS 앱 + 위젯** (`ios/`)
  `posts.json`을 `raw.githubusercontent.com`에서 직접 읽어와
  - 글 목록을 연도별로 묶어서 보여주고
  - 제목/본문 키워드 검색을 지원하고
  - 홈 화면 위젯으로 랜덤 글을 노출합니다.

  Xcode 프로젝트는 [XcodeGen](https://github.com/yonaskolb/XcodeGen)으로 `project.yml`에서
  생성합니다. 처음 여는 방법은 [ios/SETUP.md](ios/SETUP.md) 참고.

## 데이터 흐름

```
GitHub Actions (20분 주기)
  └─ crawl_pptnz_blog.py: 블로그 목록 HTML 파싱
       ├─ 새 글 있으면 Pushcut 웹훅 → iOS 알림
       └─ posts.json 커밋 (repo 루트)

iOS 앱 / 위젯
  └─ raw.githubusercontent.com/.../posts.json 직접 fetch
       └─ App Group 캐시에 저장 (앱↔위젯 공유)
```

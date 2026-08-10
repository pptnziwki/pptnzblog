# pptnzblog

페퍼톤스(PPTNZ) 공식 블로그(peppertones.net) 새 글을
감지해서 알려주고, iOS 앱/위젯으로 모아 볼 수 있게 하는 개인 프로젝트.

## 구성

- **크롤러** (`crawl_pptnz_blog.py`, `.github/workflows/crawl.yml`)
  GitHub Actions가 20분마다 블로그 목록을 파싱해 `posts.json`으로 커밋합니다.
  목록/새 글 감지용 가벼운 데이터만 담당하고, 알림은 앱이 직접 처리합니다.

- **iOS 앱 + 위젯** (`ios/`)
  `posts.json`을 `raw.githubusercontent.com`에서 직접 읽어와
  - 글 목록을 연도별로 묶어서 보여주고, 제목/본문 키워드로 검색하고
  - 글 상세를 열면 원문 페이지를 실시간으로 가져와 본문·이미지·댓글까지 보여주고
  - Background App Refresh로 주기적으로 새 글 여부를 확인해 로컬 알림을 띄우고
  - 홈 화면 위젯으로 랜덤 글을 노출합니다.

  Xcode 프로젝트는 [XcodeGen](https://github.com/yonaskolb/XcodeGen)으로 `project.yml`에서
  생성합니다. 처음 여는 방법은 [ios/SETUP.md](ios/SETUP.md) 참고.

## 데이터 흐름

```
GitHub Actions (20분 주기)
  └─ crawl_pptnz_blog.py: 블로그 목록 HTML 파싱
       └─ posts.json 커밋 (repo 루트)

iOS 앱 / 위젯
  ├─ raw.githubusercontent.com/.../posts.json 직접 fetch (목록/위젯)
  │    └─ App Group 캐시에 저장 (앱↔위젯 공유)
  ├─ 글 상세 진입 시 원문 페이지(blog/{id})를 실시간 fetch + 파싱 (본문/이미지/댓글)
  └─ Background App Refresh: posts.json과 마지막 확인 id 비교 → 새 글이면 로컬 알림
```

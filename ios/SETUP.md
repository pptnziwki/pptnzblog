# Mac에서 처음 여는 법

이 폴더(`ios/`)에는 `.xcodeproj`가 없습니다. [XcodeGen](https://github.com/yonaskolb/XcodeGen)이 `project.yml`을 읽어서
매번 새로 만들어주는 방식이라, `.xcodeproj`는 커밋하지 않습니다(`.gitignore` 처리됨).

## 1. XcodeGen 설치 (최초 1회)

```bash
brew install xcodegen
```

## 2. 프로젝트 생성 & 열기

```bash
cd pptnzblog/ios
xcodegen generate
open PPTNZBlog.xcodeproj
```

`project.yml`을 수정할 때마다 `xcodegen generate`를 다시 실행하면 `.xcodeproj`가 갱신됩니다.

## 3. Xcode에서 서명 설정

두 타겟(`PPTNZBlog`, `PPTNZBlogWidgetExtension`) 각각에 대해:

1. 타겟 선택 → **Signing & Capabilities** 탭
2. **Team**을 본인 Apple ID로 지정 (Automatically manage signing 체크)
3. **Bundle Identifier**가 `com.wooyxxng.pptnzblog` / `com.wooyxxng.pptnzblog.widget`인데, 이미 다른 사람이 쓰고 있다는 에러가 나면
   `ios/project.yml`의 `PRODUCT_BUNDLE_IDENTIFIER` 값을 본인 소유 prefix로 바꾸고 `xcodegen generate`를 다시 실행하세요.
4. **App Groups** capability가 두 타겟 모두 `group.com.wooyxxng.pptnzblog`로 잡혀 있는지 확인
   (entitlements 파일에 이미 들어있지만, Apple 개발자 계정에 실제로 등록되려면 Xcode가 Team을 알아야 합니다.
   Signing & Capabilities에 App Groups가 안 보이면 **+ Capability → App Groups**로 추가하고 체크하세요.)
   - 앱과 위젯 두 타겟의 App Group 값은 반드시 동일해야 합니다. 값을 바꾸려면
     `Sources/App/PPTNZBlog.entitlements`, `Sources/Widget/PPTNZBlogWidget.entitlements`,
     `Sources/Shared/PostsRepository.swift`의 `appGroupID`까지 세 군데를 같이 바꿔주세요.

## 4. 빌드 & 실행

- `PPTNZBlog` 스킴으로 시뮬레이터/실기기 실행 → 앱이 뜨면 posts.json을 내려받아 목록이 보입니다.
- 위젯은 홈 화면 길게 눌러 "위젯 추가"에서 "PPTNZ 블로그"를 찾아 추가하면 됩니다.
- 무료 Apple ID(유료 개발자 프로그램 미가입)로 실기기에 설치하면 서명이 7일마다 만료되어
  주기적으로 Xcode에서 다시 Run 해줘야 합니다.

## 참고

- `posts.json`은 GitHub Actions가 15~20분마다 갱신해서 리포지토리 루트에 커밋합니다.
  앱/위젯은 `https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/posts.json`을 직접 읽습니다.
- `Post.year`는 Textcube 날짜 텍스트에서 첫 4자리 숫자를 정규식으로 뽑아 연도로 씁니다.
  실제 `posts.json`을 받아본 뒤 날짜 형식이 예상과 다르면 `Sources/Shared/Post.swift`의 정규식만 손보면 됩니다.

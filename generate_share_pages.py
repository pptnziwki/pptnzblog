#!/usr/bin/env python3
"""
posts.json을 읽어서 GitHub Pages(docs/)에 올릴 글별 공유 페이지를 생성하는 스크립트.

각 페이지(docs/s/{id}.html)는:
- OG 메타 태그(og:title/og:description/og:image/og:url)를 담고 있어
  카카오톡/메시지 등에서 링크를 공유하면 썸네일 미리보기가 뜬다.
- 로드 즉시 커스텀 URL 스킴(pptnzblog://post/{id})으로 앱 실행을 시도하고,
  일정 시간 뒤에도 여전히 브라우저에 남아있으면(=앱이 없으면) 원문 블로그
  페이지로 리다이렉트한다.

이미 존재하는 페이지는 다시 만들지 않는다(매 크롤링마다 git diff가 커지는 것 방지).
GitHub Actions에서 크롤링 직후에 실행하는 걸 전제로 만들었다.
"""

import html
import json
import re
from pathlib import Path

POSTS_FILE = Path("posts.json")
DOCS_DIR = Path("docs")
SHARE_DIR = DOCS_DIR / "s"

# GitHub Pages 활성화 후 실제 호스트명으로 맞춰야 한다.
PAGES_BASE_URL = "https://pptnziwki.github.io/pptnzblog"
FALLBACK_IMAGE_URL = f"{PAGES_BASE_URL}/assets/og-fallback.png"

# 앱이 처리하는 커스텀 URL 스킴 (PPTNZBlogApp.swift의 .onOpenURL과 짝을 이룬다)
APP_SCHEME_TEMPLATE = "pptnzblog://post/{id}"

DESCRIPTION_MAX_LEN = 150


def make_description(content: str) -> str:
    """본문에서 개행을 제거하고 앞부분만 잘라 og:description으로 쓴다."""
    flattened = " ".join(content.split())
    if len(flattened) > DESCRIPTION_MAX_LEN:
        return flattened[:DESCRIPTION_MAX_LEN].rstrip() + "…"
    return flattened


def render_page(post: dict) -> str:
    post_id = post["id"]
    title = post.get("title") or "-"
    content = post.get("content") or ""
    link = post["link"]
    thumbnail = post.get("thumbnail") or FALLBACK_IMAGE_URL

    page_url = f"{PAGES_BASE_URL}/s/{post_id}.html"
    app_url = APP_SCHEME_TEMPLATE.format(id=post_id)
    description = make_description(content)

    esc_title = html.escape(title)
    esc_description = html.escape(description)

    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>{esc_title}</title>
<meta property="og:title" content="{esc_title}" />
<meta property="og:description" content="{esc_description}" />
<meta property="og:image" content="{html.escape(thumbnail)}" />
<meta property="og:url" content="{html.escape(page_url)}" />
<meta property="og:type" content="article" />
<meta name="twitter:card" content="summary_large_image" />
<meta name="twitter:title" content="{esc_title}" />
<meta name="twitter:description" content="{esc_description}" />
<meta name="twitter:image" content="{html.escape(thumbnail)}" />
</head>
<body>
<p>{esc_title}</p>
<p>{html.escape(post.get("date", ""))}</p>
<p>{esc_description}</p>
<p><a href="{html.escape(link)}">원문으로 이동</a></p>
<script>
(function () {{
  var appUrl = {json.dumps(app_url)};
  var fallbackUrl = {json.dumps(link)};
  var redirected = false;

  function goFallback() {{
    if (redirected || document.hidden) return;
    redirected = true;
    window.location.href = fallbackUrl;
  }}

  document.addEventListener("visibilitychange", function () {{
    if (document.hidden) redirected = true;
  }});

  window.location.href = appUrl;
  setTimeout(goFallback, 1200);
}})();
</script>
</body>
</html>
"""


def main() -> None:
    posts = json.loads(POSTS_FILE.read_text(encoding="utf-8"))
    SHARE_DIR.mkdir(parents=True, exist_ok=True)
    (DOCS_DIR / ".nojekyll").touch()

    created = 0
    for post in posts:
        out_path = SHARE_DIR / f"{post['id']}.html"
        if out_path.exists():
            continue
        out_path.write_text(render_page(post), encoding="utf-8")
        created += 1

    print(f"{created}개 공유 페이지 생성 완료 (총 {len(posts)}개 글)")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
PPTNZ(페퍼톤스) 블로그(Textcube) 목록 페이지를 직접 파싱해서
- 최근 글 목록을 posts.json 으로 저장하고
- 이전 실행 결과와 비교해 새 글이 있으면 Pushcut 웹훅으로 알림을 보내는 스크립트

RSS(/blog/rss)는 채널 정보만 있고 <item>이 비어있어서(의도적 설정으로 추정),
대신 목록 페이지(/blog) HTML을 직접 파싱합니다.
GitHub Actions에서 15~30분마다 실행하는 걸 전제로 만들었습니다.
posts.json은 리포지토리에 커밋되고, iOS 위젯은 이 파일을
raw.githubusercontent.com URL로 그대로 읽어가면 됩니다.
"""

import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

BASE_URL = "http://peppertones.host.whoisweb.net/blog"
POSTS_FILE = Path("posts.json")

# 신재평님 쪽에서 트래픽 봤을 때 뭔지 알 수 있도록 본인 식별 정보로 바꿔주세요
USER_AGENT = "PPTNZFanWidget/1.0 (contact: wooyxxng@gmail.com)"

# GitHub Actions repo secret으로 등록해서 사용 (코드에 직접 넣지 마세요)
PUSHCUT_WEBHOOK_URL = os.environ.get("PUSHCUT_WEBHOOK_URL")

ENTRY_ID_RE = re.compile(r"/blog/(\d+)$")


def fetch_page(page: int = 1) -> BeautifulSoup:
    """블로그 목록 페이지 하나를 가져와 파싱. 새 글 감지는 1페이지만으로 충분."""
    url = BASE_URL if page == 1 else f"{BASE_URL}?page={page}"
    resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=15)
    resp.raise_for_status()
    resp.encoding = "utf-8"
    return BeautifulSoup(resp.text, "html.parser")


def parse_entries(soup: BeautifulSoup) -> list[dict]:
    posts = []
    for entry in soup.select("div.entry"):
        title_link = entry.select_one("div.titleWrap h2 a")
        if not title_link or not title_link.get("href"):
            continue

        href = title_link["href"]
        m = ENTRY_ID_RE.search(href)
        if not m:
            continue
        post_id = m.group(1)

        date_spans = [s.get_text(strip=True) for s in entry.select("div.titleWrap span.date")]
        date_text = next((d for d in date_spans if d), "")

        content_el = entry.select_one("div.article")
        content = content_el.get_text("\n", strip=True) if content_el else ""

        raw_title = title_link.get_text(strip=True)
        # 제목 없는 글이 많아서("-"), 본문 앞부분을 대체 제목으로 사용
        display_title = raw_title if raw_title and raw_title != "-" else (content[:40] or "(제목 없음)")

        posts.append(
            {
                "id": post_id,
                "title": display_title,
                "link": urljoin(BASE_URL + "/", post_id),
                "date": date_text,
                "content": content,
            }
        )
    return posts


def load_previous() -> list[dict]:
    if POSTS_FILE.exists():
        return json.loads(POSTS_FILE.read_text(encoding="utf-8"))
    return []


def save_current(posts: list[dict]) -> None:
    POSTS_FILE.write_text(json.dumps(posts, ensure_ascii=False, indent=2), encoding="utf-8")


def notify_new_posts(new_posts: list[dict]) -> None:
    if not PUSHCUT_WEBHOOK_URL:
        print("PUSHCUT_WEBHOOK_URL이 설정되지 않아 알림을 건너뜁니다.")
        return
    for post in new_posts:
        try:
            requests.post(
                PUSHCUT_WEBHOOK_URL,
                json={"title": "PPTNZ 블로그 새 글", "text": post["title"], "input": post["link"]},
                timeout=10,
            )
        except requests.RequestException as e:
            print(f"Pushcut 알림 실패: {e}", file=sys.stderr)


def main() -> None:
    soup = fetch_page(page=1)
    current_posts = parse_entries(soup)
    previous_posts = load_previous()
    previous_ids = {p["id"] for p in previous_posts}

    new_posts = [p for p in current_posts if p["id"] not in previous_ids]

    if new_posts:
        print(f"새 글 {len(new_posts)}건 발견")
        notify_new_posts(new_posts)
    else:
        print("새 글 없음")

    # 이전 목록 + 이번에 새로 본 글 병합, 최신 200개만 유지 (위젯 랜덤 노출용으로 충분)
    merged = {p["id"]: p for p in previous_posts}
    merged.update({p["id"]: p for p in current_posts})
    all_posts = sorted(merged.values(), key=lambda p: int(p["id"]), reverse=True)[:200]

    save_current(all_posts)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
PPTNZ(페퍼톤스) 블로그(Textcube) 목록 페이지를 직접 파싱해서
최근 글 목록을 posts.json 으로 저장하는 스크립트.

RSS(/blog/rss)는 채널 정보만 있고 <item>이 비어있어서(의도적 설정으로 추정),
대신 목록 페이지(/blog) HTML을 직접 파싱합니다.
GitHub Actions에서 15~30분마다 실행하는 걸 전제로 만들었습니다.
posts.json은 리포지토리에 커밋되고, iOS 앱/위젯은 이 파일을
raw.githubusercontent.com URL로 그대로 읽어갑니다.

새 글 알림은 (Pushcut 등 외부 웹훅 대신) iOS 앱이 Background App Refresh로
posts.json을 주기적으로 확인해 로컬 알림을 띄우는 방식으로 처리합니다.
"""

import json
import re
from pathlib import Path
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

BASE_URL = "http://peppertones.host.whoisweb.net/blog"
POSTS_FILE = Path("posts.json")

# 신재평님 쪽에서 트래픽 봤을 때 뭔지 알 수 있도록 본인 식별 정보로 바꿔주세요
USER_AGENT = "PPTNZFanWidget/1.0 (contact: wooyxxng@gmail.com)"

ENTRY_ID_RE = re.compile(r"/blog/(\d+)$")


MAX_PAGES = 500  # 안전장치. 이 블로그는 page당 글 1개라 전체 글 수만큼만 있으면 충분


def fetch_page(page: int = 1) -> BeautifulSoup:
    """블로그 목록 페이지 하나를 가져와 파싱. 이 블로그는 page 파라미터당 글이 1개씩 나온다."""
    url = BASE_URL if page == 1 else f"{BASE_URL}?page={page}"
    resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=15)
    resp.raise_for_status()
    resp.encoding = "utf-8"
    return BeautifulSoup(resp.text, "html.parser")


def extract_content(content_el) -> str:
    """본문 공백/줄바꿈을 원문 그대로 살려서 텍스트로 추출.

    이 블로그는 한 줄이 <div>(또는 <font> 안에 <br/>만 있는 빈 줄) 하나씩으로
    되어 있는 글이 많다. 최상위 자식 각각의 텍스트를 그대로 모아 "\n"으로
    이어붙이면, 빈 줄(div)은 빈 문자열을 하나 기여해서 원문의 줄바꿈/공백
    줄이 그대로 재현된다. (get_text(strip=True) 등을 쓰면 빈 줄이 통째로
    사라져서 문단 구분이 뭉개진다.)
    """
    if content_el is None:
        return ""
    lines = [
        child.get_text() if hasattr(child, "get_text") else str(child)
        for child in content_el.children
    ]
    text = "\n".join(lines).replace("\xa0", " ")
    text = "\n".join(line.rstrip() for line in text.split("\n"))
    return text.strip("\n")


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
        content = extract_content(content_el)

        thumbnail = None
        if content_el is not None:
            img = content_el.find("img")
            if img and img.get("src"):
                thumbnail = urljoin(BASE_URL + "/", img["src"])

        raw_title = title_link.get_text(strip=True)
        # 제목 없는 글이 많음("-"). 원본 블로그처럼 그런 글은 제목을 비워둔다.
        display_title = raw_title if raw_title and raw_title != "-" else ""

        posts.append(
            {
                "id": post_id,
                "title": display_title,
                "link": urljoin(BASE_URL + "/", post_id),
                "date": date_text,
                "content": content,
                "thumbnail": thumbnail,
            }
        )
    return posts


def load_previous() -> list[dict]:
    if POSTS_FILE.exists():
        return json.loads(POSTS_FILE.read_text(encoding="utf-8"))
    return []


def save_current(posts: list[dict]) -> None:
    POSTS_FILE.write_text(json.dumps(posts, ensure_ascii=False, indent=2), encoding="utf-8")


def fetch_all_posts(known_ids: set[str]) -> list[dict]:
    """1페이지부터 순서대로 훑으며 글을 모은다.

    이미 알고 있는 글 id를 만나면(=예전에 저장해둔 지점까지 따라잡았으면) 멈추고,
    처음 실행이라 known_ids가 비어있으면 계속 간다. 블로그 끝(가장 오래된 글)에
    닿으면 빈 페이지 대신 마지막 글을 계속 반복해서 보여주므로, 이번 실행에서
    이미 본 id가 다시 나오면 그때 멈춘다.
    """
    posts: list[dict] = []
    seen_ids: set[str] = set()
    for page in range(1, MAX_PAGES + 1):
        entries = parse_entries(fetch_page(page=page))
        if not entries or all(p["id"] in seen_ids for p in entries):
            break
        posts.extend(entries)
        seen_ids.update(p["id"] for p in entries)
        if known_ids and any(p["id"] in known_ids for p in entries):
            break
    return posts


def main() -> None:
    previous_posts = load_previous()
    previous_ids = {p["id"] for p in previous_posts}

    current_posts = fetch_all_posts(previous_ids)
    new_posts = [p for p in current_posts if p["id"] not in previous_ids]

    if new_posts:
        print(f"새 글 {len(new_posts)}건 발견")
    else:
        print("새 글 없음")

    # 이전 목록 + 이번에 새로 본 글 병합. 지금까지 올라온 모든 글을 유지한다.
    merged = {p["id"]: p for p in previous_posts}
    merged.update({p["id"]: p for p in current_posts})
    all_posts = sorted(merged.values(), key=lambda p: int(p["id"]), reverse=True)

    save_current(all_posts)


if __name__ == "__main__":
    main()

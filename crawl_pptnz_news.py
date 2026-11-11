#!/usr/bin/env python3
"""
peppertones.net 의 news 탭(news.htm)을 파싱해서 news.json 으로 저장하는 스크립트.

news.htm 은 '-tag [2026/11/1]<br>내용' 형태의 <p> 블록이 최신순으로 쭉 이어진
정적 HTML(EUC-KR)이다. 블로그 크롤러(crawl_pptnz_blog.py)와는 완전히 별개로
동작하며, GitHub Actions에서 블로그 크롤링 직후에 같이 실행된다.

news.json 은 앱이 raw.githubusercontent.com 으로 그대로 읽어간다.
새 소식 알림은 앱이 백그라운드 새로고침 때 news.json 의 id 목록을 이전에 본 것과
비교해서 로컬 알림으로 처리한다(블로그 새 글 알림과 동일한 방식).
"""

import hashlib
import json
import re
import sys
from pathlib import Path
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

NEWS_URL = "http://www.peppertones.net/news.htm"
NEWS_FILE = Path("news.json")
USER_AGENT = "PPTNZFanWidget/1.0 (contact: wooyxxng@gmail.com)"

# 페이지가 개편되거나 일시적으로 깨져서 항목이 거의 없게 파싱되면
# 기존 news.json 을 덮어쓰지 않기 위한 안전장치. (현재 약 370건)
MIN_EXPECTED_ITEMS = 100

P_SPLIT_RE = re.compile(r"<p\b[^>]*>", re.IGNORECASE)
# "-live", "- tv", "-work & live" 처럼 맨 앞의 태그. 하이픈 뒤 공백/대소문자가 제각각이다.
TAG_RE = re.compile(r"^\s*-\s*([A-Za-z]+(?:\s*&\s*[A-Za-z]+)*)")
# 4자리 연도가 들어있는 첫 번째 [...] 가 날짜. ('[Fine.]' 같은 제목 속 대괄호와 구분)
DATE_RE = re.compile(r"\[([^\]]*\d{4}[^\]]*)\]")
YEAR_RE = re.compile(r"\d{4}")


def fetch_html() -> str:
    resp = requests.get(NEWS_URL, headers={"User-Agent": USER_AGENT}, timeout=20)
    resp.raise_for_status()
    # 서버가 charset을 내려주지 않고 페이지는 EUC-KR. cp949가 상위 호환이라 특수문자(☆ 등)도 안전.
    return resp.content.decode("cp949", errors="replace")


def normalize_tag(raw: str) -> str:
    return re.sub(r"\s+", " ", raw.strip().lower())


def parse_chunk(chunk_html: str) -> dict | None:
    soup = BeautifulSoup(chunk_html, "html.parser")

    link = None
    for a in soup.find_all("a", href=True):
        link = urljoin(NEWS_URL, a["href"].strip())
        break

    for br in soup.find_all("br"):
        br.replace_with("\n")
    text = soup.get_text().replace("\xa0", " ")

    tag_match = TAG_RE.match(text)
    if not tag_match:
        return None
    tag = normalize_tag(tag_match.group(1))
    rest = text[tag_match.end():]

    date_match = DATE_RE.search(rest)
    if not date_match:
        return None
    date = re.sub(r"\s+", " ", date_match.group(1)).strip()
    year_match = YEAR_RE.search(date)
    if not year_match:
        return None
    year = int(year_match.group(0))

    # 태그와 날짜를 제거한 나머지가 본문. 줄 단위 공백만 정리하고 줄바꿈은 살린다.
    body = rest[: date_match.start()] + rest[date_match.end():]
    lines = [re.sub(r"[ \t]+", " ", line).strip() for line in body.split("\n")]
    content = "\n".join(line for line in lines if line)
    if not content:
        return None

    return {"tag": tag, "date": date, "year": year, "content": content, "link": link}


def make_id(item: dict) -> str:
    """페이지에 고유 id가 없어서 내용 기반 해시를 id로 쓴다.
    (순서가 바뀌거나 위에 새 항목이 추가돼도 기존 항목 id는 변하지 않는다.)"""
    key = "\n".join([item["tag"], item["date"], item["content"]])
    return hashlib.sha1(key.encode("utf-8")).hexdigest()[:12]


def parse_news(html: str) -> list[dict]:
    items: list[dict] = []
    seen: dict[str, int] = {}
    # <p>가 닫히지 않은 채 이어지는 곳이 있어서, 트리로 파싱하지 않고 <p> 시작 기준으로 먼저 잘라낸다.
    for chunk in P_SPLIT_RE.split(html)[1:]:
        item = parse_chunk(chunk)
        if item is None:
            continue
        base_id = make_id(item)
        # 완전히 같은 항목이 두 번 나오는 경우를 위한 접미사
        count = seen.get(base_id, 0)
        seen[base_id] = count + 1
        item["id"] = base_id if count == 0 else f"{base_id}-{count}"
        items.append(item)
    return items


def main() -> None:
    items = parse_news(fetch_html())
    if len(items) < MIN_EXPECTED_ITEMS:
        print(f"파싱 결과가 {len(items)}건뿐이라 이상함. news.json 을 갱신하지 않음.", file=sys.stderr)
        sys.exit(1)

    ordered = [
        {k: item[k] for k in ("id", "tag", "date", "year", "content", "link")}
        for item in items
    ]
    new_text = json.dumps(ordered, ensure_ascii=False, indent=2) + "\n"

    previous_ids: set[str] = set()
    if NEWS_FILE.exists():
        try:
            previous_ids = {i["id"] for i in json.loads(NEWS_FILE.read_text(encoding="utf-8"))}
        except (ValueError, KeyError, TypeError):
            pass
        if NEWS_FILE.read_text(encoding="utf-8") == new_text:
            print("변경 없음")
            return

    added = [i for i in ordered if i["id"] not in previous_ids]
    print(f"총 {len(ordered)}건, 새 항목 {len(added)}건")
    NEWS_FILE.write_text(new_text, encoding="utf-8")


if __name__ == "__main__":
    main()

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

import datetime
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


# ---------------------------------------------------------------------------
# 투어(클럽투어 등) 항목을 공연 날짜별로 분리
#
# 원문은 한 항목에 여러 도시/날짜를 몰아 쓴 경우가 있다. 앱에서는 날짜마다 한 줄씩
# 보여주기 위해, 아래 규칙에 걸리는 live 항목을 날짜별 항목으로 쪼갠다.
#   A) 날짜란에 "날짜 도시"가 여러 개:  [2018/7/7 광주, 7/8 대전, ...] / [서울 2012/12/21~23, 부산 12/28~29]
#   B) 본문 안에 "날짜 도시 ..."가 여러 개: 페퍼톤스 클럽투어 7/11-12 서울 ..., 7/18-19 부산 ...
#   C) 날짜 N개 + 본문 끝 "at 도시,도시,도시"(N개): 순서대로 짝지음
#   수동) 규칙으로 못 푸는 항목은 MANUAL_SPLITS 에 직접 지정
# ---------------------------------------------------------------------------

# 원문 날짜란 -> 사용자가 지정한 [(날짜, 도시)] 매핑 (본문의 "도시 at 장소"와 결합)
MANUAL_SPLITS: dict[str, list[tuple[str, str]]] = {
    "2025/6/28-29, 7/5-6, 7/19-20, 7/26-27": [
        ("6/28-29", "서울"),
        ("7/5", "대구"),
        ("7/6", "부산"),
        ("7/19", "전주"),
        ("7/20", "광주"),
        ("7/26-27", "인천"),
    ],
}

DATE_TOKEN_RE = re.compile(
    r"^\s*(?P<pre>[^\d]*?)\s*(?:(?P<y>\d{4})/)?(?P<md>\d{1,2}/\d{1,2}(?:\s*[-~]\s*\d{1,2})?)\s*(?P<post>.*?)\s*$"
)
INLINE_DATE_RE = re.compile(r"(?:(?<=^)|(?<=[\s,]))(\d{1,2}/\d{1,2}(?:[-~]\d{1,2})?)\s+(?=\S)", re.MULTILINE)


def _derive(item: dict, date: str, content: str) -> dict:
    return {**item, "date": date, "content": content}


def _split_city_dates(item: dict) -> list[dict] | None:
    """A) 날짜란에 '날짜 도시'가 여러 개."""
    tokens = [t for t in item["date"].split(",") if t.strip()]
    if len(tokens) < 2:
        return None
    parsed = []
    for t in tokens:
        m = DATE_TOKEN_RE.match(t)
        city = (m.group("pre") or m.group("post")).strip() if m else ""
        if not m or not city:
            return None
        parsed.append((f"{m.group('y') or item['year']}/{m.group('md').replace(' ', '')}", city))

    cities = [c for _, c in parsed]
    lines = [l for l in item["content"].split("\n") if l.strip()]
    city_lines = {}
    title_lines = []
    for line in lines:
        city = next((c for c in cities if line.startswith(c + " ") or line == c), None)
        if city:
            city_lines[city] = line
        else:
            title_lines.append(line)
    title = " ".join(title_lines)
    return [_derive(item, d, f"{title} {city_lines.get(c, c)}".strip()) for d, c in parsed]


def _split_inline(item: dict) -> list[dict] | None:
    """B) 본문 안에 '날짜 도시 ...'가 여러 개."""
    content = item["content"]
    hits = list(INLINE_DATE_RE.finditer(content))
    if len(hits) < 3:
        return None
    title = content[: hits[0].start()].strip()
    result = []
    for i, hit in enumerate(hits):
        end = hits[i + 1].start() if i + 1 < len(hits) else len(content)
        rest = content[hit.end():end].strip(" ,\n")
        result.append(_derive(item, f"{item['year']}/{hit.group(1)}", f"{title} {rest}".strip()))
    return result


def _split_paired(item: dict) -> list[dict] | None:
    """C) 날짜 N개 + 본문 끝 'at 도시,도시,..'(N개)를 순서대로 짝짓는다."""
    tokens = [t.strip() for t in item["date"].split(",") if t.strip()]
    m = re.search(r"\s+at\s+([^,\n]+(?:,[^,\n]+)+)\s*$", item["content"])
    if len(tokens) < 2 or not m:
        return None
    places = [p.strip() for p in m.group(1).split(",")]
    if len(places) != len(tokens):
        return None
    title = item["content"][: m.start()].strip()
    month = ""
    result = []
    for token, place in zip(tokens, places):
        # 첫 토큰에만 붙어있는 연도("2011/4/15-17")는 떼고, 월이 생략된 토큰("23-24", "30-5/1")은 직전 월을 이어받는다.
        token = re.sub(r"^\d{4}/", "", token)
        lead = re.match(r"^(\d{1,2})/", token)
        if lead:
            month = lead.group(1)
        else:
            token = f"{month}/{token}"
        result.append(_derive(item, f"{item['year']}/{token}", f"{title} {place}"))
    return result


def _split_manual(item: dict) -> list[dict] | None:
    mapping = MANUAL_SPLITS.get(item["date"])
    if not mapping:
        return None
    m = re.search(r"\s(?=[가-힣]+ at )", item["content"])
    if not m:
        return None
    title = item["content"][: m.start()].strip()
    details = {c: f"{c} at {v}" for c, v in re.findall(r"([가-힣]+) at ([^,\n]+)", item["content"][m.start():])}
    return [
        _derive(item, f"{item['year']}/{d}", f"{title} {details.get(c, c)}")
        for d, c in mapping
    ]


def _parse_day(text: str, year: int, month: int | None) -> tuple[int, int, int] | None:
    """'2026/4/17' / '4/17' / '17' 형태를 (연, 월, 일)로. 월/연이 생략되면 직전 값을 이어받는다."""
    parts = [int(p) for p in text.strip().split("/")]
    if len(parts) == 3:
        return parts[0], parts[1], parts[2]
    if len(parts) == 2:
        return year, parts[0], parts[1]
    if len(parts) == 1 and month is not None:
        return year, month, parts[0]
    return None


def expand_days(item: dict) -> list[dict]:
    """live 항목의 날짜 범위('2026/4/17-19,24-26', '2017/9/2~3' 등)를 하루 단위 항목으로 쪼갠다.
    하루짜리이거나 형식을 해석할 수 없는 날짜('2020/8' 등)는 그대로 둔다."""
    if "live" not in [t.strip() for t in item["tag"].split("&")]:
        return [item]
    date = item["date"].strip()
    if not re.fullmatch(r"[\d/,\s~-]+", date) or not re.search(r"[~,-]", date):
        return [item]

    days: list[datetime.date] = []
    year, month = item["year"], None
    for token in date.split(","):
        token = token.strip()
        if not token:
            continue
        # '-' 또는 '~'로 시작/끝 날짜를 나눈다. (끝 날짜는 월이 생략될 수 있다: '17-19', '6/21-7/01')
        bounds = re.split(r"\s*[-~]\s*", token)
        if len(bounds) > 2:
            return [item]
        start = _parse_day(bounds[0], year, month)
        if start is None:
            return [item]
        year, month = start[0], start[1]
        end = _parse_day(bounds[1], year, month) if len(bounds) == 2 else start
        if end is None:
            return [item]
        try:
            first = datetime.date(*start)
            last = datetime.date(*end)
        except ValueError:
            return [item]
        if last < first or (last - first).days > 31:
            return [item]
        days.extend(first + datetime.timedelta(n) for n in range((last - first).days + 1))

    if len(days) <= 1:
        return [item]
    return [_derive(item, f"{d.year}/{d.month}/{d.day}", item["content"]) for d in days]


def split_tour(item: dict) -> list[dict]:
    if item["tag"] != "live":
        return [item]
    for splitter in (_split_manual, _split_city_dates, _split_paired, _split_inline):
        parts = splitter(item)
        if parts:
            return parts
    return [item]


def split_items(item: dict) -> list[dict]:
    """투어 분리 후, live 항목은 다시 하루 단위로 분리한다."""
    return [day for part in split_tour(item) for day in expand_days(part)]


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
        parsed = parse_chunk(chunk)
        if parsed is None:
            continue
        for item in split_items(parsed):
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

package com.wooyxxng.pptnzblog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 페퍼톤스 공식 블로그(skin `pptnz8`) CSS에서 추출한 색상 팔레트.
 * `http://peppertones.host.whoisweb.net/blog/skin/pptnz8/style.css` 기준.
 * iOS `Theme.swift`와 동일한 hex 값.
 */
/** 페이지 배경 (#FFFFFA) */
val PptnzBackground = Color(0xFFFFFFFA)

/** 본문 텍스트 (#101030) */
val PptnzInk = Color(0xFF101030)

/** 링크·강조 포인트 (#E88580) */
val PptnzCoral = Color(0xFFE88580)

/** 보조 강조 (#7AAAAA) */
val PptnzTeal = Color(0xFF7AAAAA)

/** 섹션 제목류 (#F6BBB7) */
val PptnzPink = Color(0xFFF6BBB7)

/** 글 상세 제목처럼 더 진하게 강조가 필요한 곳에 쓰는 핑크 */
val PptnzPinkStrong = Color(0xFFE8628F)

/** 헤더 "BLOG" 로고 텍스트용 크림 옐로우 */
val PptnzYellow = Color(0xFFF0EFA3)

/** 리스트 구분선 등에 쓰는 옅은 선 색 */
val PptnzDivider = PptnzInk.copy(alpha = 0.08f)

private val PptnzColorScheme = lightColorScheme(
    primary = PptnzCoral,
    background = PptnzBackground,
    surface = PptnzBackground,
    onBackground = PptnzInk,
    onSurface = PptnzInk,
)

@Composable
fun PptnzBlogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PptnzColorScheme,
        typography = Typography(),
        content = content,
    )
}

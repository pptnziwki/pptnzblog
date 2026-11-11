package com.wooyxxng.pptnzblog.ui.list

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.wooyxxng.pptnzblog.R
import com.wooyxxng.pptnzblog.data.NewsItem
import kotlin.math.ceil

/**
 * 다녀온 공연 목록을 3:4 비율 이미지(1080x1440)로 내보낸다. (iOS `AttendedExporter`)
 * 한 장에 다 들어가지 않으면 여러 장으로 나눈다. 레이아웃은 iOS와 같은 논리 크기(360x480)를
 * 3배로 키워 그린다.
 */
object AttendedExporter {
    private const val SCALE = 3f
    private const val PAGE_WIDTH = 360f
    private const val PAGE_HEIGHT = 480f

    private const val PADDING = 22f
    private const val HEADER_HEIGHT = 40f
    private const val FOOTER_HEIGHT = 16f
    private const val SECTION_SPACING = 12f
    private const val ROW_SPACING = 10f
    private const val DATE_WIDTH = 66f
    private const val COLUMN_SPACING = 8f
    private const val CONTENT_FONT = 13f
    private const val MAX_LINES = 3

    private const val BODY_HEIGHT = PAGE_HEIGHT - PADDING * 2 - HEADER_HEIGHT - FOOTER_HEIGHT - SECTION_SPACING * 2
    private const val CONTENT_WIDTH = PAGE_WIDTH - PADDING * 2 - DATE_WIDTH - COLUMN_SPACING

    private const val COLOR_BACKGROUND = 0xFFFFFFFA.toInt()
    private const val COLOR_INK = 0xFF101030.toInt()
    private const val COLOR_PINK = 0xFFF6BBB7.toInt()
    private const val COLOR_CORAL = 0xFFE88580.toInt()

    private fun px(value: Float) = value * SCALE

    private fun inkPaint(sizeDp: Float, bold: Boolean = false, alpha: Float = 1f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_INK
        this.alpha = (255 * alpha).toInt()
        textSize = px(sizeDp)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun contentLayout(item: NewsItem): StaticLayout {
        val paint = inkPaint(CONTENT_FONT).apply { textLocale = java.util.Locale.KOREAN }
        val width = px(CONTENT_WIDTH).toInt()
        val builder = StaticLayout.Builder.obtain(item.content, 0, item.content.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(MAX_LINES)
            .setEllipsize(TextUtils.TruncateAt.END)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 한글이 글자 단위로 끊기지 않고 어절 단위로 줄바꿈되게 한다.
            builder.setLineBreakConfig(
                LineBreakConfig.Builder()
                    .setLineBreakWordStyle(LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE)
                    .build(),
            )
        }
        return builder.build()
    }

    /** 항목을 순서대로 채우다가 넘치면 다음 페이지로 넘긴다. 높이는 실제 글자 레이아웃으로 잰다. */
    fun paginate(items: List<NewsItem>): List<List<NewsItem>> {
        val pages = mutableListOf<List<NewsItem>>()
        var current = mutableListOf<NewsItem>()
        var used = 0f
        val bodyHeight = px(BODY_HEIGHT)
        val spacing = px(ROW_SPACING)
        for (item in items) {
            val height = contentLayout(item).height.toFloat()
            val needed = if (current.isEmpty()) height else height + spacing
            if (current.isNotEmpty() && used + needed > bodyHeight) {
                pages.add(current)
                current = mutableListOf()
                used = 0f
            }
            used += if (current.isEmpty()) height else height + spacing
            current.add(item)
        }
        if (current.isNotEmpty()) pages.add(current)
        return pages
    }

    fun render(context: Context, items: List<NewsItem>): List<Bitmap> {
        val pages = paginate(items)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        return pages.mapIndexed { index, pageItems ->
            renderPage(logo, pageItems, items.size, index + 1, pages.size)
        }
    }

    private fun renderPage(logo: Bitmap?, items: List<NewsItem>, totalCount: Int, page: Int, pageCount: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(px(PAGE_WIDTH).toInt(), px(PAGE_HEIGHT).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BACKGROUND)

        val left = px(PADDING)
        val right = px(PAGE_WIDTH - PADDING)
        var y = px(PADDING)

        // 헤더: 로고 + "다녀온 공연 N" (+ 페이지 표시)
        if (logo != null) {
            val logoHeight = px(20f)
            val logoWidth = logoHeight * logo.width / logo.height
            val dst = android.graphics.RectF(left, y, left + logoWidth, y + logoHeight)
            canvas.drawBitmap(logo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        val titlePaint = inkPaint(16f, bold = true).apply { color = COLOR_PINK }
        canvas.drawText("다녀온 공연 $totalCount", left, y + px(20f + 4f) + titlePaint.textSize * 0.85f, titlePaint)
        if (pageCount > 1) {
            val pagePaint = inkPaint(11f, bold = true, alpha = 0.55f).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("$page / $pageCount", right, y + pagePaint.textSize, pagePaint)
        }
        y += px(HEADER_HEIGHT + SECTION_SPACING)

        // 본문: 날짜 + 내용
        val datePaint = inkPaint(11f, alpha = 0.55f)
        val contentLeft = left + px(DATE_WIDTH + COLUMN_SPACING)
        for (item in items) {
            val layout = contentLayout(item)
            canvas.drawText(item.date, left, y + px(1f) - datePaint.ascent(), datePaint)
            canvas.save()
            canvas.translate(contentLeft, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + px(ROW_SPACING)
        }

        // 푸터
        val footerPaint = inkPaint(11f, bold = true).apply {
            color = COLOR_CORAL
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("pptnz.net", right, px(PAGE_HEIGHT - PADDING) - footerPaint.descent(), footerPaint)
        return bitmap
    }

    /** 만든 이미지를 갤러리(Pictures/pptnz)에 저장한다. 저장에 성공한 장수를 돌려준다. */
    fun saveToGallery(context: Context, bitmaps: List<Bitmap>): Int {
        val stamp = System.currentTimeMillis()
        var saved = 0
        bitmaps.forEachIndexed { index, bitmap ->
            val fileName = "pptnz_attended_${stamp}_${index + 1}.png"
            val ok = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/pptnz")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } != null
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, fileName, null) != null
                }
            }.getOrDefault(false)
            if (ok) saved++
        }
        return saved
    }
}

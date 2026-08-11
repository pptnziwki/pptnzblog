package com.wooyxxng.pptnzblog.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wooyxxng.pptnzblog.data.BookmarksStore
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.data.PostsRepository
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzTeal
import kotlinx.coroutines.flow.first

enum class WidgetPostSource {
    RANDOM,
    BOOKMARKS,
}

internal val widgetSourceKey = stringPreferencesKey("widget_post_source")

/**
 * 텍스트 위주 홈 화면 위젯. iOS `PPTNZBlogWidget`(WidgetKit)에 대응.
 * 소스(랜덤/북마크함)는 위젯 추가 시 `WidgetConfigureActivity`에서 선택해 Glance 상태에 저장한다.
 */
class PptnzGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val source = prefs[widgetSourceKey]
            ?.let { runCatching { WidgetPostSource.valueOf(it) }.getOrNull() }
            ?: WidgetPostSource.RANDOM

        val repository = PostsRepository.getInstance(context)
        val allPosts = runCatching { repository.loadPosts() }.getOrElse { repository.loadCachedPosts() }

        val post = when (source) {
            WidgetPostSource.RANDOM -> allPosts.randomOrNull()
            WidgetPostSource.BOOKMARKS -> {
                val bookmarkedIds = BookmarksStore.getInstance(context).bookmarkedIdsFlow.first()
                allPosts.filter { it.id in bookmarkedIds }.randomOrNull()
            }
        }

        provideContent {
            WidgetContent(post = post, source = source)
        }
    }
}

@Composable
private fun WidgetContent(post: Post?, source: WidgetPostSource) {
    var modifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(PptnzBackground))
        .padding(12.dp)

    if (post != null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pptnzblog://post/${post.id}"))
        modifier = modifier.clickable(actionStartActivity(intent))
    }

    // iOS WidgetKit 레이아웃(pptnz.net 라벨 → 제목 → 본문 → 아래쪽 고정 작성일)과 맞춘다.
    Column(modifier = modifier) {
        // 위젯을 어떤 소스(랜덤/북마크함)로 추가했는지와 무관하게 iOS와 동일하게 항상 "pptnz.net"만 표시한다.
        Text(
            text = "pptnz.net",
            maxLines = 1,
            style = TextStyle(color = ColorProvider(PptnzTeal), fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )

        if (post == null) {
            Text(
                text = if (source == WidgetPostSource.BOOKMARKS) "저장한 글이 없어요" else "글을 불러올 수 없어요",
                maxLines = 2,
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(color = ColorProvider(PptnzInk), fontSize = 13.sp),
            )
        } else {
            Text(
                text = post.title,
                maxLines = 2,
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(color = ColorProvider(PptnzInk), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = post.content,
                maxLines = 8,
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(color = ColorProvider(PptnzInk.copy(alpha = 0.65f)), fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = post.date,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(PptnzPink), fontSize = 11.sp),
            )
        }
    }
}

class PptnzWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PptnzGlanceWidget()
}

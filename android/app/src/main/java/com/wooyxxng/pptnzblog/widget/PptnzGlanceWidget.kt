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
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPinkStrong
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
        .padding(16.dp)

    if (post != null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pptnzblog://post/${post.id}"))
        modifier = modifier.clickable(actionStartActivity(intent))
    }

    Column(modifier = modifier) {
        Text(
            text = if (source == WidgetPostSource.BOOKMARKS) "북마크함" else "pptnz.net",
            maxLines = 1,
            style = TextStyle(color = ColorProvider(PptnzTeal), fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.padding(top = 6.dp))

        if (post == null) {
            Text(
                text = if (source == WidgetPostSource.BOOKMARKS) "저장한 글이 없어요" else "글을 불러올 수 없어요",
                style = TextStyle(color = ColorProvider(PptnzInk), fontSize = 14.sp),
            )
        } else {
            // 위젯 배경(크림색)과 대비가 약한 연한 핑크 대신, 진한 핑크로 제목이 잘 보이게 한다.
            Text(
                text = post.title,
                maxLines = 2,
                style = TextStyle(color = ColorProvider(PptnzPinkStrong), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.padding(top = 6.dp))
            Text(
                text = post.content,
                maxLines = 2,
                style = TextStyle(color = ColorProvider(PptnzInk), fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.padding(top = 6.dp))
            Text(
                text = post.date,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(PptnzCoral), fontSize = 11.sp),
            )
        }
    }
}

class PptnzWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PptnzGlanceWidget()
}

package com.wooyxxng.pptnzblog.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzBlogTheme
import kotlinx.coroutines.launch

/**
 * 위젯을 홈 화면에 추가할 때 나오는 소스 선택 화면. iOS `PostSourceIntent`(위젯 편집 메뉴)에 대응.
 */
class WidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            PptnzBlogTheme {
                WidgetConfigureScreen(onSourceSelected = { source -> saveAndFinish(source) })
            }
        }
    }

    private fun saveAndFinish(source: WidgetPostSource) {
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@WidgetConfigureActivity)
            val glanceId = manager.getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigureActivity, glanceId) { prefs ->
                prefs[widgetSourceKey] = source.name
            }
            PptnzGlanceWidget().updateAll(this@WidgetConfigureActivity)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@Composable
private fun WidgetConfigureScreen(onSourceSelected: (WidgetPostSource) -> Unit) {
    var selected by remember { mutableStateOf(WidgetPostSource.RANDOM) }

    Scaffold(containerColor = PptnzBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "위젯에 표시할 글",
                style = MaterialTheme.typography.headlineSmall,
                color = PptnzPink,
            )

            SourceOption(
                title = "랜덤",
                description = "전체 글 중에서 무작위로 보여줘요.",
                selected = selected == WidgetPostSource.RANDOM,
                onClick = { selected = WidgetPostSource.RANDOM },
            )
            SourceOption(
                title = "북마크함",
                description = "저장한 글 중에서 무작위로 보여줘요.",
                selected = selected == WidgetPostSource.BOOKMARKS,
                onClick = { selected = WidgetPostSource.BOOKMARKS },
            )

            Button(onClick = { onSourceSelected(selected) }) {
                Text("추가하기")
            }
        }
    }
}

@Composable
private fun SourceOption(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PptnzCoral.copy(alpha = 0.15f) else PptnzInk.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = PptnzInk)
        Text(description, style = MaterialTheme.typography.bodySmall, color = PptnzInk.copy(alpha = 0.7f))
    }
}

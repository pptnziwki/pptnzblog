package com.wooyxxng.pptnzblog.ui.list

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.data.NewsItem
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 다녀온 공연 모아보기. 좌측 상단 다운로드 버튼으로 3:4 이미지(여러 장일 수 있음)를 갤러리에 저장한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendedScreen(
    items: List<NewsItem>,
    onToggleAttended: (NewsItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var isExporting by remember { mutableStateOf(false) }
    val sortedItems = remember(items) { items.sortedByDescending { it.sortKey } }

    fun export() {
        if (isExporting || sortedItems.isEmpty()) return
        scope.launch {
            isExporting = true
            val saved = withContext(Dispatchers.Default) {
                val bitmaps = AttendedExporter.render(context, sortedItems)
                AttendedExporter.saveToGallery(context, bitmaps)
            }
            isExporting = false
            val message = if (saved > 0) "${saved}장의 이미지를 갤러리에 저장했어요." else "이미지를 저장하지 못했어요."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (items.isEmpty()) "다녀온 공연" else "다녀온 공연 (${items.size})",
                        fontWeight = FontWeight.Bold,
                        color = PptnzInk,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { export() }, enabled = items.isNotEmpty() && !isExporting) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PptnzCoral)
                        } else {
                            Icon(Icons.Outlined.Download, contentDescription = "이미지로 저장")
                        }
                    }
                },
                actions = { TextButton(onClick = onDismiss) { Text("닫기") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(PptnzBackground)) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("다녀온 공연이 없어요\nNEWS 목록에서 공연 오른쪽 체크박스를 눌러 추가해보세요.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    items(sortedItems, key = { it.id }) { item ->
                        NewsRow(
                            item = item,
                            isAttended = true,
                            onToggleAttended = { onToggleAttended(item) },
                            onOpenLink = { link -> runCatching { uriHandler.openUri(link) } },
                        )
                        HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

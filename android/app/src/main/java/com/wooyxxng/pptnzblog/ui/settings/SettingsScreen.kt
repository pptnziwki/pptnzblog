package com.wooyxxng.pptnzblog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wooyxxng.pptnzblog.BuildConfig
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.DailyTime
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/** 설정 화면 항목 행의 통일된 높이 */
private val SettingsRowHeight = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    dailyTimes: List<DailyTime>,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onDailyTimesChange: (List<DailyTime>) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingTime by remember { mutableStateOf<DailyTime?>(null) }

    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold, color = PptnzInk) },
                actions = { TextButton(onClick = onDismiss) { Text("닫기", color = PptnzCoral) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PptnzBackground)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 알림
            SettingsSection(title = "알림") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(SettingsRowHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("알림 허용", color = PptnzInk, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PptnzBackground,
                            checkedTrackColor = PptnzCoral,
                            checkedBorderColor = PptnzCoral,
                            uncheckedThumbColor = PptnzBackground,
                            uncheckedTrackColor = PptnzInk.copy(alpha = 0.2f),
                            uncheckedBorderColor = PptnzInk.copy(alpha = 0.2f),
                        ),
                    )
                }

                if (notificationsEnabled) {
                    dailyTimes.forEach { time ->
                        HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(SettingsRowHeight),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { editingTime = time }, contentPadding = PaddingValues(0.dp)) {
                                Text(
                                    "매일 %02d:%02d".format(time.hour, time.minute),
                                    color = PptnzInk,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            if (dailyTimes.size > 1) {
                                IconButton(onClick = {
                                    onDailyTimesChange(dailyTimes.filterNot { it.id == time.id })
                                }) {
                                    Icon(Icons.Filled.RemoveCircle, contentDescription = "삭제", tint = PptnzCoral)
                                }
                            }
                        }
                    }
                    if (dailyTimes.size < AppPreferences.MAX_DAILY_TIMES) {
                        HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SettingsRowHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onDailyTimesChange(dailyTimes + DailyTime(hour = 9, minute = 0)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PptnzCoral)
                            Text(
                                "알림 시간 추가",
                                color = PptnzCoral,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }

            // 페퍼톤스 공식 계정
            SettingsSection(title = "페퍼톤스 공식 계정") {
                AccountLink("공식 홈페이지", "http://peppertones.net/")
                HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                AccountLink("Instagram", "https://www.instagram.com/peppertones_official")
                HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                AccountLink("X (Twitter)", "https://x.com/pptnzexpress")
            }

            // 앱 정보
            SettingsSection(title = "앱 정보") {
                InfoRow("버전", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                AccountLink(label = "개발자", url = "https://x.com/h6ox2i?s=11", valueLabel = "@h6ox2i")
                HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                AccountLink(label = "문의하기", url = "mailto:pptnzblog@gmail.com", valueLabel = "pptnzblog@gmail.com")
            }
        }
    }

    editingTime?.let { time ->
        TimeEditDialog(
            initialHour = time.hour,
            initialMinute = time.minute,
            onConfirm = { hour, minute ->
                onDailyTimesChange(dailyTimes.map { if (it.id == time.id) it.copy(hour = hour, minute = minute) else it })
                editingTime = null
            },
            onDismiss = { editingTime = null },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PptnzInk,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PptnzInk.copy(alpha = 0.03f))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(SettingsRowHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PptnzInk)
        Text(value, color = PptnzInk.copy(alpha = 0.6f))
    }
}

@Composable
private fun AccountLink(label: String, url: String, valueLabel: String? = null) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsRowHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PptnzInk)
        if (valueLabel != null) {
            Text(valueLabel, color = PptnzCoral)
        }
    }
}

@Composable
private fun TimeEditDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PptnzBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "알림 시간 설정",
                color = PptnzInk,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WheelPicker(
                    items = (0..23).map { "%02d".format(it) },
                    selectedIndex = hour,
                    onSelectedIndexChange = { hour = it },
                    modifier = Modifier.width(64.dp),
                )
                Text(
                    ":",
                    color = PptnzInk,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                WheelPicker(
                    items = (0..59).map { "%02d".format(it) },
                    selectedIndex = minute,
                    onSelectedIndexChange = { minute = it },
                    modifier = Modifier.width(64.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("취소", color = PptnzInk.copy(alpha = 0.6f))
                }
                TextButton(onClick = { onConfirm(hour, minute) }, modifier = Modifier.weight(1f)) {
                    Text("확인", color = PptnzCoral, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val WheelItemHeight = 40.dp
private const val WheelVisibleCount = 3
private const val WheelHalfCount = WheelVisibleCount / 2

/**
 * 안드로이드 기본 [androidx.compose.material3.TimePicker]의 시계 다이얼 대신,
 * 세로 스크롤 휠 형태로 시/분을 선택하는 커스텀 피커.
 *
 * LazyColumn의 contentPadding으로 여백을 주는 방식은 리스트 맨 앞/뒤 경계에서만
 * 특별하게 취급되어(스크롤 오프셋 계산이 index 0 주변에서 어긋남) 선택 항목이
 * 가운데 줄이 아닌 다른 줄에 위치하는 문제가 있었다. 대신 위·아래에 빈 아이템을
 * 채워 넣어 모든 항목(빈 항목 포함)이 동일하게 취급되도록 한다.
 */
@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paddedItems = remember(items) {
        List<String?>(WheelHalfCount) { null } + items + List<String?>(WheelHalfCount) { null }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    Box(
        modifier = modifier.height(WheelItemHeight * WheelVisibleCount),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(PptnzCoral.copy(alpha = 0.1f)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
        ) {
            itemsIndexed(paddedItems) { paddedIndex, label ->
                val itemIndex = paddedIndex - WheelHalfCount
                Box(
                    modifier = Modifier.height(WheelItemHeight).fillParentMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (label != null) {
                        Text(
                            label,
                            color = if (itemIndex == selectedIndex) PptnzInk else PptnzInk.copy(alpha = 0.3f),
                            fontWeight = if (itemIndex == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = info.viewportSize.height / 2
            info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - center) }?.index
        }
            .filterNotNull()
            .map { it - WheelHalfCount }
            .filter { it in items.indices }
            .distinctUntilChanged()
            .collect(onSelectedIndexChange)
    }
}

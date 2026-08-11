package com.wooyxxng.pptnzblog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.BuildConfig
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.DailyTime
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral

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
                title = { Text("설정") },
                actions = { TextButton(onClick = onDismiss) { Text("닫기") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PptnzBackground)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 알림
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("알림")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("알림 허용")
                    Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsEnabledChange)
                }

                if (notificationsEnabled) {
                    dailyTimes.forEach { time ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = { editingTime = time }) {
                                Text("매일 %02d:%02d".format(time.hour, time.minute))
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
                        TextButton(onClick = {
                            onDailyTimesChange(dailyTimes + DailyTime(hour = 9, minute = 0))
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PptnzCoral)
                            Text("알림 시간 추가", color = PptnzCoral, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            Divider()

            // 페퍼톤스 공식 계정
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("페퍼톤스 공식 계정")
                AccountLink("공식 홈페이지", "http://peppertones.net/")
                AccountLink("Instagram", "https://www.instagram.com/peppertones_official")
                AccountLink("X (Twitter)", "https://x.com/pptnzexpress")
            }

            Divider()

            // 앱 정보
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("앱 정보")
                InfoRow("버전", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                AccountLink(label = "개발자", url = "https://x.com/h6ox2i?s=11", valueLabel = "@h6ox2i")
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
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value)
    }
}

@Composable
private fun AccountLink(label: String, url: String, valueLabel: String? = null) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }) {
            Text(label)
        }
        if (valueLabel != null) {
            Text(valueLabel, color = PptnzCoral)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeEditDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        text = { TimePicker(state = state) },
    )
}

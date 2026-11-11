package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzSky
import com.wooyxxng.pptnzblog.ui.theme.PptnzSkyText

/** 홈 화면의 두 섹션. 헤더 토글로 오간다. (iOS `HomeSection`) */
enum class HomeSection(val label: String) {
    BLOG("BLOG"),
    NEWS("NEWS"),
}

/** 헤더 BLOG / NEWS 토글. 기본 세그먼트 버튼에 글자만 굵게·하늘색으로 입혔다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionToggle(
    selection: HomeSection,
    onSelectionChange: (HomeSection) -> Unit,
) {
    val sections = HomeSection.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.width(200.dp)) {
        sections.forEachIndexed { index, section ->
            SegmentedButton(
                selected = selection == section,
                onClick = { onSelectionChange(section) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = sections.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = PptnzSky.copy(alpha = 0.25f),
                    activeContentColor = PptnzSkyText,
                    activeBorderColor = PptnzDivider,
                    inactiveContainerColor = PptnzBackground,
                    inactiveContentColor = PptnzSkyText,
                    inactiveBorderColor = PptnzDivider,
                ),
                icon = {},
            ) {
                Text(section.label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 화면 하단 검색창. BLOG/NEWS가 함께 쓴다. */
@Composable
fun SearchBottomBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
) {
    Surface(color = PptnzBackground, tonalElevation = 0.dp, modifier = Modifier.imePadding()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
            placeholder = {
                Text(
                    "제목·본문 검색",
                    fontSize = 13.sp,
                    color = PptnzInk.copy(alpha = 0.35f),
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = PptnzDivider,
                focusedBorderColor = PptnzDivider,
            ),
        )
    }
}

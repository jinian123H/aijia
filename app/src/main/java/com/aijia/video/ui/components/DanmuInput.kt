package com.aijia.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// 弹幕颜色选项
val DanmuColors = listOf(
    Color.White,
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Blue,
    Color.Magenta,
    Color.Cyan
)

@Composable
fun DanmuInput(
    isExpanded: Boolean,
    onSendDanmu: (content: String, color: Color, fontSize: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedFontSize by remember { mutableStateOf(24f) }

    val sendAction = {
        if (content.isNotBlank()) {
            onSendDanmu(content, selectedColor, selectedFontSize)
            content = ""
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 输入框 + 发送按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("输入弹幕内容") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { sendAction() }
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { sendAction() },
                enabled = content.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("发送", fontSize = 14.sp)
            }
        }

        // 颜色选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "颜色:",
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DanmuColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (color == selectedColor) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable {
                                selectedColor = color
                            }
                    )
                }
            }
        }

        // 字体大小滑动选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "大小:",
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Slider(
                value = selectedFontSize,
                onValueChange = { selectedFontSize = it.roundToInt().toFloat() },
                valueRange = 14f..36f,
                steps = 10,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${selectedFontSize.toInt()}px",
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp).width(36.dp)
            )
        }
    }
}

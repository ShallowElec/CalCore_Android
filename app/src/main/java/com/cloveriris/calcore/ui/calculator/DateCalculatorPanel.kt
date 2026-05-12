package com.cloveriris.calcore.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

enum class DateOperation(val label: String) {
    DIFFERENCE("日期间隔"),
    ADD_DAYS("日期+天数"),
    SUBTRACT_DAYS("日期-天数")
}

/**
 * 日期计算器面板
 *
 * 提供日期间隔、日期加减等计算功能。
 */
@Composable
fun DateCalculatorPanel(
    modifier: Modifier = Modifier
) {
    var operation by remember { mutableStateOf(DateOperation.DIFFERENCE) }
    var dateInput1 by remember { mutableStateOf("") }
    var dateInput2 by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    fun calculate() {
        error = null
        result = ""
        when (operation) {
            DateOperation.DIFFERENCE -> {
                try {
                    val d1 = LocalDate.parse(dateInput1.trim(), formatter)
                    val d2 = LocalDate.parse(dateInput2.trim(), formatter)
                    val days = ChronoUnit.DAYS.between(d1, d2)
                    result = "$days 天"
                } catch (_: DateTimeParseException) {
                    error = "请输入有效日期 (yyyy-MM-dd)"
                }
            }
            DateOperation.ADD_DAYS -> {
                try {
                    val d1 = LocalDate.parse(dateInput1.trim(), formatter)
                    val days = dateInput2.trim().toLongOrNull()
                    if (days == null) {
                        error = "天数必须是整数"
                        return
                    }
                    val d2 = d1.plusDays(days)
                    result = d2.format(formatter)
                } catch (_: DateTimeParseException) {
                    error = "请输入有效日期 (yyyy-MM-dd)"
                }
            }
            DateOperation.SUBTRACT_DAYS -> {
                try {
                    val d1 = LocalDate.parse(dateInput1.trim(), formatter)
                    val days = dateInput2.trim().toLongOrNull()
                    if (days == null) {
                        error = "天数必须是整数"
                        return
                    }
                    val d2 = d1.minusDays(days)
                    result = d2.format(formatter)
                } catch (_: DateTimeParseException) {
                    error = "请输入有效日期 (yyyy-MM-dd)"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 操作选择
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DateOperation.entries.forEachIndexed { index, op ->
                SegmentedButton(
                    selected = operation == op,
                    onClick = {
                        operation = op
                        result = ""
                        error = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, DateOperation.entries.size)
                ) {
                    Text(op.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日期输入 1
        OutlinedTextField(
            value = dateInput1,
            onValueChange = {
                dateInput1 = it
                error = null
            },
            label = { Text("日期 (yyyy-MM-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )

        // 日期输入 2（或天数）
        OutlinedTextField(
            value = dateInput2,
            onValueChange = {
                dateInput2 = it
                error = null
            },
            label = {
                Text(
                    when (operation) {
                        DateOperation.DIFFERENCE -> "对比日期 (yyyy-MM-dd)"
                        DateOperation.ADD_DAYS -> "增加天数"
                        DateOperation.SUBTRACT_DAYS -> "减少天数"
                    }
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )

        // 快速填充今天
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.TextButton(
                onClick = { dateInput1 = LocalDate.now().format(formatter) }
            ) {
                Text("今天", style = MaterialTheme.typography.labelSmall)
            }
            androidx.compose.material3.TextButton(
                onClick = { dateInput2 = LocalDate.now().format(formatter) }
            ) {
                Text("填入今天", style = MaterialTheme.typography.labelSmall)
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    dateInput1 = ""
                    dateInput2 = ""
                    result = ""
                    error = null
                }
            ) {
                Text("清空", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 计算按钮
        androidx.compose.material3.Button(
            onClick = { calculate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("计算")
        }

        // 结果展示
        if (result.isNotBlank() || error != null) {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "结果: $result",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

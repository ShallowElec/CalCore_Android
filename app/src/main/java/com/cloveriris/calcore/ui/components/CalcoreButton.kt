package com.cloveriris.calcore.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloveriris.calcore.ui.theme.CalcoreTheme

enum class ButtonType {
    NUMBER,       // 数字键
    OPERATOR,     // 运算符
    FUNCTION,     // 功能键（C, CE, %, 等）
    EQUALS,       // 等号
    MEMORY        // 记忆键
}

@Composable
fun CalcoreButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.NUMBER,
    enabled: Boolean = true
) {
    when (type) {
        ButtonType.EQUALS -> {
            Button(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        ButtonType.OPERATOR -> {
            Button(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        ButtonType.FUNCTION -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        ButtonType.MEMORY -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        ButtonType.NUMBER -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalcoreButtonPreview() {
    CalcoreTheme {
        CalcoreButton(
            label = "=",
            onClick = {},
            type = ButtonType.EQUALS
        )
    }
}

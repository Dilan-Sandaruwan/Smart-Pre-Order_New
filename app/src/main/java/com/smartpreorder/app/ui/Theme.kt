package com.smartpreorder.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Color(0xFF2E7D32),
    background = Color(0xFFFFFBF8),
    surface = Color(0xFFFFFBF8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF5D1600),
    primaryContainer = Color(0xFF842400),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF81C784),
)

@Composable
fun SmartPreOrderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}

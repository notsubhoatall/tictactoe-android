package com.example.myempty.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassStyle { LIGHT, DARK, CLEAR }

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    style: GlassStyle = GlassStyle.CLEAR,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, content = content)
}

@Composable
fun LiquidBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(content = content)
}

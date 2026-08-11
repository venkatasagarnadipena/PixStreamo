package com.example.mega_stream.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeaderButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isFocused) Color.Black else Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DribbbleLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dribbble_loader")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    Canvas(modifier = modifier.size(80.dp)) {
        drawArc(
            color = Color.Yellow,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

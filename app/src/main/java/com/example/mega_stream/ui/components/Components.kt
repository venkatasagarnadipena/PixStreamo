package com.example.mega_stream.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
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
    val view = LocalView.current
    
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { 
            if (it.isFocused) {
                view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
            }
        },
        colors = ButtonDefaults.colors(
            containerColor = Color.White, // INVERTED: Default is now White
            contentColor = Color.Black,
            focusedContainerColor = Color(0xFF1E1E1E), // INVERTED: Focused is now Dark
            focusedContentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * QR Code Display Component
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QrCodeView(bitmap: android.graphics.Bitmap, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(220.dp)
            .background(Color.White, MaterialTheme.shapes.medium)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DribbbleLoader(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
    }
}

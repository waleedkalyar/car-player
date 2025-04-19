package com.example.carplayer.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.media3.ui.PlayerView
import androidx.palette.graphics.Palette

 fun Bitmap.extractDominantColor(onColorReady: (Int) -> Unit) {
    Palette.from(this).generate { palette ->
        val dominantColor = palette?.getDominantColor(Color.BLACK) ?: Color.BLACK
        onColorReady(dominantColor)
    }
}

 fun PlayerView.applyColorAsBackground(color: Int) {
    val overlayColor = ColorUtils.setAlphaComponent(color, 180) // 70% alpha
    setBackgroundColor(overlayColor)
}
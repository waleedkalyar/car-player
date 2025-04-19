package com.example.carplayer.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.media3.ui.PlayerView
import androidx.palette.graphics.Palette
import androidx.core.graphics.createBitmap

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


fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        this.bitmap?.let { return it }
    }

    val width = if (intrinsicWidth > 0) intrinsicWidth else 1
    val height = if (intrinsicHeight > 0) intrinsicHeight else 1

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap
}
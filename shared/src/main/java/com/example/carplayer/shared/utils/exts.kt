package com.example.carplayer.shared.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import java.io.ByteArrayOutputStream

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}


suspend fun Context.toImageBitmap(imageUrl: String, maxBytes: Long = 5_000_000): Bitmap? {
    return try {
        val request = ImageRequest.Builder(this)
            .data(imageUrl)
            .allowHardware(false) // needed to access bitmap
            .size(512) // safely resize large images
            .build()

        val result = this.imageLoader.execute(request)
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
        bitmap
//        val byteSize = bitmap?.allocationByteCount ?: 0
//        Log.d("ImageSafety", "Decoded bitmap size = $byteSize bytes")
//
//        byteSize < maxBytes
    } catch (e: Exception) {
        null
    }
}
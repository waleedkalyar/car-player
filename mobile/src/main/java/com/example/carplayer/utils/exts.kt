package com.example.carplayer.utils

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.view.LayoutInflater
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri
import com.example.carplayer.R

suspend fun Activity.checkMediaTypeFromUrl(url: String, onResult: (String?) -> Unit) {

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val contentType = connection.contentType // e.g., "audio/mpeg", "video/mp4"
            withContext(Dispatchers.Main) {
                onResult(contentType)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null) // failed to fetch type
            }
        }

}

private suspend fun detectMediaType(url: String): String? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.setRequestProperty("Icy-MetaData", "1") // For Icecast streams
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.connect()

        connection.contentType?.lowercase()?.also {
            println("Detected Content-Type: $it")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun Activity.detectAudioOrVideoStream(url: String, onResult: (String?) -> Unit) {
    val contentType = detectMediaType(url)
     when {
        contentType?.startsWith("video") == true -> onResult.invoke("video")
        contentType?.startsWith("audio") == true -> onResult.invoke("audio")
        else -> onResult.invoke(guessMediaTypeFromUrl(url))
    }
}

private fun guessMediaTypeFromUrl(url: String): String {
    val uri = url.toUri()
    val path = uri.lastPathSegment ?: return "unknown"

    return when {
        path.endsWith(".mp3", true) || path.endsWith(".aac", true) || path.endsWith(".m3u", true) -> "audio"
        path.endsWith(".m3u8", true) || path.endsWith(".ts", true) || path.endsWith(".mp4", true) -> "video"
        else -> "unknown"
    }
}



private var Activity.progressDialog: AlertDialog?
    get() = this.window.decorView.getTag(R.id.progress_dialog_tag) as? AlertDialog
    set(value) {
        this.window.decorView.setTag(R.id.progress_dialog_tag, value)
    }

fun Activity.showLoadingDialog() {
    if (progressDialog == null) {
        val builder = AlertDialog.Builder(this)
        builder.setView(LayoutInflater.from(this).inflate(R.layout.dialog_loading, null))
        builder.setCancelable(false)
        progressDialog = builder.create()
    }
    progressDialog?.show()
}

fun Activity.hideLoadingDialog() {
    progressDialog?.dismiss()
    progressDialog = null
}


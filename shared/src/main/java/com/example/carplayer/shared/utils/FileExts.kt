package com.example.carplayer.shared.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.carplayer.shared.database.AlbumsDao
import com.example.carplayer.shared.models.TrackAlbumModel
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream



fun AlbumsDao.exportAlbumsToCsv(context: Context): Boolean {
    val fileName = "albums_export.csv"
    return try {
        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for API 29+
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)
            } else null
        } else {
            // Legacy approach for API < 29 (requires WRITE_EXTERNAL_STORAGE)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }

        outputStream?.bufferedWriter().use { writer ->
            writer?.appendLine("id,channel,title,streamUrl,playBoxUrl")
            getAll().forEach { album ->
                writer?.appendLine(
                    listOf(
                        album.id,
                        album.channelNumber.toString(),
                        album.title,
                        album.streamUrl,
                        album.playBoxUrl.toString(),
                    ).joinToString(",") { it.csvEscape() }
                )
            }
        }

        Log.d("AlbumsDao", "CSV export successful to Downloads folder")
        true
    } catch (e: Exception) {
        Log.e("AlbumsDao", "CSV export failed: ${e.message}", e)
        false
    }
}


fun AlbumsDao.importAlbumsFromCsv(context: Context, uri: Uri): Boolean {
    return try {
        val albums = mutableListOf<TrackAlbumModel>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLine() // Skip header
                reader.lineSequence().forEach { line ->
                    val tokens = line.splitCsv()
                    if (tokens.size >= 3) {
                        albums.add(
                            TrackAlbumModel(
                                id = tokens[0],
                                channelNumber =tokens[1].toIntOrNull() ?: 0,//getMaxChannelNumber() ?: 0, //
                                title = tokens[2],
                                        // Fill as needed
                                streamUrl = tokens[3],
                                playBoxUrl = tokens[4],
                                isPlaying = false,
                                imageUrl = ""
                            )
                        )
                    }
                }
            }
        }

        insertAll(*albums.toTypedArray())

        Log.d("AlbumsDao", "Imported ${albums.size} albums from CSV")
        true
    } catch (e: Exception) {
        Log.e("AlbumsDao", "Import failed: ${e.message}", e)
        false
    }
}




private fun String.csvEscape(): String {
    return if (this.contains(",") || this.contains("\"") || this.contains("\n")) {
        "\"" + this.replace("\"", "\"\"") + "\""
    } else {
        this
    }
}

private fun String.splitCsv(): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (c in this) {
        when (c) {
            '"' -> {
                inQuotes = !inQuotes
            }
            ',' -> {
                if (inQuotes) {
                    current.append(c)
                } else {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
            }
            else -> current.append(c)
        }
    }
    result.add(current.toString().trim())
    return result
}

package com.example.carplayer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import androidx.core.net.toUri

fun Context.launchTrackInMusicApp(artist: String, track: String) {
    val query = URLEncoder.encode("$artist $track", "UTF-8")

    val apps = listOf(
        MusicApp(
            name = "YouTube Music",
            packageName = "com.google.android.apps.youtube.music",
            uri = null, // No working deep link for search
            webUrl = "https://www.youtube.com/results?search_query=$query"
        ),
        MusicApp(
            name = "Spotify",
            packageName = "com.spotify.music",
            uri = "spotify:search:$query".toUri(),
            webUrl = "https://open.spotify.com/search/$query"
        ),
        MusicApp(
            name = "Tidal",
            packageName = "com.aspiro.tidal",
            uri = "tidal://search?query=$query".toUri(),
            webUrl = "https://tidal.com/browse/search/$query"
        ),
        MusicApp(
            name = "Deezer",
            packageName = "deezer.android.app",
            uri = "deezer://search/$query".toUri(),
            webUrl = "https://www.deezer.com/search/$query"
        ),
        MusicApp(
            name = "Apple Music",
            packageName = null, // No native deep link on Android
            uri = null,
            webUrl = "https://music.apple.com/us/search?term=$query"
        )
    )

    for (app in apps) {
        try {
            if (app.packageName != null && isAppInstalled(this, app.packageName)) {
                val intent = Intent(Intent.ACTION_VIEW, app.uri)
                intent.setPackage(app.packageName)
                this.startActivity(intent)
                return // success
            } else if (app.packageName == null) {
                // Apple Music (browser only)
                this.startActivity(Intent(Intent.ACTION_VIEW, app.webUrl.toUri()))
                return
            }
        } catch (e: Exception) {
            // Continue to next app
        }
    }

    // Final fallback
    Toast.makeText(this, "No supported music apps found.", Toast.LENGTH_SHORT).show()
}

fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}


data class MusicApp(
    val name: String,
    val packageName: String?,
    val uri: Uri?,
    val webUrl: String
)

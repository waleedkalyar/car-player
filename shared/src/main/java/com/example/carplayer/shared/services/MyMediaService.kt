package com.example.carplayer.shared.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.example.carplayer.shared.models.TrackAlbumModel
import com.example.carplayer.shared.network.api.lastFmApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.UUID

class MyMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    var lastMetadataKey: String? = null


    val mediaItems = listOf(
        // MediaItem.fromUri("https://icecast.radiofrance.fr/fip-hifi.aac"),
        MediaItem.fromUri("https://stream-dc1.radioparadise.com/aac-320"),
 //       MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-anewbeginning.mp3"),
//            MediaItem.fromUri("https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/KODOMOi/Sunny/KODOMOi_-_Sunny.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-creativeminds.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-dubstep.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-memories.mp3"),

//            MediaItem.Builder()
//                .setUri("https://stream-dc1.radioparadise.com/aac-320")
//                .build(),


//           MediaItem.fromUri("http://pewaukee.loginto.me:33000/0.ts"),
        // mediaItem,
        MediaItem.fromUri("http://pewaukee.loginto.me:49000/stream2")
    )


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val okHttpClient = OkHttpClient.Builder().build()
        val dataSourceFactory2 = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))


        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory2)







        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                addAnalyticsListener(object : AnalyticsListener {

//                    override fun onEvents(player: Player, events: AnalyticsListener.Events) {
//                        super.onEvents(player, events)
//                        if (!events.contains(AnalyticsListener.EVENT_IS_PLAYING_CHANGED)) {
//                            return
//                        }
//
//                        //for (i in 0 until player.mediaMetadata.) {
//                            val entry = player.mediaMetadata
//                            //if (entry is IcyInfo) {
//                                val streamTitle = entry.title // e.g., "Artist - Title"
//                                val (artist, track) = streamTitle?.split(" - ")
//                                    ?.let { it.getOrNull(0) to it.getOrNull(1) }
//                                    ?: (null to null)
//
//                                if (!artist.isNullOrBlank() && !track.isNullOrBlank()) {
//                                    updateMetadata(artist, track, mediaItems = mediaItems)
//                                }
//                           // }
//                        //}
//
//                    }

//                    override fun onMetadata(
//                        eventTime: AnalyticsListener.EventTime,
//                        metadata: androidx.media3.common.Metadata
//                    ) {
//                        for (i in 0 until metadata.length()) {
//                            val entry = metadata[i]
//                            if (entry is IcyInfo) {
//                                val streamTitle = entry.title // e.g., "Artist - Title"
//                                val (artist, track) = streamTitle?.split(" - ")
//                                    ?.let { it.getOrNull(0) to it.getOrNull(1) }
//                                    ?: (null to null)
//
//                                if (!artist.isNullOrBlank() && !track.isNullOrBlank()) {
//                                    updateMetadata(artist, track, mediaItems = mediaItems)
//                                }
//                            }
//                        }
//                    }

                })
            }


        // Set your audio/video media item (HLS, MP4, etc.)
        val mediaItem = MediaItem.Builder()
            .setUri("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
            .setMimeType("application/x-mpegURL")
            .build()

        //player?.setMediaItem(mediaItem)


        player?.setMediaItems(mediaItems)



        player?.prepare()
        player?.playWhenReady = true

        player?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                val rawTitle = mediaMetadata.title?.toString()
                val rawArtist = mediaMetadata.artist?.toString()


                val metadataKey = "$rawArtist|$rawTitle"

                // Skip if empty or identical to last
                if (rawTitle.isNullOrBlank() || metadataKey == lastMetadataKey) {
                    return
                }

                lastMetadataKey = metadataKey // Update the cache

                val (artist, track) = rawTitle.split(" - ").let {
                    it.getOrNull(0).orEmpty() to it.getOrNull(1).orEmpty()
                }
                Log.d(
                    "MyMediaService",
                    "artist -> $artist  track -> $track artwork -> ${mediaMetadata.artworkUri}"
                )
                if (artist.isNotBlank() && track.isNotBlank() && mediaMetadata.artworkUri?.toString()
                        .isNullOrEmpty()
                ) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var albumInfo =
                            fetchAlbumArt(artist, track, "c62a1d89e34fa71897a4bb4df15e8510")


                        val newMetadata = MediaMetadata.Builder()
                            .setTitle(albumInfo?.title ?: "Unknown")
                            .setArtist(albumInfo?.artist ?: "Unknown")
                            .setArtworkUri((albumInfo?.imageUrl ?: "").toUri())
                            .build()


                        val updatedMediaItem = player?.currentMediaItem
                            ?.buildUpon()
                            ?.setMediaMetadata(newMetadata)
                            ?.build()

                        val currentIndex = player?.currentMediaItemIndex ?: RESULT_ERROR

                        if (updatedMediaItem != null) {
                            player?.replaceMediaItem(currentIndex, updatedMediaItem)
                        }


                    }

                }

            }
        })


        mediaSession = MediaSession.Builder(this, player!!)
//            .setCallback(object : MediaSession.Callback {
//
//                override fun onConnect(
//                    session: MediaSession,
//                    controller: MediaSession.ControllerInfo
//                ): MediaSession.ConnectionResult {
//                    val allowedCommands = SessionCommands.Builder()
//                        .add(SessionCommand("UPDATE_METADATA", Bundle.EMPTY))
//                        .build()
//                    return MediaSession.ConnectionResult.accept(
//                        allowedCommands,
//                        Player.Commands.Builder().addAllCommands().build()
//                    )
//                }
//
//
//                override fun onCustomCommand(
//                    session: MediaSession,
//                    controller: MediaSession.ControllerInfo,
//                    customCommand: SessionCommand,
//                    args: Bundle
//                ): ListenableFuture<SessionResult> {
//                    if (customCommand.customAction == "UPDATE_METADATA") {
//                        val title = args.getString("title") ?: ""
//                        val artist = args.getString("artist") ?: ""
//                        val artworkUrl = args.getString("artworkUrl") ?: ""
//
//                        Log.d("CustomCommand", "title -> $title , artist -> $artist")
//
//                        val newMetadata = MediaMetadata.Builder()
//                            .setTitle(title)
//                            .setArtist(artist)
//                            .setArtworkUri(artworkUrl.toUri())
//                            .build()
//
//
//                        val updatedMediaItem = player?.currentMediaItem
//                            ?.buildUpon()
//                            ?.setMediaMetadata(newMetadata)
//                            ?.build()
//
//                        val currentIndex = player?.currentMediaItemIndex ?: RESULT_ERROR
//
//                        if (updatedMediaItem != null) {
//
//                            val items = mediaItems.toMutableList()
//                            items[currentIndex] = updatedMediaItem
//                            player?.setMediaItems(items, currentIndex, C.TIME_UNSET)
//                        }
//
//
//                    }
//
//                    // return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
//
//                    return super.onCustomCommand(session, controller, customCommand, args)
//                }
//            })
            .build()


        // ✅ Start Foreground with basic media notification
        //startForegroundWithNotification()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }


    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundWithNotification() {
        val channelId = "media_playback_channel"
        val channelName = "Media Playback"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CarPlayer")
            .setContentText("Playing music...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }


    suspend fun fetchAlbumArt(artist: String, track: String, apiKey: String): TrackAlbumModel? {
        try {
            val response = lastFmApi.getTrackInfo(apiKey, artist, track)
            val images = response.track?.album?.image
            // Pick "extralarge" or the biggest one
            var imageUrl = images?.findLast { it.size == "extralarge" || it.size == "mega" }?.url

            var title: String = response.track?.album?.title.toString()
            var artist = response.track?.artist?.name.toString()



            return TrackAlbumModel(
                id = response.track?.mbid ?: UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                imageUrl = imageUrl.toString()
            )

        } catch (e: Exception) {
            Log.e("LastFm", "Error fetching album art: ${e.message}")
            return null

        }
    }

    fun updateMetadata(artist: String, track: String, mediaItems: List<MediaItem>) {
        CoroutineScope(Dispatchers.Main).launch {
            val albumInfo = fetchAlbumArt(artist, track, "c62a1d89e34fa71897a4bb4df15e8510")

            val newMetadata = MediaMetadata.Builder()
                .setTitle(track)
                .setArtist(artist)
                .setArtworkUri((albumInfo?.imageUrl ?: "").toUri())
                .build()


            val currentItem = player?.currentMediaItem ?: return@launch
            val newItem = currentItem.buildUpon()
                .setMediaMetadata(newMetadata)
                .build()
            val index = player?.currentMediaItemIndex ?: return@launch
            val currentIndex = player?.currentMediaItemIndex ?: 0
            val currentList = mediaItems.toMutableList()

            // 🧠 Only replace the current item
            currentList[index] = newItem

            val playWhenReady = player?.playWhenReady
            val position = player?.currentPosition

            currentList[index] = newItem

            player?.setMediaItems(currentList, currentIndex, position ?: 0L)
            player?.playWhenReady = playWhenReady == true
        }
    }


    companion object {
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = -1
    }

}


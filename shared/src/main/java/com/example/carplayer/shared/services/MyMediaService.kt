package com.example.carplayer.shared.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask

class MyMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Set your audio/video media item (HLS, MP4, etc.)
        val mediaItem = MediaItem.Builder()
            .setUri("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
            .setMimeType("application/x-mpegURL")
            .build()

        //player?.setMediaItem(mediaItem)


        val mediaItems = listOf(
            MediaItem.fromUri("https://icecast.radiofrance.fr/fip-hifi.aac"),
//            MediaItem.fromUri("https://stream-dc1.radioparadise.com/aac-320"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-anewbeginning.mp3"),
//            MediaItem.fromUri("https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/KODOMOi/Sunny/KODOMOi_-_Sunny.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-creativeminds.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-dubstep.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-memories.mp3"),

            MediaItem.Builder()
                .setUri("https://stream-dc1.radioparadise.com/aac-320")
                .build(),


//            MediaItem.fromUri("http://pewaukee.loginto.me:33000/0.ts"),
           // mediaItem,
            MediaItem.fromUri("http://pewaukee.loginto.me:49000/stream2")
        )


        player?.setMediaItems(mediaItems)



        player?.prepare()
        player?.playWhenReady = true

        mediaSession = MediaSession.Builder(this, player!!)

            .setCallback(object : MediaSession.Callback{

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val allowedCommands = SessionCommands.Builder()
                        .add(SessionCommand("UPDATE_METADATA", Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.accept(
                        allowedCommands,
                        Player.Commands.Builder().addAllCommands().build()
                    )
                }



                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if(customCommand.customAction == "UPDATE_METADATA"){
                        val title = args.getString("title") ?: ""
                        val artist = args.getString("artist") ?: ""
                        val artworkUrl = args.getString("artworkUrl") ?: ""

                        Log.d("CustomCommand","title -> $title , artist -> $artist")

                        val newMetadata = MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .setArtworkUri(artworkUrl.toUri())
                            .build()


                        val updatedMediaItem = player?.currentMediaItem
                            ?.buildUpon()
                            ?.setMediaMetadata(newMetadata)
                            ?.build()

                        val currentIndex = player?.currentMediaItemIndex ?: RESULT_ERROR

                        if (updatedMediaItem != null) {

                            val items = mediaItems.toMutableList()
                            items[currentIndex] = updatedMediaItem
                            player?.setMediaItems(items, currentIndex, C.TIME_UNSET)
                        }



                    }

                   // return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))

                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
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

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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


    companion object {
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = -1
    }

}


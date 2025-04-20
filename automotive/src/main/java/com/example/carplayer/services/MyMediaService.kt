package com.example.carplayer.services

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MyMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val mediaSourceFactory = DefaultMediaSourceFactory(this)

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
//            MediaItem.fromUri("https://icecast.radiofrance.fr/fip-hifi.aac"),
//            MediaItem.fromUri("https://stream-dc1.radioparadise.com/aac-320"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-anewbeginning.mp3"),
//            MediaItem.fromUri("https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/KODOMOi/Sunny/KODOMOi_-_Sunny.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-creativeminds.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-dubstep.mp3"),
//            MediaItem.fromUri("https://www.bensound.com/bensound-music/bensound-memories.mp3"),

//            MediaItem.Builder()
//                .setUri("https://stream-dc1.radioparadise.com/aac-320")
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setTitle("DC Once")
//                        .setArtist("DCOne Radio Station")
//                        .setArtworkUri("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSjTQhQiSTPZZdvDVEM9L3eAtuHIfAgi5DmIf50-71d9akTsspWf5mOMVDxp2nOrbgfgE0&usqp=CAU".toUri()) // <- Set artwork here
//                        .build()
//                )
//                .build(),

            MediaItem.Builder()
                .setUri("http://pewaukee.loginto.me:49000/stream2")
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setTitle("Pewaukee Stream")
//                        .setArtist("Pewaukee Radio")
//                        .setArtworkUri("https://cdn-images.dzcdn.net/images/cover/8c989b334b11c25f27c70b9bd74ec667/1900x1900-000000-80-0-0.jpg".toUri()) // <- Set artwork here
//                        .build()
//                )
                .build(),
            MediaItem.fromUri("http://pewaukee.loginto.me:33000/0.ts"),
            //mediaItem,
           // MediaItem.fromUri("http://pewaukee.loginto.me:49000/stream2")
        )


        player?.setMediaItems(mediaItems)



        player?.prepare()
        player?.playWhenReady = true

        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }
}


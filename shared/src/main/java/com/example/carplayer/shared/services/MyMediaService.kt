package com.example.carplayer.shared.services

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.carplayer.shared.R
import com.example.carplayer.shared.models.TrackAlbumModel
import com.example.carplayer.shared.network.api.lastFmApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.UUID

class MyMediaService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    var lastMetadataKey: String? = null


   lateinit var defaultArtWorkUri: Uri


    val mediaItems: MutableList<MediaItem> = mutableListOf()


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

         defaultArtWorkUri =
            "android.resource://${packageName}/${R.drawable.default_album_art}".toUri()



      mediaItems.add(MediaItem.Builder().setUri("http://pewaukee.loginto.me:49000/stream2")
            .setMediaMetadata(MediaMetadata.Builder()
                .setArtworkUri(defaultArtWorkUri)
                .setDescription("local")
                .build())
            .build())
//        mediaItems.add(MediaItem.Builder().setUri("http://pewaukee.loginto.me:49000/stream2")
//            .setMediaMetadata(MediaMetadata.Builder()
//                .setArtworkUri(defaultArtWorkUri)
//                .setDescription("local")
//                .build())
//            .build())




        val okHttpClient = OkHttpClient.Builder().build()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)


        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()


        player?.setMediaItems(mediaItems)



        player?.prepare()
        player?.playWhenReady = true

        player?.addListener(object : Player.Listener {

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val rawTitle = mediaItem?.mediaMetadata?.toString()
                val rawArtist =mediaItem?. mediaMetadata?.artist?.toString()

                Log.d(
                    "MyMediaService",
                    "onMediaMetadataChanged: -> description -> ${mediaItem?.mediaMetadata?.description}"
                )

                //  val metadataKey = "$rawArtist|$rawTitle"

                // Skip if empty or identical to last
//                if (mediaMetadata.description == "API") {
//                    Log.d("MyMediaService", "onMediaMetadataChanged: found from API returning")
//                    return
//                }


                val (artist, track) = rawTitle?.split(" - ").let {
                    it?.getOrNull(0).orEmpty() to it?.getOrNull(1).orEmpty()
                }
                Log.d(
                    "MyMediaService",
                    "artist -> $artist  track -> $track artwork -> ${mediaItem?.mediaMetadata?.artworkUri}"
                )
                if (artist.isNotBlank() && track.isNotBlank() && mediaItem?.mediaMetadata?.artworkUri?.toString()
                        .equals(defaultArtWorkUri.toString())
                ) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var albumInfo =
                            fetchAlbumArt(artist, track, "c62a1d89e34fa71897a4bb4df15e8510")


                        val newMetadata = MediaMetadata.Builder()
                            .setTitle(albumInfo?.title ?: "Unknown")
                            .setArtist(albumInfo?.artist ?: "Unknown")
                            .setArtworkUri((albumInfo?.imageUrl ?: "").toUri())
                            .setSubtitle(albumInfo?.artist)
                            .setAlbumTitle(albumInfo?.title ?: "Unknown")
                            .setAlbumArtist(albumInfo?.artist ?: "Unknown")
                            .setDescription("API")
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

//            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
//                super.onMediaMetadataChanged(mediaMetadata)
//                val rawTitle = mediaMetadata.title?.toString()
//                val rawArtist = mediaMetadata.artist?.toString()
//
//
//
//
//                Log.d("MyMediaService", "onMediaMetadataChanged: -> description -> ${mediaMetadata.description}")
//
//              //  val metadataKey = "$rawArtist|$rawTitle"
//
//                // Skip if empty or identical to last
////                if (mediaMetadata.description == "API") {
////                    Log.d("MyMediaService", "onMediaMetadataChanged: found from API returning")
////                    return
////                }
//
//
//                val (artist, track) = rawTitle?.split(" - ").let {
//                    it?.getOrNull(0).orEmpty() to it?.getOrNull(1).orEmpty()
//                }
//                Log.d(
//                    "MyMediaService",
//                    "artist -> $artist  track -> $track artwork -> ${mediaMetadata.artworkUri}"
//                )
//                if (artist.isNotBlank() && track.isNotBlank() && mediaMetadata.artworkUri?.toString().equals(defaultArtWorkUri.toString())
//                ) {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        var albumInfo =
//                            fetchAlbumArt(artist, track, "c62a1d89e34fa71897a4bb4df15e8510")
//
//
//                        val newMetadata = MediaMetadata.Builder()
//                            .setTitle(albumInfo?.title ?: "Unknown")
//                            .setArtist(albumInfo?.artist ?: "Unknown")
//                            .setArtworkUri((albumInfo?.imageUrl ?: "").toUri())
//                            .setSubtitle(albumInfo?.artist)
//                            .setAlbumTitle(albumInfo?.title ?: "Unknown")
//                            .setAlbumArtist(albumInfo?.artist ?: "Unknown")
//                            .setDescription("API")
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
//                            player?.replaceMediaItem(currentIndex, updatedMediaItem)
//                        }
//
//
//                    }
//
//                }
//
//            }
        })


        mediaSession = MediaSession.Builder(this, player!!)
            .build()

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }


    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
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
            return TrackAlbumModel(id = UUID.randomUUID().toString(), title = track, artist = artist, imageUrl = defaultArtWorkUri.toString())

        }
    }



    companion object {
        const val RESULT_SUCCESS = 0
        const val RESULT_ERROR = -1
    }

}


package com.example.carplayer.shared.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyHeaders
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import coil.imageLoader
import coil.request.ImageRequest
import com.example.carplayer.shared.R
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.models.TrackAlbumModel
import com.example.carplayer.shared.network.api.lastFmApi
import com.example.carplayer.shared.utils.AutoMediaConstants
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.core.net.toUri


class MyMediaService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null

    var lastMetadataKey: String? = null


    lateinit var defaultArtWorkUri: Uri


    lateinit var database: CarPlayerDatabase

    val mediaItems: MutableList<MediaItem> = mutableListOf()


    val sessionCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putInt(
                    AutoMediaConstants.CONTENT_STYLE_SUPPORTED,
                    AutoMediaConstants.CONTENT_STYLE_GRID_ITEM_HINT
                )
                putInt(
                    AutoMediaConstants.CONTENT_STYLE_BROWSABLE_HINT,
                    AutoMediaConstants.CONTENT_STYLE_GRID_ITEM_HINT
                )
            }

            val rootMetadata = MediaMetadata.Builder()
                .setTitle("My Library")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setExtras(rootExtras) // put extras *here*
                .build()

            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(rootMetadata)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem,params))
        }


        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentMediaId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when (parentMediaId) {
                "root" -> listOf(
                    createCategoryItem("all", "All Media"),
                    createCategoryItem("favourites", "Favourites"),

                )
                "all" -> getMediaItemsFromDbAsGrid()
                "favourites" -> getFavouritesFromDb()
                else -> emptyList()
            }

//            Log.d("MediaSession", "Returning ${items.size} children for parent: $parentMediaId")
//            items.forEach {
//                Log.d("MediaSession", "Item -> id=${it.mediaId}, uri=${it.localConfiguration?.uri}")
//            }

            return Futures.immediateFuture(LibraryResult.ofItemList(items,params))
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            // Perform any logic for subscription, e.g., adding to a list of subscribers or managing the state

            // Return a success result
            val result = LibraryResult.ofVoid(params)  // Using ofVoid to indicate successful subscription
            return Futures.immediateFuture(result)
        }

        override fun onUnsubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String
        ): ListenableFuture<LibraryResult<Void>> {
            // Perform any logic for unsubscription, e.g., removing from a list of subscribers or clearing the state
            // Return a success result
            val result = LibraryResult.ofVoid()  // Using ofVoid to indicate successful unsubscription
            return Futures.immediateFuture(result)
        }


        @SuppressLint("UnsafeOptInUsageError")
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            mediaItems.forEach {
                Log.d("MediaSession", "onSetMedia: Item -> id=${it.mediaId}, uri=${it.localConfiguration?.uri}")
            }

            val currentItems = player?.currentTimeline?.let {
                (0 until it.windowCount).mapNotNull { i -> player?.getMediaItemAt(i) }
            } ?: emptyList()

            val requestedMediaId = mediaItems.firstOrNull()?.mediaId.orEmpty()

            // Attempt to find the matching item in DB
            val item = database.albumsDao().getItemById(requestedMediaId)

            if (item != null) {
                val targetIndex = currentItems.indexOfFirst {
                    it.localConfiguration?.uri.toString() == item.streamUrl
                }.takeIf { it >= 0 } ?: 0

                val currentIndex = player?.currentMediaItemIndex ?: -1
                val currentlyPlayingUri = player?.currentMediaItem?.localConfiguration?.uri?.toString()

                if (currentlyPlayingUri != item.streamUrl || currentIndex != targetIndex) {
                    Log.d("MediaSession", "Seeking to index $targetIndex (was $currentIndex), streamUrl = ${item.streamUrl}")
                    player?.seekTo(targetIndex, startPositionMs)
                } else {
                    Log.d("MediaSession", "Already playing the requested item at correct index; skipping seekTo")
                }

                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(currentItems, targetIndex, startPositionMs)
                )
            }

            // Default fallback
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(currentItems, 0, startPositionMs)
            )
        }







    }

    private fun createCategoryItem(mediaId: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }




    fun getMediaItemsFromDbAsGrid(): List<MediaItem> {
        return database.albumsDao().getAll().map {
            MediaItem.Builder()
                .setMediaId(it.id)
                .setUri(it.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(it.title)
                        .setArtist(it.streamUrl)
                        .setIsBrowsable(false)
                        .setArtworkUri(defaultArtWorkUri)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
    }

    fun getFavouritesFromDb(): List<MediaItem> {
        return database.albumsDao().getAllFavourites().map {
            MediaItem.Builder()
                .setMediaId(it.id)
                .setUri(it.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(it.title)
                        .setArtist(it.streamUrl)
                        .setIsBrowsable(false)
                        .setArtworkUri(defaultArtWorkUri)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
    }


    fun isVideoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val videoExtensions = listOf(
            ".mp4", ".mkv", ".webm", ".ts", ".flv", ".avi", ".mov",
            ".m4v", ".3gp", ".3g2", ".f4v", ".f4p", ".f4a", ".f4b"
        )

        val normalizedUrl = url.lowercase().substringBefore("?") // Strip query params
        return videoExtensions.any { normalizedUrl.endsWith(it) }
    }






    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        database = CarPlayerDatabase.getInstance(this@MyMediaService)
        isRunning = true

        defaultArtWorkUri =
            "android.resource://${packageName}/${R.drawable.default_album_art}".toUri()


//        mediaItems.add(
//            MediaItem.Builder().setUri("http://pewaukee.loginto.me:49000/stream2")
//                .setMimeType(MimeTypes.AUDIO_MPEG)
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setArtworkUri(defaultArtWorkUri)
//                        .setTitle("---")
//                        .setArtist("")
//                        .setDescription("local")
//                        .build()
//                )
//                .build()
//        )
//        mediaItems.add(
//            MediaItem.Builder().setUri("https://stream-dc1.radioparadise.com/aac-320")
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setArtworkUri(defaultArtWorkUri)
//                        .setTitle("---")
//                        .setArtist("")
//                        .setDescription("local")
//                        .build()
//                )
//                .build()
//        )

//        mediaItems.add(MediaItem.Builder().setUri("http://pewaukee.loginto.me:33000/0.ts").build())


        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("CarPlayer")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))


        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)


        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)//ProgressiveMediaSource.Factory(httpDataSourceFactory)
            .build()


        CoroutineScope(Dispatchers.IO).launch {
            database
                .apply {
                    albumsDao().listenAll().collect { items ->
                        withContext(Dispatchers.Main) {

                            if (items.size != player?.mediaItemCount) {
                                Log.d(
                                    "MyMediaService",
                                    "onCreate: new item for play found and run playing function"
                                )

                                val currentUris = player?.currentTimeline?.let {
                                    (0 until it.windowCount).mapNotNull { i -> player?.getMediaItemAt(i)?.localConfiguration?.uri?.toString() }
                                } ?: emptyList()

                                val newUris = items.map { it.streamUrl }

                                if (newUris != currentUris) {

                                    player?.setMediaItems(items.map {
                                        MediaItem.Builder().setUri(it.streamUrl)
                                            .build()
                                    })
                                }
                            }
                        }
                    }
                }
        }

        // player?.setMediaItems(mediaItems)


//        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
//
//        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//            .setAudioAttributes(
//            android.media.AudioAttributes.Builder()
//                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
//                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
//                .build()
//        )
//            .setOnAudioFocusChangeListener { focusChange ->
//                when (focusChange) {
//                    AudioManager.AUDIOFOCUS_GAIN -> {
//                        player?.playWhenReady = true
//                    }
//                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
//                        player?.volume = 0.2f
//                    }
//                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
//                        player?.playWhenReady = false
//                    }
//                }
//            }
//            .build()
//
//        val result = audioManager.requestAudioFocus(focusRequest)
//        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            player?.playWhenReady = true
//        } else {
//            Log.d(TAG, "onCreate: audio focus not granted -> ${result.toString()}")
//        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player?.setAudioAttributes(audioAttributes, true)



        player?.prepare()
        player?.playWhenReady = true




        player?.addListener(object : Player.Listener {

            override fun onMetadata(metadata: Metadata) {
                super.onMetadata(metadata)
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i]
                    if (entry is IcyHeaders) {
                        Log.d(
                            "Metadata",
                            "onMetadata: Header found -> ${entry.url}, name -> ${entry.name}, genre -> ${entry.genre}"
                        )
                    }
                    if (entry is IcyInfo) {
                        Log.d("Metadata", "Metadata entry: $entry")
                        val title = entry.title
                        val url = entry.url

                        Log.d("Metadata", "Title: $title, URL: $url")
                        val (artist, track) = entry.title?.split(" - ").let {
                            it?.getOrNull(0).orEmpty() to it?.getOrNull(1).orEmpty()
                        }

                        if (url == null) {
                            CoroutineScope(Dispatchers.Main).launch {
                                title?.let {

                                    val album = fetchAlbumArt(
                                        artist,
                                        track,
                                        "c62a1d89e34fa71897a4bb4df15e8510"
                                    )
                                    updateMediaItem(
                                        title = track,
                                        artist = artist,
                                        imageUrl = album?.imageUrl ?: defaultArtWorkUri.toString()
                                    )

//                                    updateMediaItem(
//                                        title = title,
//                                        artist = artist,
//                                        imageUrl = defaultArtWorkUri.toString()
//                                    )

                                }
                            }
                        } else {
                            url.let {
                                if (it.endsWith(".jpg") || it.endsWith(".png")) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        updateMediaItem(
                                            title = artist,
                                            artist = artist,
                                            imageUrl = url
                                        )
                                    }
                                    // updateArtwork(it)
                                } else {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        updateMediaItem(
                                            title = track,
                                            artist = artist,
                                            imageUrl = defaultArtWorkUri.toString()
                                        )
                                        // Sometimes url is just a website — needs extra handling
                                        Log.w("Metadata", "StreamUrl is not an image link: $it")
                                    }

                                }
                            }
                        }


                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
               var isVideo = isVideoUrl(mediaItem?.localConfiguration?.uri.toString())
                CoroutineScope(Dispatchers.Main).launch {
                    updateMediaItem(
                        title = mediaItem?.mediaMetadata?.title?.toString() ?: if (isVideo) "" else "Loading...",
                        artist = mediaItem?.mediaMetadata?.artist?.toString() ?: if (isVideo) "" else "Loading...",
                        imageUrl = defaultArtWorkUri.toString()
                    )
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


//        mediaSession = MediaSession.Builder(this, player!!)
//            .build()

        mediaSession = MediaLibrarySession.Builder(this,player!!, sessionCallback)
            .setId("CarPlayerSession")
            .build()





        val intent = Intent().apply {
            Log.d("MediaIntent", "current pkg -> ${MyMediaService::class.java.packageName}")
            component = ComponentName("com.example.carplayer", "com.example.carplayer.MainActivity")
            // flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession?.setSessionActivity(sessionActivityPendingIntent)

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }





    suspend fun updateMediaItem(title: String, artist: String, imageUrl: String) {
        var index = player?.currentMediaItemIndex ?: 0

        Log.d("Update metadata", "updateMediaItem:  uri is -> $imageUrl")

        val safe = isImageSizeSafe(imageUrl)
        val artworkUri = if (safe) imageUrl.toUri() else defaultArtWorkUri

        database.albumsDao().markOnlyOneAsPlaying(
            player?.currentMediaItem?.localConfiguration?.uri?.toString()
                .toString(),
            title = title,
            artist = artist,
            artwork = artworkUri.toString()
        )

//        Log.d("Artwork", "updateMediaItem: url -> $artworkUri")
//        val bitmpa = this.toImageBitmap(artworkUri.toString())
//        Log.d("Artwork", "updateMediaItem: bitmap -> $bitmpa")
//        Log.d("Artwork", "updateMediaItem: array -> ${bitmpa?.toByteArray()}")

        val updatedMediaMetadata =
            player?.currentMediaItem?.mediaMetadata?.buildUpon()
                ?.setArtworkUri(artworkUri)
                //  ?.setArtworkData(this.toImageBitmap(artworkUri.toString())?.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                ?.setTitle(title)
                ?.setArtist(artist)
                ?.setSubtitle(artist)
                ?.build()

        val updatedMediaItem = updatedMediaMetadata?.let {
            player?.currentMediaItem?.buildUpon()
                ?.setMediaMetadata(it)
        }
            ?.build()

        // Log.d("Metadata", " url is -> ${album?.imageUrl}")

        updatedMediaItem?.let { mediaItem ->
            player?.replaceMediaItem(
                index,
                mediaItem
            )


        }

    }

    suspend fun updateMediaItemWithBitmap(title: String, artist: String, imageUrl: String) {
        val bitmap = loadResizedBitmap(applicationContext, imageUrl)

        bitmap?.let {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkData(it.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .setArtworkUri(imageUrl.toUri())
                .build()

            val currentItem = player?.currentMediaItem ?: return
            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(metadata)
                .build()

            val index = player?.currentMediaItemIndex ?: 0
            player?.replaceMediaItem(index, updatedItem)
        }
    }


    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
        isRunning = false
    }


    suspend fun fetchAlbumArt(artist: String, track: String, apiKey: String): TrackAlbumModel? {
        try {
            Log.d("FetchAlbumArt", "artist -> $artist, track -> $track")
            val response = lastFmApi.getTrackInfo(apiKey, artist, track)
            val images = response.track?.album?.image
            // Pick "extralarge" or the biggest one
            var imageUrl = images?.findLast { it.size == "large" || it.size == "mega" }?.url

            var title: String = response.track?.album?.title.toString()
            var artist = response.track?.artist?.name.toString()

            if (imageUrl == null) {
                imageUrl = defaultArtWorkUri.toString()
            }


            //if (imageUrl.isNullOrEmpty()) {
            return TrackAlbumModel(
                id = response.track?.mbid ?: UUID.randomUUID().toString(),
                title = title,
                imageUrl = imageUrl.toString(),
                streamUrl = ""
            )
            // }
//            else {
//                // Resize the image if necessary
//                val resizedImageUrl = imageUrl
//                Log.d("FetchImage", "fetchAlbumArt: resized url -> $resizedImageUrl")
//                // Now return the track information with the possibly resized image URL
//                val title = response.track?.album?.title.toString()
//                val artist = response.track?.artist?.name.toString()
//                return TrackAlbumModel(
//                    id = response.track?.mbid ?: UUID.randomUUID().toString(),
//                    title = title,
//                    artist = artist,
//                    imageUrl = resizedImageUrl.toString(),
//                    streamUrl = ""
//                )
//            }

        } catch (e: Exception) {
            Log.e("LastFm", "Error fetching album art: ${e.message}")
            return TrackAlbumModel(
                id = UUID.randomUUID().toString(),
                title = track,
                imageUrl = defaultArtWorkUri.toString(),
                streamUrl = ""
            )

        }
    }


    suspend fun loadResizedBitmap(context: Context, imageUrl: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(512) // Resize to prevent OOM
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e("MediaService", "Failed to load image: ${e.message}")
            null
        }
    }

    fun Bitmap.toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }


    suspend fun isImageSizeSafe(imageUrl: String, maxBytes: Long = 5_000_000): Boolean {
        return try {
            val request = ImageRequest.Builder(this)
                .data(imageUrl)
                .allowHardware(false) // needed to access bitmap
                .size(512) // safely resize large images
                .build()

            val result = this.imageLoader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap

            val byteSize = bitmap?.allocationByteCount ?: 0
            Log.d("ImageSafety", "Decoded bitmap size = $byteSize bytes")

            byteSize < maxBytes
        } catch (e: Exception) {
            false
        }
    }


    companion object {
        @Volatile
        var isRunning = false
    }

}


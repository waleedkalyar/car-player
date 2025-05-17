package com.example.carplayer.services

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.session.MediaController
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaTabScreen(
    private val carContext: CarContext,
    private val mediaItems: List<TrackAlbumModel>,
    private var mediaController: MediaController?
) {

    private var gridItems: List<GridItem> = listOf()
    private var isLoaded = false

    suspend fun preloadGridItems() {
        val loadedItems = mutableListOf<GridItem>()
        for (album in mediaItems) {
            try {
                val bitmap = loadBitmapFromUrl(album.imageUrl)
                val icon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()

                val item = GridItem.Builder()
                    .setTitle(album.title)
                    .setText(album.artist)
                    .setImage(icon, GridItem.IMAGE_TYPE_LARGE)
                    .setOnClickListener {
                        mediaController?.let { controller ->
                            updateMediaSession(controller, album)
                        }
                    }
                    .build()

                loadedItems.add(item)

            } catch (e: Exception) {
                Log.w("MediaTabScreen", "Image load failed: ${album.title}")
            }
        }
        gridItems = loadedItems
        isLoaded = true
    }

    fun onGetTemplate(): Template {
        if (!isLoaded) {
            CoroutineScope(Dispatchers.Main).launch {
                preloadGridItems()
            }

            return MessageTemplate.Builder("Loading...")
                .setTitle("Please wait")
                .build()
        }

        val itemList = ItemList.Builder().apply {
            gridItems.forEach { addItem(it) }
        }.build()

        return GridTemplate.Builder()
            .setTitle("Media")
            .setSingleList(itemList)
            .build()
    }

    private suspend fun loadBitmapFromUrl(url: String): Bitmap = withContext(Dispatchers.IO) {
        val loader = ImageLoader(carContext)
        val request = ImageRequest.Builder(carContext)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        (result.drawable as BitmapDrawable).bitmap
    }

    fun updateMediaController(controller: MediaController) {
        this.mediaController = controller
    }

    private fun updateMediaSession(controller: MediaController, album: TrackAlbumModel) {
        val index = controller.currentTimeline.let { timeline ->
            (0 until timeline.windowCount).firstOrNull { i ->
                controller.getMediaItemAt(i).localConfiguration?.uri.toString() == album.streamUrl
            }
        }

        if (index != null) {
            controller.seekTo(index, 0L)
        } else {
            Log.w("MediaTabScreen", "Stream URL not found.")
        }
    }
}

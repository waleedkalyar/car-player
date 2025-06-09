import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.session.MediaController
import com.example.carplayer.R
import com.example.carplayer.shared.database.AlbumsDao
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MediaBrowseScreen : Screen {

    // It can initially be null
    private val albumsDao: AlbumsDao
    private var mediaController: MediaController?


    constructor(
        carContext: CarContext,
        albumsDao: AlbumsDao,
        mediaController: androidx.media3.session.MediaController?
    ) : super(carContext) {
        this.albumsDao = albumsDao
        this.mediaController = mediaController
        this.allItems = listOf<TrackAlbumModel>()
        this.favouriteItems = listOf<TrackAlbumModel>()
        CoroutineScope(Dispatchers.Main).launch {
            allItems = albumsDao.listenAll().first()
            favouriteItems = albumsDao.listenAllFavourites().first()
            invalidate() // Refresh the template after data is loaded
        }
    }

    private var allItems: List<TrackAlbumModel>
    private var favouriteItems: List<TrackAlbumModel>
    private var currentTab = "ALL"


    // Method to update the media controller when it becomes available
    fun updateMediaController(controller: androidx.media3.session.MediaController) {
        this.mediaController = controller
        // You can now perform actions with the controller (e.g., update UI, etc.)
        // You might also want to update the media list, metadata, or playback state.
        // For example, trigger an update to reflect the media that's playing
    }



    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder().setTitle("All").setOnClickListener {
                    currentTab = "ALL"
                    invalidate()
                }.build()
            )
//            .addAction(
//                Action.Builder().setTitle("Videos").setOnClickListener {
//                    currentTab = "VIDEOS"
//                    invalidate()
//                }.build()
//            )
//            .addAction(
//                Action.Builder().setTitle("Audios").setOnClickListener {
//                    currentTab = "AUDIOS"
//                    invalidate()
//                }.build()
//            )
            .build()


        val mediaItems = getMediaItemsForTab()
        val itemList = createGridItemList(mediaItems)

        return GridTemplate.Builder()
            .setTitle("Browse Media")

            .setActionStrip(actionStrip)
            .setSingleList(itemList)
            .build()
    }

    private fun getMediaItemsForTab(): List<TrackAlbumModel> {
        return when (currentTab) {
            "Favourites" -> favouriteItems
            else -> allItems
        }
    }


    private fun createGridItemList(mediaItems: List<TrackAlbumModel>): ItemList {
        val builder = ItemList.Builder()
        val bitmap = BitmapFactory.decodeResource(carContext.resources, R.drawable.default_album_art)
        val iconCompat = IconCompat.createWithBitmap(bitmap)
        val carIcon = CarIcon.Builder(iconCompat).build()
        for (album in mediaItems) {

            val gridItem = GridItem.Builder()
                .setTitle("CH-${album.channelNumber} ${album.title}")
                .setText(album.streamUrl)
                .setImage(
                    carIcon,
                    GridItem.IMAGE_TYPE_LARGE
                )
                .setOnClickListener {
                    mediaController?.let {
                        updateMediaSession(controller = it, album)
                    }
                }
                .build()

            builder.addItem(gridItem)
        }

        return builder.build()
    }


    private fun updateMediaSession(controller: MediaController, album: TrackAlbumModel) {
        // Update the media session's metadata when a media item is selected

        // Update the playback state to reflect the new media
        val index = controller.currentTimeline
            .let { timeline ->
                (0 until timeline.windowCount).firstOrNull { i ->
                    controller.getMediaItemAt(i).localConfiguration?.uri.toString() == album.streamUrl
                }
            }
        Log.d("AlbumListDialogFragment", "initViewPager: update received play now -> $index")
        if (index != null) {
            controller.seekTo(index, 0L)
        } else {
            Log.w("ExoPlayer", "URL not found in media items.")
        }
    }
}

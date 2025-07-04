package com.example.carplayer.main

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.example.carplayer.R
import com.example.carplayer.browser.BrowserActivity
import com.example.carplayer.databinding.ActivityMainBinding
import com.example.carplayer.databinding.CustomPlayerControllerBinding
import com.example.carplayer.dialogs.AcknowledgmentDialogFragment
import com.example.carplayer.dialogs.AlbumListDialogFragment
import com.example.carplayer.shared.services.MyMediaService
import com.example.carplayer.utils.launchTrackInMusicApp
import com.google.common.util.concurrent.ListenableFuture
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.example.carplayer.shared.database.CarPlayerDatabase


class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!
    private var mediaController: MediaController? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private val mainViewModel: MainViewModel by viewModels()

    private val bluetoothPermissions = mutableListOf(
        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
    )


    val serviceIntent by lazy { Intent(this, MyMediaService::class.java) }

    // Add new permissions for Android 12+ and location
    @SuppressLint("InlinedApi")
    private val android12PlusPermissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("Bluetooth", "Bluetooth enabled by user")
            Toast.makeText(
                this,
                "Bluetooth is enabled, connect media with your car",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            Log.e("Bluetooth", "Bluetooth not enabled by user")
        }
    }


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hasAccepted = prefs.getBoolean("acknowledgment_accepted", false)
        if (!hasAccepted) {
            Log.d("MainHome", "onCreate: fragment is created and shown")
            AcknowledgmentDialogFragment {
                  prefs.edit { putBoolean("acknowledgment_accepted", true) }
                initViews()
                lifecycleScope.launch {
                    delay(2500L)
                    checkAndRequestBluetoothPermissions()
                }
            }.show(supportFragmentManager, "AckDialog")
        }else {
            initViews()
            lifecycleScope.launch {
                delay(2500L)
                checkAndRequestBluetoothPermissions()
            }
        }


    }

    override fun onResume() {
        super.onResume()
        if (!MyMediaService.Companion.isRunning && CarPlayerDatabase.getInstance(this).albumsDao().getAll().isNotEmpty()) {
            startForegroundService(serviceIntent)
        }
    }



    fun initViews() {
        // Start MediaService if not already running
        if (!MyMediaService.Companion.isRunning && CarPlayerDatabase.getInstance(this).albumsDao().getAll().isNotEmpty()) {

            startForegroundService(serviceIntent)
        }

        // Connect to MediaSession

        mainViewModel.lastMetadata?.let { handleMetadata(it) }

        connectToMediaSession()
        updateImageViewVisibilityBasedOnWidth()

        initClickListeners()

    }


    private fun initClickListeners() = with(binding) {
        btnAddUrl.setOnClickListener {
//            val bottomSheet = AddUrlBottomSheet { url ->
//                // Save or handle the URL here
//                //Toast.makeText(this, "URL Saved: $url", Toast.LENGTH_SHORT).show()
//            }
//            bottomSheet.show(supportFragmentManager, "AddUrlBottomSheet")
            AlbumListDialogFragment().apply {
                show(supportFragmentManager, "albums")
            }
        }


//        btnBrowser.setOnClickListener {
//            Intent(this@MainActivity, WebViewPhoneActivity::class.java).apply {
//                startActivity(this)
//            }
//
//        }

    }


    private fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions.addAll(android12PlusPermissions)
        } else {
            bluetoothPermissions.addAll(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        val permissionsToRequest = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE
            )
        }
    }


    // Optional: Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("Permissions", "All permissions granted.")
                enableBluetoothAndConnectToCar()
            } else {
                Log.e("Permissions", "Some permissions were denied.")
                Toast.makeText(
                    this@MainActivity,
                    "Permissions are required to play with Car Player",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                // Optionally show dialog or disable functionality
            }
        }
    }


    fun enableBluetoothAndConnectToCar() {
        val adapter = getBluetoothAdapter(this)
        adapter?.let { bluetoothAdapter ->
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
        }
    }

    fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        val bluetoothManager =
            context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter
    }


    @OptIn(UnstableApi::class)
    private fun connectToMediaSession() = with(binding) {

        // playerView.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FIT

        ContextCompat.getDrawable(this@MainActivity, R.drawable.default_album_art)
            ?.let { showAndSetArtOnVisibilityBase(artwork = it) }


        val sessionToken = SessionToken(
            this@MainActivity, ComponentName(this@MainActivity, MyMediaService::class.java)
        )

        mediaControllerFuture =
            MediaController.Builder(this@MainActivity, sessionToken).buildAsync()




        mediaControllerFuture?.addListener({
            try {
                val controller = mediaControllerFuture?.get()
                mediaController = controller
                playerView.player = controller

                updateDurationDisplay()

                controller?.mediaMetadata?.let { metadata ->
                    if (!metadata.title.isNullOrEmpty()) {
                        mainViewModel.lastMetadata = metadata
                        handleMetadata(metadata) // <- use your update UI code here
                        updateControllerOnMediaTrack(controller.currentTracks)
                    }
                }

                // call this to load meta data for first item.. after that it will update from on metadata changed


                controller?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            updateDurationDisplay()  // Safe time to check live
                        }
                        when (state) {
                            Player.STATE_BUFFERING -> loadingSpinner.visibility = View.VISIBLE
                            Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> loadingSpinner.visibility =
                                View.GONE
                        }
                    }


                    @SuppressLint("UseKtx")
                    override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                        mainViewModel.lastMetadata = metadata
                        handleMetadata(metadata)
                    }


                    override fun onTracksChanged(tracks: Tracks) {
                        super.onTracksChanged(tracks)
                        updateControllerOnMediaTrack(tracks)


                    }


                })


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))
    }

    @OptIn(UnstableApi::class)
    private fun updateControllerOnMediaTrack(tracks: Tracks) = with(binding) {
        var hasVideo = hasVideoTrack(tracks)
        var hasAudio = hasAudioTrack(tracks)

        val titleText = findViewById<TextView>(R.id.track_title)
        val artistText = findViewById<TextView>(R.id.track_artist)

        Log.d("TRACKS", "Video: $hasVideo, Audio: $hasAudio")

        mainViewModel.isCurrentVideo = hasVideo
        updateImageViewVisibilityBasedOnWidth()

        // You can now show/hide views based on what's active:
        if (hasVideo) {
            // Show video surface (PlayerView will do this by default)
            playerView.controllerShowTimeoutMs = 3000 // e.g., 3 seconds
            playerView.controllerAutoShow = true
            playerView.controllerHideOnTouch = true
            playerView.videoSurfaceView?.visibility = View.VISIBLE
            titleText.visibility = View.INVISIBLE
            artistText.visibility = View.INVISIBLE
        } else if (hasAudio) {
            // Maybe show album art, metadata, etc.
            playerView.controllerShowTimeoutMs = 0
            playerView.showController()
            playerView.controllerHideOnTouch = false
            playerView.videoSurfaceView?.visibility = View.INVISIBLE
            playerView.setBackgroundColor(Color.TRANSPARENT)
            playerView.setShutterBackgroundColor(Color.TRANSPARENT)
            imgBackground.visibility = View.VISIBLE
            titleText.visibility = View.VISIBLE
            artistText.visibility = View.VISIBLE
            // playerView.defaultArtwork = Color.TRANSPARENT.toDrawable()
        }
    }

    private fun hasVideoTrack(tracks: Tracks): Boolean {
        var hasVideo = false
        var hasAudio = false

        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val format = group.getTrackFormat(i)
                    when {
                        format.sampleMimeType?.startsWith("video") == true -> hasVideo =
                            true

                        format.sampleMimeType?.startsWith("audio") == true -> hasAudio =
                            true
                    }
                }
            }
        }

        return hasVideo
    }

    private fun hasAudioTrack(tracks: Tracks): Boolean {
        var hasVideo = false
        var hasAudio = false

        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val format = group.getTrackFormat(i)
                    when {
                        format.sampleMimeType?.startsWith("video") == true -> hasVideo =
                            true

                        format.sampleMimeType?.startsWith("audio") == true -> hasAudio =
                            true
                    }
                }
            }
        }

        return hasAudio
    }


    @OptIn(UnstableApi::class)
    private fun handleMetadata(metadata: MediaMetadata) = with(binding) {
        val title = metadata.title
        val artist = metadata.artist
        val artworkUri = metadata.artworkUri


        btnMusicLauncher.setOnClickListener {
            if (title == null || artist == null) return@setOnClickListener
            this@MainActivity.launchTrackInMusicApp(
                artist = artist.toString(),
                track = title.toString()
            )
        }


        val titleText = findViewById<TextView>(R.id.track_title)
        val artistText = findViewById<TextView>(R.id.track_artist)

        if (title?.contains(" - ") == false) {
            titleText.text = title
            artistText.text = artist
        }


        if (metadata.artworkUri != null && metadata.artworkUri.toString()
                .isNotEmpty()
        ) {
            lifecycleScope.launch {
                runCatching {
                    val request = ImageRequest.Builder(this@MainActivity)
                        .data(metadata.artworkUri)
                        .allowHardware(false) // Required to get Bitmap for Palette
                        .build()

                    val result = this@MainActivity.imageLoader.execute(request)
                    val drawable = result.drawable

                    if (drawable != null) {
                        albumImage.setImageDrawable(drawable)
                        // Extract dominant color from bitmap using Palette
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        Palette.from(bitmap).generate { palette ->
                            val dominantColor =
                                palette?.getDominantColor(Color.BLACK)
                                    ?: Color.BLACK
                            imgBackground.setBackgroundColor(dominantColor)
                            albumImage.setBackgroundColor(Color.TRANSPARENT)

                            Blurry.with(this@MainActivity).radius(25).sampling(4)
                                .from(bitmap).into(imgBackground)

                            showAndSetArtOnVisibilityBase(artwork = drawable)
                        }
                    } else {
                        setDefaultArtworkAndBackground()
                    }
                }.onFailure {
                    setDefaultArtworkAndBackground()
                }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        mediaController?.release()
        stopService(serviceIntent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateImageViewVisibilityBasedOnWidth()
    }


    @OptIn(UnstableApi::class)
    private fun updateImageViewVisibilityBasedOnWidth() = with(binding) {
        val displayMetrics = resources.displayMetrics

        // Calculate width in dp (density-independent pixels)
        val widthDp = displayMetrics.widthPixels / displayMetrics.density


        // Show ImageView only if width is 720dp or more
        if (widthDp >= 720 && !mainViewModel.isCurrentVideo) {
            albumImage.visibility = View.VISIBLE
            playerView.setBackgroundColor(Color.TRANSPARENT)
            // playerView.videoSurfaceView?.visibility = View.INVISIBLE
            playerView.setShutterBackgroundColor(Color.TRANSPARENT)


        } else {
            // playerView.setBackgroundColor(Color.BLACK)
            // playerView.videoSurfaceView?.visibility = View.VISIBLE
            // playerView.setShutterBackgroundColor(Color.BLACK)
            albumImage.visibility = View.GONE
            imgBackground.visibility = View.INVISIBLE
        }
    }

    @UnstableApi
    private fun showAndSetArtOnVisibilityBase(artwork: Drawable) = with(binding) {
        val displayMetrics = resources.displayMetrics

        // Calculate width in dp (density-independent pixels)
        val widthDp = displayMetrics.widthPixels / displayMetrics.density

        if (widthDp >= 720 && !mainViewModel.isCurrentVideo) {
            playerView.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_OFF
            playerView.defaultArtwork = Color.TRANSPARENT.toDrawable()
        } else {
            playerView.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FIT
            playerView.defaultArtwork = artwork

        }

    }


    @OptIn(UnstableApi::class)
    private fun setDefaultArtworkAndBackground() = with(binding) {
        val context = playerView.context
        val placeholder = ContextCompat.getDrawable(context, R.drawable.default_album_art)
        //playerView.defaultArtwork = placeholder
        albumImage.setImageDrawable(placeholder)
        imgBackground.setBackgroundColor(Color.BLACK)
        //playerView.setBackgroundColor(Color.BLACK)
        Blurry.with(this@MainActivity).radius(25).sampling(4).from(placeholder?.toBitmap())
            .into(imgBackground)
    }


    @OptIn(UnstableApi::class)
    private fun updateDurationDisplay() {
        val durationTextView = findViewById<TextView>(R.id.exo_duration1)
        val player = mediaController ?: return

        val isLive = player.isCurrentMediaItemLive

        if (isLive) {
            durationTextView.text = "LIVE"
            durationTextView.setTextColor(Color.WHITE)
            durationTextView.background =
                ContextCompat.getDrawable(this, R.drawable.live_background)
            durationTextView.setPadding(12, 4, 12, 4) // Optional: Add some padding
        } else {
            val durationMs = player.duration
            durationTextView.text = androidx.media3.common.util.Util.getStringForTime(
                StringBuilder(), java.util.Formatter(), durationMs
            )
            durationTextView.setTextColor(Color.WHITE)
            durationTextView.background = null
            durationTextView.setPadding(0, 0, 0, 0)
        }
    }


}
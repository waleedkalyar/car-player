package com.example.carplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build

import androidx.media3.common.MediaItem
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.example.carplayer.services.MyMediaService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isInvisible
import com.example.carplayer.databinding.ActivityMainBinding
import com.example.carplayer.utils.toBitmap
import jp.wasabeef.blurry.Blurry

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    val binding get() = _binding!!
    private var mediaController: MediaController? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private var isCurrentVideo = false


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

        initViews()

    }

    fun initViews() {
        // Start MediaService if not already running
        val serviceIntent = Intent(this, MyMediaService::class.java)
        startForegroundService(serviceIntent)

        // Connect to MediaSession
        connectToMediaSession()
        updateImageViewVisibilityBasedOnWidth()

        initClickListeners()

    }

    private fun initClickListeners() {

    }


    @OptIn(UnstableApi::class)
    private fun connectToMediaSession() = with(binding) {

        // playerView.defaultArtwork = ContextCompat.getDrawable(this@MainActivity, R.drawable.default_album_art)

        // playerView.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FIT


        val sessionToken = SessionToken(
            this@MainActivity,
            ComponentName(this@MainActivity, MyMediaService::class.java)
        )

        mediaControllerFuture = MediaController.Builder(this@MainActivity, sessionToken)
            .buildAsync()


        val titleText = findViewById<TextView>(R.id.track_title)
        val artistText = findViewById<TextView>(R.id.track_artist)

        mediaControllerFuture?.addListener({
            try {
                val controller = mediaControllerFuture?.get()
                mediaController = controller
                playerView.player = controller


                controller?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> loadingSpinner.visibility = View.VISIBLE
                            Player.STATE_READY,
                            Player.STATE_ENDED,
                            Player.STATE_IDLE -> loadingSpinner.visibility = View.GONE
                        }
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        // loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }

                    @SuppressLint("UseKtx")
                    override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                        val title = metadata.title ?: "Unknown Title"
                        val artist = metadata.artist ?: "Unknown Artist"
                        titleText.text = title
                        artistText.text = artist

                        val artworkUri = metadata.artworkUri

                        Log.d("Meta", "onMediaMetadataChanged: artwork url -> $artworkUri ")

                        if (artworkUri != null) {
                            val context = playerView.context

                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val request = ImageRequest.Builder(context)
                                        .data(artworkUri)
                                        .allowHardware(false) // Required to get Bitmap for Palette
                                        .build()

                                    val result = context.imageLoader.execute(request)
                                    val drawable = result.drawable

                                    if (drawable != null) {
                                        // Set artwork
                                        // playerView.defaultArtwork = drawable

                                        albumImage.setImageDrawable(drawable)

                                        // Extract dominant color from bitmap using Palette
                                        val bitmap = (drawable as BitmapDrawable).bitmap
                                        Palette.from(bitmap).generate { palette ->
                                            val dominantColor =
                                                palette?.getDominantColor(Color.BLACK)
                                                    ?: Color.BLACK
                                            imgBackground.setBackgroundColor(dominantColor)
                                            albumImage.setBackgroundColor(Color.TRANSPARENT)

                                            Blurry.with(this@MainActivity).radius(25).sampling(4).from(bitmap).into(imgBackground)
                                        }


                                    } else {
                                        setDefaultArtworkAndBackground()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ArtworkLoad", "Failed to load artwork", e)
                                    setDefaultArtworkAndBackground()
                                }
                            }
                        } else {
                            setDefaultArtworkAndBackground()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
//                        binding.playPauseButton.setImageResource(
//                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
//                        )
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        super.onTracksChanged(tracks)
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

                        Log.d("TRACKS", "Video: $hasVideo, Audio: $hasAudio")

                        isCurrentVideo = hasVideo
                        updateImageViewVisibilityBasedOnWidth()

                        // You can now show/hide views based on what's active:
                        if (hasVideo) {
                            // Show video surface (PlayerView will do this by default)
                            playerView.controllerShowTimeoutMs = 3000 // e.g., 3 seconds
                            playerView.controllerAutoShow = true
                            playerView.controllerHideOnTouch = true
                            playerView.videoSurfaceView?.visibility = View.VISIBLE
                        } else if (hasAudio) {
                            // Maybe show album art, metadata, etc.
                            playerView.controllerShowTimeoutMs = 0
                            playerView.showController()
                            playerView.controllerHideOnTouch = false
                            playerView.videoSurfaceView?.visibility = View.INVISIBLE
                            playerView.setBackgroundColor(Color.TRANSPARENT)
                            playerView.setShutterBackgroundColor(Color.TRANSPARENT)
                            playerView.defaultArtwork = Color.TRANSPARENT.toDrawable()
                        }
                    }

                })


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.release()
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
        if (widthDp >= 720 && !isCurrentVideo) {
            albumImage.visibility = View.VISIBLE
            playerView.setBackgroundColor(Color.TRANSPARENT)
            // playerView.videoSurfaceView?.visibility = View.INVISIBLE
            playerView.setShutterBackgroundColor(Color.TRANSPARENT)


        } else {
            // playerView.setBackgroundColor(Color.BLACK)
            // playerView.videoSurfaceView?.visibility = View.VISIBLE
            // playerView.setShutterBackgroundColor(Color.BLACK)
            albumImage.visibility = View.GONE
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
        Blurry.with(this@MainActivity).radius(25).sampling(4).from(placeholder?.toBitmap()).into(imgBackground)
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun Bitmap.blur(context: Context, radius: Float): Bitmap {
        val renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)

        val renderNode = RenderNode("blur_node").apply {
            setPosition(0, 0, width, height)
            val canvas = beginRecording()
            canvas.drawBitmap(this@blur, 0f, 0f, null)
            endRecording()
            setRenderEffect(renderEffect)
        }

        val output = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)

        val hardwareRenderer = HardwareRenderer().apply {
            setContentRoot(renderNode)
            setSurface(Surface(SurfaceTexture(false).apply {
                setDefaultBufferSize(width, height)
                detachFromGLContext() // Detach from GL as we only want to render to bitmap
            }))
            setLightSourceAlpha(0.5f, 0.5f)
            setOpaque(false)
        }

        // Render the node into the bitmap
        val syncResult = hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        // Now draw the RenderNode's output into a Bitmap using PixelCopy (requires real SurfaceView or View)
        // But since that's complex for offscreen, we fall back to not doing it directly here.
        // Simplest alternative is to just use Paint.setRenderEffect(), or use View.draw() in a real UI.

        hardwareRenderer.destroy() // Clean up

        // Alternative simple blur if not showing in UI: fallback (paint-based blur)
        val fallback = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
        val fallbackCanvas = Canvas(fallback)
        // val paint = Paint().apply { setRenderEffect(renderEffect) }
        // fallbackCanvas.drawBitmap(this, 0f, 0f, paint)
        return fallback
    }


}
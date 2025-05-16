//package com.example.carplayer.auto;
//
//import android.annotation.SuppressLint;
//import android.content.ComponentName;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.Color;
//import android.graphics.drawable.BitmapDrawable;
//import android.graphics.drawable.ColorDrawable;
//import android.graphics.drawable.Drawable;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.annotation.OptIn;
//import androidx.core.content.ContextCompat;
//import androidx.lifecycle.ViewModelProvider;
//import androidx.lifecycle.ViewModelStore;
//import androidx.lifecycle.ViewModelStoreOwner;
//import androidx.media3.common.Format;
//import androidx.media3.common.MediaMetadata;
//import androidx.media3.common.Player;
//import androidx.media3.common.Tracks;
//import androidx.media3.common.util.UnstableApi;
//import androidx.media3.session.MediaController;
//import androidx.media3.session.SessionToken;
//import androidx.media3.ui.PlayerView;
//import androidx.palette.graphics.Palette;
//
//import com.example.carplayer.R;
//import com.example.carplayer.main.MainViewModel;
//import com.example.carplayer.shared.services.MyMediaService;
//import com.google.android.apps.auto.sdk.CarActivity;
//import com.google.common.util.concurrent.ListenableFuture;
//
//import java.util.Formatter;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import coil.request.ImageRequest;
//import coil.target.Target;
//import jp.wasabeef.blurry.Blurry;
//
//public class MainAutoActivity extends CarActivity implements ViewModelStoreOwner {
//
//    private MediaController mediaController;
//
//    PlayerView playerView;
//    ProgressBar loadingSpinner;
//
//    ImageView albumImage,backgroundImage;
//
//    private MainViewModel mainViewModel;
//
//    private final ViewModelStore viewModelStore = new ViewModelStore();
//
//
//    @Override
//    public void onCreate(Bundle bundle) {
//        super.onCreate(bundle);
//        setContentView(R.layout.activity_main);
//        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
//        initIds();
//        connectToMediaSession();
//    }
//
//    private void initIds() {
//        playerView = (PlayerView) findViewById(R.id.player_view);
//        loadingSpinner = (ProgressBar) findViewById(R.id.loading_spinner);
//        albumImage = (ImageView) findViewById(R.id.albumImage);
//        backgroundImage = (ImageView) findViewById(R.id.imgBackground);
//    }
//
//
//    @SuppressLint("NewApi")
//    @OptIn(markerClass = UnstableApi.class)
//    private void connectToMediaSession() {
//        Drawable defaultArt = ContextCompat.getDrawable(this, R.drawable.default_album_art);
//        if (defaultArt != null) {
//            showAndSetArtOnVisibilityBase(defaultArt);
//        }
//
//        SessionToken sessionToken = new SessionToken(
//                this, new ComponentName(this, MyMediaService.class)
//        );
//
//        ListenableFuture<MediaController> mediaControllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
//
//        mediaControllerFuture.addListener(() -> {
//            try {
//                MediaController controller = mediaControllerFuture.get();
//                mediaController = controller;
//                playerView.setPlayer(controller);
//
//                updateDurationDisplay();
//
//                MediaMetadata metadata = controller.getMediaMetadata();
//                if (metadata.title != null && !metadata.title.isEmpty()) {
//                   // mainViewModel.setLastMetadata(metadata);
//                    handleMetadata(metadata);
//                    updateControllerOnMediaTrack(controller.getCurrentTracks());
//                }
//
//                controller.addListener(new Player.Listener() {
//                    @Override
//                    public void onPlaybackStateChanged(int state) {
//                        if (state == Player.STATE_READY) {
//                            updateDurationDisplay();
//                        }
//                        switch (state) {
//                            case Player.STATE_BUFFERING:
//                                loadingSpinner.setVisibility(View.VISIBLE);
//                                break;
//                            case Player.STATE_READY:
//                            case Player.STATE_ENDED:
//                            case Player.STATE_IDLE:
//                                loadingSpinner.setVisibility(View.GONE);
//                                break;
//                        }
//                    }
//
//                    @Override
//                    public void onMediaMetadataChanged(@NonNull MediaMetadata metadata) {
//                        mainViewModel.setLastMetadata(metadata);
//                        handleMetadata(metadata);
//                    }
//
//                    @Override
//                    public void onTracksChanged(@NonNull Tracks tracks) {
//                        updateControllerOnMediaTrack(tracks);
//                    }
//                });
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//
//
//    private void handleMetadata(MediaMetadata metadata) {
//        TextView titleText = (TextView) findViewById(R.id.track_title);
//        TextView artistText =  (TextView) findViewById(R.id.track_artist);
//        ImageView albumImage = (ImageView) findViewById(R.id.albumImage); // replace with actual ID
//        View imgBackground = findViewById(R.id.imgBackground); // replace with actual ID
//
//        CharSequence title = metadata.title;
//        CharSequence artist = metadata.artist;
//
//        if (title != null && !title.toString().contains(" - ")) {
//            titleText.setText(title);
//            artistText.setText(artist);
//        }
//
//        Uri artworkUri = metadata.artworkUri;
//
//        if (artworkUri != null && !artworkUri.toString().isEmpty()) {
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            Handler mainHandler = new Handler(Looper.getMainLooper());
//
//            executor.execute(() -> {
//                try {
//                    ImageRequest request = new ImageRequest.Builder(this)
//                            .data(artworkUri)
//                            .allowHardware(false)
//                            .target(new Target() {
//                                @Override
//                                public void onError(@Nullable Drawable error) {
//                                    Target.super.onError(error);
//                                    mainHandler.post(() -> setDefaultArtworkAndBackground());
//                                }
//
//
//
//                                @Override
//                                public void onStart(@Nullable Drawable placeholder) {
//                                    Target.super.onStart(placeholder);
//                                }
//
//                                @OptIn(markerClass = UnstableApi.class)
//                                @Override
//                                public void onSuccess(@NonNull Drawable result) {
//                                    Target.super.onSuccess(result);
//
//                                    mainHandler.post(() -> {
//                                        albumImage.setImageDrawable(result);
//
//                                        Bitmap bitmap = ((BitmapDrawable) result).getBitmap();
//
//                                        Palette.from(bitmap).generate(palette -> {
//                                            int dominantColor = Color.BLACK;
//                                            if (palette != null) {
//                                                dominantColor = palette.getDominantColor(Color.BLACK);
//                                            }
//
//                                            imgBackground.setBackgroundColor(dominantColor);
//                                            albumImage.setBackgroundColor(Color.TRANSPARENT);
//
//                                            Blurry.with(MainAutoActivity.this)
//                                                    .radius(25)
//                                                    .sampling(4)
//                                                    .from(bitmap)
//                                                    .into((ImageView) imgBackground);
//
//                                            showAndSetArtOnVisibilityBase(result); // You must implement this
//                                        });
//                                    });
//                                }
//                            })
//                            .build();
//
//
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    mainHandler.post(this::setDefaultArtworkAndBackground);
//                }
//            });
//        }
//    }
//
//
//    @OptIn(markerClass = UnstableApi.class)
//    private void setDefaultArtworkAndBackground() {
//        Context context = playerView.getContext();
//        Drawable placeholder = ContextCompat.getDrawable(context, R.drawable.default_album_art);
//
//        // playerView.setDefaultArtwork(placeholder);
//        albumImage.setImageDrawable(placeholder);
//        backgroundImage.setBackgroundColor(Color.BLACK);
//        // playerView.setBackgroundColor(Color.BLACK);
//
//        if (placeholder instanceof BitmapDrawable) {
//            BitmapDrawable bitmapDrawable = (BitmapDrawable) placeholder;
//            Bitmap bitmap = bitmapDrawable.getBitmap();
//
//            Blurry.with(MainAutoActivity.this)
//                    .radius(25)
//                    .sampling(4)
//                    .from(bitmap)
//                    .into(backgroundImage);
//        }
//    }
//
//
//    @UnstableApi
//    private void showAndSetArtOnVisibilityBase(Drawable artwork) {
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//
//        // Calculate width in dp
//        float widthDp = displayMetrics.widthPixels / displayMetrics.density;
//
//        if (widthDp >= 720 && !mainViewModel.isCurrentVideo()) {
//            playerView.setArtworkDisplayMode(PlayerView.ARTWORK_DISPLAY_MODE_OFF);
//            playerView.setDefaultArtwork(new ColorDrawable(Color.TRANSPARENT));
//        } else {
//            playerView.setArtworkDisplayMode(PlayerView.ARTWORK_DISPLAY_MODE_FIT);
//            playerView.setDefaultArtwork(artwork);
//        }
//    }
//
//    @OptIn(markerClass = UnstableApi.class)
//    private void updateControllerOnMediaTrack(Tracks tracks) {
//        boolean hasVideo = hasVideoTrack(tracks);
//        boolean hasAudio = hasAudioTrack(tracks);
//
//        TextView titleText = (TextView) findViewById(R.id.track_title);
//        TextView artistText = (TextView) findViewById(R.id.track_artist);
//
//        Log.d("TRACKS", "Video: " + hasVideo + ", Audio: " + hasAudio);
//
//        mainViewModel.setCurrentVideo(hasVideo);
//        updateImageViewVisibilityBasedOnWidth();
//
//        if (hasVideo) {
//            // Show video surface (PlayerView will do this by default)
//            playerView.setControllerShowTimeoutMs(3000); // 3 seconds
//            playerView.setControllerAutoShow(true);
//            playerView.setControllerHideOnTouch(true);
//            if (playerView.getVideoSurfaceView() != null) {
//                playerView.getVideoSurfaceView().setVisibility(View.VISIBLE);
//            }
//            titleText.setVisibility(View.INVISIBLE);
//            artistText.setVisibility(View.INVISIBLE);
//        } else if (hasAudio) {
//            // Show album art, metadata, etc.
//            playerView.setControllerShowTimeoutMs(0);
//            playerView.showController();
//            playerView.setControllerHideOnTouch(false);
//            if (playerView.getVideoSurfaceView() != null) {
//                playerView.getVideoSurfaceView().setVisibility(View.INVISIBLE);
//            }
//            playerView.setBackgroundColor(Color.TRANSPARENT);
//            playerView.setShutterBackgroundColor(Color.TRANSPARENT);
//            backgroundImage.setVisibility(View.VISIBLE);
//            titleText.setVisibility(View.VISIBLE);
//            artistText.setVisibility(View.VISIBLE);
//            // playerView.setDefaultArtwork(new ColorDrawable(Color.TRANSPARENT));
//        }
//    }
//
//    private boolean hasVideoTrack(Tracks tracks) {
//        boolean hasVideo = false;
//
//        for (Tracks.Group group : tracks.getGroups()) {
//            for (int i = 0; i < group.length; i++) {
//                if (group.isTrackSelected(i)) {
//                    Format format = group.getTrackFormat(i);
//                    if (format.sampleMimeType != null && format.sampleMimeType.startsWith("video")) {
//                        hasVideo = true;
//                    }
//                }
//            }
//        }
//
//        return hasVideo;
//    }
//
//    private boolean hasAudioTrack(Tracks tracks) {
//        boolean hasAudio = false;
//
//        for (Tracks.Group group : tracks.getGroups()) {
//            for (int i = 0; i < group.length; i++) {
//                if (group.isTrackSelected(i)) {
//                    Format format = group.getTrackFormat(i);
//                    if (format.sampleMimeType != null && format.sampleMimeType.startsWith("audio")) {
//                        hasAudio = true;
//                    }
//                }
//            }
//        }
//
//        return hasAudio;
//    }
//
//
//    @OptIn(markerClass = UnstableApi.class)
//    private void updateImageViewVisibilityBasedOnWidth() {
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//
//        // Calculate width in dp (density-independent pixels)
//        float widthDp = displayMetrics.widthPixels / displayMetrics.density;
//
//        // Show ImageView only if width is 720dp or more and not a video
//        if (widthDp >= 720 && !mainViewModel.isCurrentVideo()) {
//            albumImage.setVisibility(View.VISIBLE);
//            playerView.setBackgroundColor(Color.TRANSPARENT);
//            playerView.setShutterBackgroundColor(Color.TRANSPARENT);
//            // if (playerView.getVideoSurfaceView() != null)
//            //     playerView.getVideoSurfaceView().setVisibility(View.INVISIBLE);
//        } else {
//            albumImage.setVisibility(View.GONE);
//            backgroundImage.setVisibility(View.INVISIBLE);
//            // playerView.setBackgroundColor(Color.BLACK);
//            // if (playerView.getVideoSurfaceView() != null)
//            //     playerView.getVideoSurfaceView().setVisibility(View.VISIBLE);
//            // playerView.setShutterBackgroundColor(Color.BLACK);
//        }
//    }
//
//
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        mediaController.release();
//        viewModelStore.clear();
//    }
//
//    @OptIn(markerClass = UnstableApi.class)
//    @SuppressWarnings("UnstableApiUsage")
//    private void updateDurationDisplay() {
//        TextView durationTextView =(TextView) findViewById(R.id.exo_duration1);
//        MediaController player = mediaController;
//        if (player == null) return;
//
//        boolean isLive = player.isCurrentMediaItemLive();
//
//        if (isLive) {
//            durationTextView.setText("LIVE");
//            durationTextView.setTextColor(Color.WHITE);
//            durationTextView.setBackground(ContextCompat.getDrawable(this, R.drawable.live_background));
//            durationTextView.setPadding(12, 4, 12, 4); // Optional padding
//        } else {
//            long durationMs = player.getDuration();
//            StringBuilder builder = new StringBuilder();
//            Formatter formatter = new Formatter();
//            String duration = androidx.media3.common.util.Util.getStringForTime(builder, formatter, durationMs);
//
//            durationTextView.setText(duration);
//            durationTextView.setTextColor(Color.WHITE);
//            durationTextView.setBackground(null);
//            durationTextView.setPadding(0, 0, 0, 0);
//        }
//    }
//
//
//    @NonNull
//    @Override
//    public ViewModelStore getViewModelStore() {
//        return viewModelStore;
//    }
//}

package com.example.carplayer.main

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaMetadata

class MainViewModel : ViewModel() {
    var lastMetadata: MediaMetadata? = null
    var isCurrentVideo: Boolean = false
}
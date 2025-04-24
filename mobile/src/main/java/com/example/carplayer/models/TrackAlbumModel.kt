package com.example.carplayer.models

import kotlinx.serialization.Serializable

@Serializable
data class TrackAlbumModel(
    var id: String,
    var title: String,
    var artist: String,
    var imageUrl: String,
)
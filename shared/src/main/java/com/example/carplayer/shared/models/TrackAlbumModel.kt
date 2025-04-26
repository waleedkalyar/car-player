package com.example.carplayer.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class TrackAlbumModel(
    var id: String,
    var title: String,
    var artist: String,
    var imageUrl: String,
)
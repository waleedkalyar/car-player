package com.example.carplayer.shared.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "albums")
data class TrackAlbumModel(
    @PrimaryKey var id: String,
    var title: String = "Unknown",
    var artist: String = "Unknown",
    var imageUrl: String,
    var streamUrl: String,
    var isPlaying: Boolean = false,
    var isVideo: Boolean = false,
)
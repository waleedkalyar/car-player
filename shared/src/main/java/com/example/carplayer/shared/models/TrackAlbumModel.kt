package com.example.carplayer.shared.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "albums")
data class TrackAlbumModel(
    @PrimaryKey var id: String,
    var title: String = "Unknown",
    var streamUrl: String,
    var imageUrl: String,
    var isPlaying: Boolean = false,
    var isFavourite: Boolean = false,
)
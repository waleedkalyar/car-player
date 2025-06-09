package com.example.carplayer.shared.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "albums", indices = [Index(value = ["channelNumber"], unique = true)])
data class TrackAlbumModel(
    @PrimaryKey var id: String,
    var title: String = "Unknown",
    var streamUrl: String,
    var imageUrl: String,
    var isPlaying: Boolean = false,
    var isFavourite: Boolean = false,
    var playBoxUrl: String? = null,
    var channelNumber: Int = 0
)
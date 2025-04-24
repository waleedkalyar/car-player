package com.example.carplayer.network.response

import com.google.gson.annotations.SerializedName

data class LastFmResponse(
    val track: Track?
)

data class Track(
    val name: String?,
    val artist: Artist?,
    val album: Album?
)

data class Artist(
    val name: String?
)

data class Album(
    val title: String?,
    val image: List<LastFmImage>?
)

data class LastFmImage(
    @SerializedName("#text")
    val url: String,
    val size: String
)

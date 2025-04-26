package com.example.carplayer.shared.network.api

import com.example.carplayer.shared.network.response.LastFmResponse
import retrofit2.http.GET
import retrofit2.http.Query

    interface LastFmApi {
        @GET("?method=track.getInfo&format=json")
        suspend fun getTrackInfo(
            @Query("api_key") apiKey: String,
            @Query("artist") artist: String,
            @Query("track") track: String
        ): LastFmResponse
    }
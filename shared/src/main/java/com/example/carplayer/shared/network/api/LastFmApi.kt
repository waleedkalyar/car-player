package com.example.carplayer.shared.network.api

import com.example.carplayer.shared.network.response.LastFmResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Query
import retrofit2.http.Url

interface LastFmApi {
        @GET("?method=track.getInfo&format=json")
        suspend fun getTrackInfo(
            @Query("api_key") apiKey: String,
            @Query("artist") artist: String,
            @Query("track") track: String
        ): LastFmResponse

        @HEAD
        suspend fun getImageHeaders(@Url url: String): Response<Void>
    }
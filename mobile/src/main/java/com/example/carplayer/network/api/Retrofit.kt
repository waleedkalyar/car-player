package com.example.carplayer.network.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://ws.audioscrobbler.com/2.0/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val lastFmApi = retrofit.create(LastFmApi::class.java)

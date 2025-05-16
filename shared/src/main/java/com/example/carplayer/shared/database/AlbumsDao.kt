package com.example.carplayer.shared.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumsDao {
    @Query("SELECT * FROM albums")
    fun getAll(): List<TrackAlbumModel>

    @Query("SELECT * FROM albums")
    fun listenAll(): Flow<List<TrackAlbumModel>>

    @Query("SELECT * FROM albums WHERE isVideo = 1")
    fun listenAllVideos(): Flow<List<TrackAlbumModel>>

    @Query("SELECT * FROM albums WHERE isVideo = 0")
    fun listenAllAudios(): Flow<List<TrackAlbumModel>>

    @Query("UPDATE albums SET isPlaying = 0")
    suspend fun resetPlaying()


    @Query("UPDATE albums SET isPlaying = 1,title = :title, artist =:artist, imageUrl = :artwork  WHERE streamUrl = :streamUrl")
    suspend fun updatePlayingByStreamUrl(
        streamUrl: String, title: String,
        artist: String,
        artwork: String
    )

    @Query("UPDATE albums SET isPlaying = 1 WHERE streamUrl = :streamUrl")
    suspend fun updatePlayingOnlyByStreamUrl(
        streamUrl: String,
    )


    @Transaction
    suspend fun markOnlyOneAsPlaying(
        streamUrl: String,
        title: String,
        artist: String,
        artwork: String
    ) {
        resetPlaying()
        updatePlayingOnlyByStreamUrl(streamUrl)
//        updatePlayingByStreamUrl(
//            streamUrl, title,
//            artist,
//            artwork
//        )
    }

    suspend fun listenAllWithConditions(
        isVideo: Boolean = false,
        isAudio: Boolean = false,
        all: Boolean = true
    ): Flow<List<TrackAlbumModel>> {
        return if (isVideo) {
            listenAllVideos()
        } else if (isAudio) {
            listenAllAudios()
        } else listenAll()
    }


    @Insert
    fun insertAll(vararg album: TrackAlbumModel)

    @Delete
    fun delete(album: TrackAlbumModel)


}
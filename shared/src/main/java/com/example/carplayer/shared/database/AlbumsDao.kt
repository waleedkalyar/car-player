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

    @Query("UPDATE albums SET isPlaying = 0")
    suspend fun resetPlaying()

    @Query("UPDATE albums SET isPlaying = 1 WHERE streamUrl = :streamUrl")
   suspend fun updatePlayingByStreamUrl(streamUrl: String)

    @Transaction
    suspend fun markOnlyOneAsPlaying(streamUrl: String) {
        resetPlaying()
        updatePlayingByStreamUrl(streamUrl)
    }

    @Insert
    fun insertAll(vararg album: TrackAlbumModel)

    @Delete
    fun delete(album: TrackAlbumModel)


}
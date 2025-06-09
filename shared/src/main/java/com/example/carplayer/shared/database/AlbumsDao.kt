package com.example.carplayer.shared.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.carplayer.shared.models.TrackAlbumModel
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumsDao {
    @Query("SELECT * FROM albums")
    fun getAll(): List<TrackAlbumModel>

    @Query("SELECT * FROM albums")
    fun listenAll(): Flow<List<TrackAlbumModel>>

    @Query("SELECT * FROM albums WHERE isFavourite = 1")
    fun listenAllFavourites(): Flow<List<TrackAlbumModel>>

    @Query("SELECT * FROM albums WHERE isFavourite = 1")
    fun getAllFavourites(): List<TrackAlbumModel>


    @Query("UPDATE albums SET isPlaying = 0")
    suspend fun resetPlaying()

    @Query("SELECT * FROM albums WHERE id=:id LIMIT 1")
    fun getItemById(id: String): TrackAlbumModel?


    @Query("UPDATE albums SET isPlaying = 1,title = :title  WHERE streamUrl = :streamUrl")
    suspend fun updatePlayingByStreamUrl(
        streamUrl: String, title: String)

    @Query("UPDATE albums SET isPlaying = 1 WHERE streamUrl = :streamUrl")
    suspend fun updatePlayingOnlyByStreamUrl(
        streamUrl: String,
    )

    @Query("UPDATE albums SET isFavourite = :isFavourite WHERE streamUrl = :streamUrl")
    suspend fun updateFavourite(isFavourite: Int, streamUrl: String)


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
        isFavourite: Boolean = false,
        all: Boolean = true
    ): Flow<List<TrackAlbumModel>> {
        return if (isFavourite) {
            listenAllFavourites()
        } else listenAll()
    }


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg album: TrackAlbumModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAlbum(album: TrackAlbumModel)

    @Delete
    fun delete(album: TrackAlbumModel)


    @Query("SELECT MAX(channelNumber) FROM albums")
    fun getMaxChannelNumber(): Int?

    @Query("SELECT * FROM albums WHERE streamUrl = :streamUrl LIMIT 1")
    fun getItemByStreamUrl(streamUrl: String): TrackAlbumModel?

    @Query("SELECT * FROM albums WHERE channelNumber = :channel LIMIT 1")
    fun getItemByChannel(channel: Int): TrackAlbumModel?

}
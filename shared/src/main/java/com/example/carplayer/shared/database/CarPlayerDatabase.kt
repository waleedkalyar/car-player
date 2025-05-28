package com.example.carplayer.shared.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.carplayer.shared.models.TrackAlbumModel

@Database(entities = [TrackAlbumModel::class], version = 5)
abstract class CarPlayerDatabase : RoomDatabase() {
    abstract fun albumsDao() : AlbumsDao



    companion object {
        @Volatile
        private var INSTANCE: CarPlayerDatabase? = null
        // Singleton instance
        fun getInstance(context: Context): CarPlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarPlayerDatabase::class.java,
                    "car_player_db"
                ).fallbackToDestructiveMigration(false) // Handles migrations
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
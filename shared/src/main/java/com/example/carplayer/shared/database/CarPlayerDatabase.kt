package com.example.carplayer.shared.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.carplayer.shared.models.TrackAlbumModel

@Database(entities = [TrackAlbumModel::class], version = 7)
abstract class CarPlayerDatabase : RoomDatabase() {
    abstract fun albumsDao() : AlbumsDao




    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE albums ADD COLUMN channelNumber INTEGER NOT NULL DEFAULT 0")
            }
        }
        @Volatile
        private var INSTANCE: CarPlayerDatabase? = null
        // Singleton instance
        fun getInstance(context: Context): CarPlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarPlayerDatabase::class.java,
                    "car_player_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false) // Handles migrations
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.miseservice.camerastream.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AdminSettingsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CameraStreamDatabase : RoomDatabase() {
    abstract fun adminSettingsDao(): AdminSettingsDao

    companion object {
        @Volatile
        private var instance: CameraStreamDatabase? = null

        fun getInstance(context: Context): CameraStreamDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CameraStreamDatabase::class.java,
                    "camera_stream.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}


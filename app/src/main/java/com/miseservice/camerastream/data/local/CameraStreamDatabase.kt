package com.miseservice.camerastream.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AdminSettingsEntity::class],
    version = 3,
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
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE admin_settings ADD COLUMN streamingPort INTEGER NOT NULL DEFAULT 8080"
                )
            }
        }
    }
}


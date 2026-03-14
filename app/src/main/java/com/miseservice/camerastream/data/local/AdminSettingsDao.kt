package com.miseservice.camerastream.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AdminSettingsDao {
    @Query("SELECT * FROM admin_settings WHERE id = :id")
    suspend fun getById(id: Int = AdminSettingsEntity.SINGLETON_ID): AdminSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AdminSettingsEntity)
}


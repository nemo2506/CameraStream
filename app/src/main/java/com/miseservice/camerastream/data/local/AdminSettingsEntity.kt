package com.miseservice.camerastream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_settings")
data class AdminSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val isStreaming: Boolean = false,
    val isFrontCamera: Boolean = true,
    val localIpAddress: String? = null,
    val isWakeLockActive: Boolean = false,
    val streamingPort: Int = 8080
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}


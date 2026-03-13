package com.miseservice.camerastream.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun allPermissionsGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun hasCamera(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.CAMERA)
    }

    fun hasInternet(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.INTERNET)
    }

    fun hasWakeLock(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.WAKE_LOCK)
    }
}


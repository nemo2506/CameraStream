package com.miseservice.camerastream.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.miseservice.camerastream.service.CameraStreamService
import com.miseservice.camerastream.domain.repository.StreamingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StreamingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StreamingRepository {
    override fun startStreaming(port: Int) {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_START
            putExtra(CameraStreamService.EXTRA_STREAMING_PORT, port)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopStreaming() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun switchCamera() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_SWITCH_CAMERA
        }
        context.startService(intent)
    }

    override fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }
}


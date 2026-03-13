package com.miseservice.camerastream.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.miseservice.camerastream.R
import com.miseservice.camerastream.camera.CameraManager
import com.miseservice.camerastream.server.StreamingHttpServer
import com.miseservice.camerastream.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CameraStreamService : Service() {
    private var cameraManager: CameraManager? = null
    private var httpServer: StreamingHttpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "camera_stream_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.miseservice.camerastream.ACTION_START"
        const val ACTION_STOP = "com.miseservice.camerastream.ACTION_STOP"
        const val ACTION_SWITCH_CAMERA = "com.miseservice.camerastream.ACTION_SWITCH_CAMERA"
        const val ACTION_TOGGLE_WAKE_LOCK = "com.miseservice.camerastream.ACTION_TOGGLE_WAKE_LOCK"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startStreaming()
            ACTION_STOP -> stopStreaming()
            ACTION_SWITCH_CAMERA -> switchCamera()
            ACTION_TOGGLE_WAKE_LOCK -> toggleWakeLock()
            else -> startStreaming()
        }

        return START_STICKY
    }

    private fun startStreaming() {
        if (httpServer != null) return // Already running

        scope.launch(Dispatchers.Default) {
            try {
                // Initialize Camera
                cameraManager = CameraManager(this@CameraStreamService)
                cameraManager?.initializeCamera()

                // Create HTTP Server
                httpServer = StreamingHttpServer(
                    port = 8080,
                    frameDataFlow = cameraManager?.latestFrameData ?: kotlinx.coroutines.flow.MutableStateFlow(null)
                )
                httpServer?.start()

                // Start foreground notification
                val notification = createNotification("Streaming actif")
                startForeground(NOTIFICATION_ID, notification)

                // Acquire wake lock if not already acquired
                if (wakeLock?.isHeld != true) {
                    acquireWakeLock()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("CameraStreamService", "Error starting streaming: ${e.message}", e)
                stopSelf()
            }
        }
    }

    private fun stopStreaming() {
        try {
            httpServer?.stop()
            httpServer = null
            cameraManager?.release()
            cameraManager = null
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchCamera() {
        cameraManager?.switchCamera()
    }

    private fun toggleWakeLock() {
        if (wakeLock?.isHeld == true) {
            releaseWakeLock()
        } else {
            acquireWakeLock()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CameraStream:WakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Flux caméra",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Service de streaming caméra"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val ip = NetworkUtils.getLocalIpAddress(this)
        val displayText = if (ip != null) "IP: $ip" else "Configuration..."

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Streaming Caméra")
            .setContentText(displayText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopStreaming()
        scope.cancel()
        super.onDestroy()
    }
}




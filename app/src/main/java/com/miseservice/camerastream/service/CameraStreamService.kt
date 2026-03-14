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
import com.miseservice.camerastream.data.streaming.StreamingRuntime
import com.miseservice.camerastream.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CameraStreamService : Service() {
    @Inject
    lateinit var streamingRuntime: StreamingRuntime

    private var isStreamingRuntimeStarted = false
    private var currentStreamingPort: Int = DEFAULT_STREAMING_PORT
    // CPU WakeLock : maintient le CPU et les capteurs d'orientation actifs même écran éteint
    // L'écran est géré par FLAG_KEEP_SCREEN_ON dans MainActivity (pattern Parking)
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "camera_stream_channel"
        private const val NOTIFICATION_ID = 1
        private const val DEFAULT_STREAMING_PORT = 8080
        const val ACTION_START = "com.miseservice.camerastream.ACTION_START"
        const val ACTION_STOP = "com.miseservice.camerastream.ACTION_STOP"
        const val ACTION_SWITCH_CAMERA = "com.miseservice.camerastream.ACTION_SWITCH_CAMERA"
        const val EXTRA_STREAMING_PORT = "extra_streaming_port"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val requestedPort = intent.getIntExtra(EXTRA_STREAMING_PORT, DEFAULT_STREAMING_PORT)
                currentStreamingPort = if (requestedPort in 1..65535) requestedPort else DEFAULT_STREAMING_PORT
                startStreaming()
            }
            ACTION_STOP -> stopStreaming()
            ACTION_SWITCH_CAMERA -> switchCamera()
            else -> startStreaming()
        }

        return START_STICKY
    }

    private fun startStreaming() {
        if (isStreamingRuntimeStarted) return

        scope.launch(Dispatchers.Default) {
            try {
                streamingRuntime.start(currentStreamingPort)
                isStreamingRuntimeStarted = true

                // Start foreground notification
                val notification = createNotification("Streaming WebRTC actif")
                startForeground(NOTIFICATION_ID, notification)

                // Acquérir automatiquement le WakeLock CPU pour maintenir
                // les capteurs actifs (orientation, gyroscope) même écran éteint
                acquireWakeLock()
                android.util.Log.d("CameraStreamService", "WebRTC streaming started")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("CameraStreamService", "Error starting streaming: ${e.message}", e)
                stopSelf()
            }
        }
    }

    private fun stopStreaming() {
        try {
            android.util.Log.d("CameraStreamService", "Stopping streaming...")

            streamingRuntime.stop()
            isStreamingRuntimeStarted = false

            // ✅ S'assurer que WakeLock est libéré
            releaseWakeLock()

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            android.util.Log.d("CameraStreamService", "Streaming stopped")
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error stopping streaming: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun switchCamera() {
        streamingRuntime.switchCamera()
    }

    /** Maintient le CPU actif (et les capteurs d'orientation) même écran éteint */
    private fun acquireWakeLock() {
        try {
            if (cpuWakeLock?.isHeld == true) return
            if (cpuWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                cpuWakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "CameraStream:CpuWakeLock"
                ).apply { setReferenceCounted(false) }
            }
            cpuWakeLock?.acquire()
            android.util.Log.d("CameraStreamService", "CPU WakeLock acquired: ${cpuWakeLock?.isHeld}")
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error acquiring CPU WakeLock: ${e.message}", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (cpuWakeLock?.isHeld == true) {
                cpuWakeLock?.release()
                android.util.Log.d("CameraStreamService", "CPU WakeLock released")
            }
            cpuWakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error releasing CPU WakeLock: ${e.message}", e)
            cpuWakeLock = null
        }
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
        val displayText = if (ip != null) "WebRTC: http://$ip:$currentStreamingPort/viewer" else contentText

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




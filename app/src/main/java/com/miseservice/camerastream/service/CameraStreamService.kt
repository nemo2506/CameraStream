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
import com.miseservice.camerastream.camera.CameraFrame
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
    // WakeLock CPU : maintient le CPU actif pour les capteurs d'orientation
    private var cpuWakeLock: PowerManager.WakeLock? = null
    // WakeLock écran : contrôlé par le bouton utilisateur
    private var screenWakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "camera_stream_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.miseservice.camerastream.ACTION_START"
        const val ACTION_STOP = "com.miseservice.camerastream.ACTION_STOP"
        const val ACTION_SWITCH_CAMERA = "com.miseservice.camerastream.ACTION_SWITCH_CAMERA"
        const val ACTION_SET_WAKE_LOCK = "com.miseservice.camerastream.ACTION_SET_WAKE_LOCK"
        const val EXTRA_WAKE_LOCK_ENABLED = "extra_wake_lock_enabled"
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
            ACTION_SET_WAKE_LOCK -> setWakeLockEnabled(
                intent.getBooleanExtra(EXTRA_WAKE_LOCK_ENABLED, false)
            )
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
                    frameDataFlow = cameraManager?.latestFrameData ?: kotlinx.coroutines.flow.MutableStateFlow<CameraFrame?>(null)
                )
                httpServer?.start()

                // Start foreground notification
                val notification = createNotification("Streaming actif")
                startForeground(NOTIFICATION_ID, notification)

                // Acquérir automatiquement le WakeLock CPU pour maintenir
                // les capteurs actifs (orientation, gyroscope) même écran éteint
                acquireWakeLock()
                android.util.Log.d("CameraStreamService", "Streaming started (WakeLock auto-acquired)")
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

            httpServer?.stop()
            httpServer = null
            cameraManager?.release()
            cameraManager = null

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
        cameraManager?.switchCamera()
    }

    private fun setWakeLockEnabled(enabled: Boolean) {
        try {
            if (enabled) {
                android.util.Log.d("CameraStreamService", "Setting Screen WakeLock: ON")
                acquireScreenWakeLock()
            } else {
                android.util.Log.d("CameraStreamService", "Setting Screen WakeLock: OFF")
                releaseScreenWakeLock()
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error setting WakeLock: ${e.message}", e)
        }
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
            releaseScreenWakeLock()
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error releasing CPU WakeLock: ${e.message}", e)
            cpuWakeLock = null
        }
    }

    /** Maintient l'écran allumé (contrôlé par le bouton utilisateur) */
    @Suppress("DEPRECATION")
    private fun acquireScreenWakeLock() {
        try {
            if (screenWakeLock?.isHeld == true) {
                android.util.Log.d("CameraStreamService", "Screen WakeLock already held")
                return
            }
            if (screenWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                screenWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "CameraStream:ScreenWakeLock"
                ).apply { setReferenceCounted(false) }
            }
            screenWakeLock?.acquire()
            android.util.Log.d("CameraStreamService", "Screen WakeLock acquired: ${screenWakeLock?.isHeld}")
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error acquiring Screen WakeLock: ${e.message}", e)
        }
    }

    private fun releaseScreenWakeLock() {
        try {
            if (screenWakeLock?.isHeld == true) {
                screenWakeLock?.release()
                android.util.Log.d("CameraStreamService", "Screen WakeLock released")
            }
            screenWakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("CameraStreamService", "Error releasing Screen WakeLock: ${e.message}", e)
            screenWakeLock = null
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




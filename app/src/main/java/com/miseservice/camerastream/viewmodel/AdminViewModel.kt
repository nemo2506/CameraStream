package com.miseservice.camerastream.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.camerastream.service.CameraStreamService
import com.miseservice.camerastream.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isStreaming: Boolean = false,
    val isFrontCamera: Boolean = true,
    val localIpAddress: String? = null,
    val streamingUrl: String? = null,
    val statusUrl: String? = null,
    val isWifiConnected: Boolean = false,
    val wifiNetworkName: String? = null,
    val isWakeLockActive: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AdminViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    init {
        // Initialiser la détection de l'IP au démarrage
        initializeNetworkDetection()
    }

    /**
     * Initialise la détection de l'IP
     */
    private fun initializeNetworkDetection() {
        viewModelScope.launch {
            try {
                // Vérifier la connexion WiFi
                val isWifiConnected = NetworkUtils.isWifiConnected(context)

                if (!isWifiConnected) {
                    _uiState.value = _uiState.value.copy(
                        isWifiConnected = false,
                        errorMessage = "WiFi non connecté. Veuillez connecter le WiFi.",
                        isLoading = false
                    )
                    return@launch
                }

                // Détecter l'IP locale
                val ipAddress = NetworkUtils.getLocalIpAddress(context)
                val streamingUrl = NetworkUtils.getStreamingUrl(context)
                val statusUrl = NetworkUtils.getStatusUrl(context)
                val wifiNetworkName = NetworkUtils.getWifiNetworkName(context)

                if (ipAddress != null) {
                    _uiState.value = _uiState.value.copy(
                        localIpAddress = ipAddress,
                        streamingUrl = streamingUrl,
                        statusUrl = statusUrl,
                        isWifiConnected = true,
                        wifiNetworkName = wifiNetworkName,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isWifiConnected = false,
                        errorMessage = "Impossible de détecter l'adresse IP. Essayez de reconnecter le WiFi.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erreur lors de la détection IP: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Rafraîchit la détection de l'IP
     */
    fun refreshNetworkDetection() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        initializeNetworkDetection()
    }

    fun startStreaming() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.value = _uiState.value.copy(isStreaming = true)
    }

    fun stopStreaming() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_STOP
        }
        context.startService(intent)
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun switchCamera() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_SWITCH_CAMERA
        }
        context.startService(intent)
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }

    fun toggleWakeLock() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_TOGGLE_WAKE_LOCK
        }
        context.startService(intent)
        _uiState.value = _uiState.value.copy(isWakeLockActive = !_uiState.value.isWakeLockActive)
    }

    fun copyUrlToClipboard() {
        val url = _uiState.value.streamingUrl ?: return
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Streaming URL", url)
        clipboard.setPrimaryClip(clip)
    }
}


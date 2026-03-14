package com.miseservice.camerastream.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miseservice.camerastream.domain.model.AdminSettings
import com.miseservice.camerastream.domain.usecase.CopyToClipboardUseCase
import com.miseservice.camerastream.domain.usecase.FetchNetworkInfoUseCase
import com.miseservice.camerastream.domain.usecase.LoadAdminSettingsUseCase
import com.miseservice.camerastream.domain.usecase.SaveAdminSettingsUseCase
import com.miseservice.camerastream.domain.usecase.StartStreamingUseCase
import com.miseservice.camerastream.domain.usecase.StopStreamingUseCase
import com.miseservice.camerastream.domain.usecase.SwitchCameraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isStreaming: Boolean = false,
    val isFrontCamera: Boolean = true,
    val streamingPort: Int = 8080,
    val localIpAddress: String? = null,
    val streamingUrl: String? = null,
    val statusUrl: String? = null,
    val isWifiConnected: Boolean = false,
    val wifiNetworkName: String? = null,
    val ipChangeNotice: String? = null,
    val isWakeLockActive: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val loadAdminSettingsUseCase: LoadAdminSettingsUseCase,
    private val saveAdminSettingsUseCase: SaveAdminSettingsUseCase,
    private val fetchNetworkInfoUseCase: FetchNetworkInfoUseCase,
    private val startStreamingUseCase: StartStreamingUseCase,
    private val stopStreamingUseCase: StopStreamingUseCase,
    private val switchCameraUseCase: SwitchCameraUseCase,
    private val copyToClipboardUseCase: CopyToClipboardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState
    private var hasCompletedInitialNetworkCheck = false
    private var ipMonitoringJob: Job? = null

    init {
        viewModelScope.launch {
            restorePersistedState()
            initializeNetworkDetection()
        }
        startIpMonitoring()
    }

    /**
     * Initialise la détection de l'IP
     */
    private suspend fun initializeNetworkDetection() {
        try {
            val previousIpAddress = _uiState.value.localIpAddress
            val networkInfo = fetchNetworkInfoUseCase(_uiState.value.streamingPort)

            if (networkInfo.localIpAddress != null) {
                val showIpChangeNotice = hasCompletedInitialNetworkCheck &&
                    !previousIpAddress.isNullOrBlank() &&
                    previousIpAddress != networkInfo.localIpAddress

                updateStateAndPersist(
                    _uiState.value.copy(
                        localIpAddress = networkInfo.localIpAddress,
                        streamingUrl = networkInfo.streamingUrl,
                        statusUrl = networkInfo.statusUrl,
                        isWifiConnected = networkInfo.isWifiConnected,
                        wifiNetworkName = networkInfo.wifiNetworkName,
                        ipChangeNotice = if (showIpChangeNotice) {
                            "IP locale changée : $previousIpAddress -> ${networkInfo.localIpAddress}"
                        } else {
                            _uiState.value.ipChangeNotice
                        },
                        isLoading = false,
                        errorMessage = networkInfo.errorMessage
                    )
                )
                hasCompletedInitialNetworkCheck = true
            } else {
                updateStateAndPersist(
                    _uiState.value.copy(
                        isWifiConnected = networkInfo.isWifiConnected,
                        localIpAddress = networkInfo.localIpAddress,
                        streamingUrl = networkInfo.streamingUrl,
                        statusUrl = networkInfo.statusUrl,
                        wifiNetworkName = networkInfo.wifiNetworkName,
                        errorMessage = networkInfo.errorMessage,
                        isLoading = false
                    )
                )
                hasCompletedInitialNetworkCheck = true
            }
        } catch (e: Exception) {
            updateStateAndPersist(
                _uiState.value.copy(
                    errorMessage = "Erreur lors de la détection IP: ${e.message}",
                    isLoading = false
                )
            )
            hasCompletedInitialNetworkCheck = true
        }
    }

    /**
     * Rafraîchit la détection de l'IP
     */
    fun refreshNetworkDetection() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            initializeNetworkDetection()
        }
    }

    fun dismissIpChangeNotice() {
        _uiState.value = _uiState.value.copy(ipChangeNotice = null)
    }

    fun startStreaming() {
        startStreamingUseCase(_uiState.value.streamingPort)
        _uiState.value = _uiState.value.copy(
            isStreaming = true
        )
        persistCurrentState()
    }

    fun setStreamingPort(rawPort: String) {
        val parsed = rawPort.toIntOrNull()
        val validatedPort = parsed?.takeIf { it in 1..65535 } ?: return
        if (validatedPort == _uiState.value.streamingPort) return

        _uiState.value = _uiState.value.copy(streamingPort = validatedPort)
        persistCurrentState()
        refreshNetworkDetection()
    }

    fun stopStreaming() {
        stopStreamingUseCase()
        _uiState.value = _uiState.value.copy(
            isStreaming = false,
            isWakeLockActive = false  // ✅ WakeLock est désactivé à l'arrêt
        )
        persistCurrentState()
    }

    fun switchCamera() {
        switchCameraUseCase()
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
        persistCurrentState()
    }

    fun setWakeLockEnabled(enabled: Boolean) {
        // Le maintien écran allumé est géré par FLAG_KEEP_SCREEN_ON dans MainActivity
        // (même pattern que le projet Parking)
        // Le CPU WakeLock est géré automatiquement par le service au démarrage du streaming
        android.util.Log.d("AdminViewModel", "Setting WakeLock (screen) to: $enabled")
        _uiState.value = _uiState.value.copy(isWakeLockActive = enabled)
        persistCurrentState()
    }

    fun copyUrlToClipboard() {
        val url = _uiState.value.streamingUrl ?: return
        copyToClipboardUseCase("Streaming URL", url)
    }

    private suspend fun restorePersistedState() {
        val saved = loadAdminSettingsUseCase()
        _uiState.value = _uiState.value.copy(
            isStreaming = saved.isStreaming,
            isFrontCamera = saved.isFrontCamera,
            streamingPort = saved.streamingPort,
            localIpAddress = saved.localIpAddress,
            isWakeLockActive = saved.isWakeLockActive,
            isLoading = true
        )
    }

    private fun persistCurrentState() {
        viewModelScope.launch {
            saveAdminSettingsUseCase(_uiState.value.toDomain())
        }
    }

    private suspend fun updateStateAndPersist(newState: AdminUiState) {
        if (_uiState.value == newState) return
        _uiState.value = newState
        saveAdminSettingsUseCase(newState.toDomain())
    }

    private fun startIpMonitoring() {
        ipMonitoringJob?.cancel()
        ipMonitoringJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                initializeNetworkDetection()
            }
        }
    }

    private fun AdminUiState.toDomain(): AdminSettings {
        return AdminSettings(
            isStreaming = isStreaming,
            isFrontCamera = isFrontCamera,
            streamingPort = streamingPort,
            localIpAddress = localIpAddress,
            isWakeLockActive = isWakeLockActive
        )
    }

    override fun onCleared() {
        ipMonitoringJob?.cancel()
        super.onCleared()
    }
}



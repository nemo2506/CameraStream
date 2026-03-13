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

class AdminViewModel(private val context: Context) : ViewModel() {

    private val _streamingUrl = MutableStateFlow<String?>(null)
    val streamingUrl: StateFlow<String?> = _streamingUrl

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera

    private val _isWakeLockActive = MutableStateFlow(false)
    val isWakeLockActive: StateFlow<Boolean> = _isWakeLockActive

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp

    init {
        viewModelScope.launch {
            updateStreamingUrl()
        }
    }

    private fun updateStreamingUrl() {
        val url = NetworkUtils.getStreamingUrl(context)
        val ip = NetworkUtils.getLocalIpAddress(context)
        _streamingUrl.value = url
        _localIp.value = ip
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
        _isStreaming.value = true
        updateStreamingUrl()
    }

    fun stopStreaming() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_STOP
        }
        context.startService(intent)
        _isStreaming.value = false
    }

    fun switchCamera() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_SWITCH_CAMERA
        }
        context.startService(intent)
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun toggleWakeLock() {
        val intent = Intent(context, CameraStreamService::class.java).apply {
            action = CameraStreamService.ACTION_TOGGLE_WAKE_LOCK
        }
        context.startService(intent)
        _isWakeLockActive.value = !_isWakeLockActive.value
    }

    fun copyUrlToClipboard() {
        val url = _streamingUrl.value ?: return
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Streaming URL", url)
        clipboard.setPrimaryClip(clip)
    }
}


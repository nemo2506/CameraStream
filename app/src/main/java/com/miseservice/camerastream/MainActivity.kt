package com.miseservice.camerastream

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miseservice.camerastream.ui.screens.AdminScreen
import com.miseservice.camerastream.ui.theme.CameraStreamTheme
import com.miseservice.camerastream.presentation.viewmodel.AdminViewModel
import com.miseservice.camerastream.service.CameraStreamService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        setContent {
            CameraStreamTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    MainContent(
                        window = window,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        notifyServiceAppState(CameraStreamService.ACTION_APP_FOREGROUND)
    }

    override fun onStop() {
        notifyServiceAppState(CameraStreamService.ACTION_APP_BACKGROUND)
        super.onStop()
    }

    private fun notifyServiceAppState(action: String) {
        val intent = Intent(this, CameraStreamService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
private fun MainContent(
    window: android.view.Window,
    modifier: Modifier = Modifier
) {
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WAKE_LOCK
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    if (permissionsGranted) {
        val viewModel: AdminViewModel = hiltViewModel()

        // Même pattern que le projet Parking :
        // FLAG_KEEP_SCREEN_ON appliqué/retiré de façon réactive selon l'état du bouton veille
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(uiState.isWakeLockActive) {
            if (uiState.isWakeLockActive) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        AdminScreen(viewModel = viewModel, modifier = modifier)
    }
}

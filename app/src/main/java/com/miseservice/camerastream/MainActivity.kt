package com.miseservice.camerastream

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miseservice.camerastream.ui.screens.AdminScreen
import com.miseservice.camerastream.ui.theme.CameraStreamTheme
import com.miseservice.camerastream.viewmodel.AdminViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraStreamTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    MainContent(
                        context = this@MainActivity,
                        window = window,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    context: android.content.Context,
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
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    if (permissionsGranted) {
        val viewModel = viewModel<AdminViewModel>(
            factory = AdminViewModelFactory(context)
        )

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

class AdminViewModelFactory(private val context: android.content.Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            AdminViewModel(context) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
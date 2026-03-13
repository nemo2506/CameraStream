package com.miseservice.camerastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miseservice.camerastream.viewmodel.AdminViewModel

@Composable
fun AdminScreen(viewModel: AdminViewModel, modifier: Modifier = Modifier) {
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingUrl by viewModel.streamingUrl.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isWakeLockActive by viewModel.isWakeLockActive.collectAsState()
    val localIp by viewModel.localIp.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Administration - Streaming Caméra",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Status Card
        StatusCard(isStreaming = isStreaming)

        // Control Buttons
        ControlButtonsSection(
            isStreaming = isStreaming,
            onStartStreaming = { viewModel.startStreaming() },
            onStopStreaming = { viewModel.stopStreaming() }
        )

        // Camera Selection
        CameraSelectionCard(
            isFrontCamera = isFrontCamera,
            onSwitchCamera = { viewModel.switchCamera() }
        )

        // Network Information
        NetworkInfoCard(
            localIp = localIp,
            streamingUrl = streamingUrl,
            onCopyUrl = { viewModel.copyUrlToClipboard() }
        )

        // Wake Lock Control
        WakeLockCard(
            isWakeLockActive = isWakeLockActive,
            onToggleWakeLock = { viewModel.toggleWakeLock() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatusCard(isStreaming: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isStreaming) Color(0xFF4CAF50) else Color(0xFFFFB74D)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isStreaming) "Streaming ACTIF" else "Streaming ARRÊTÉ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isStreaming) "✓ Caméra en cours de diffusion" else "Cliquez pour démarrer",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ControlButtonsSection(
    isStreaming: Boolean,
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartStreaming,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xCCCCCC)
            ),
            enabled = !isStreaming,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Démarrer", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onStopStreaming,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336),
                disabledContainerColor = Color(0xCCCCCC)
            ),
            enabled = isStreaming,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Arrêter", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CameraSelectionCard(
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sélection de la caméra",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFrontCamera) Color(0xFF2196F3) else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraFront,
                        contentDescription = "Caméra avant",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Avant", color = Color.White)
                }

                Spacer(modifier = Modifier.size(12.dp))

                Button(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isFrontCamera) Color(0xFF2196F3) else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraRear,
                        contentDescription = "Caméra arrière",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Arrière", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Actuellement: Caméra ${if (isFrontCamera) "avant" else "arrière"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkInfoCard(
    localIp: String?,
    streamingUrl: String?,
    onCopyUrl: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Informations de connexion",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (localIp != null) {
                InfoRow(label = "Adresse IP locale", value = localIp)
            } else {
                Text(
                    text = "WiFi non connecté",
                    fontSize = 14.sp,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (streamingUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "URL de streaming",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = streamingUrl,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = onCopyUrl,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier URL",
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
            } else {
                Text(
                    text = "URL non disponible",
                    fontSize = 14.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun WakeLockCard(
    isWakeLockActive: Boolean,
    onToggleWakeLock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Mode veille",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isWakeLockActive) "Veille désactivée" else "Veille activée",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isWakeLockActive,
                onCheckedChange = { onToggleWakeLock() }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}


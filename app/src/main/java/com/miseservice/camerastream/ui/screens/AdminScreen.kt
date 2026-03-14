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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.miseservice.camerastream.presentation.viewmodel.AdminViewModel

@Composable
fun AdminScreen(viewModel: AdminViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Administration - Streaming Caméra",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Démarrez la diffusion, copiez l'URL du flux et réglez la caméra depuis cette page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!uiState.ipChangeNotice.isNullOrBlank()) {
            IpChangeNoticeCard(
                message = uiState.ipChangeNotice,
                onDismiss = { viewModel.dismissIpChangeNotice() }
            )
        }

        StatusCard(isStreaming = uiState.isStreaming)

        ControlButtonsSection(
            isStreaming = uiState.isStreaming,
            onStartStreaming = { viewModel.startStreaming() },
            onStopStreaming = { viewModel.stopStreaming() }
        )

        NetworkInfoCard(
            streamingUrl = uiState.streamingUrl,
            onCopyUrl = { viewModel.copyUrlToClipboard() }
        )

        SectionLabel(text = "Réglages")

        PortEditorCard(
            currentPort = uiState.streamingPort,
            isStreaming = uiState.isStreaming,
            onPortChanged = { viewModel.setStreamingPort(it) }
        )

        CameraSelectionCard(
            isFrontCamera = uiState.isFrontCamera,
            onSwitchCamera = { viewModel.switchCamera() }
        )

        WakeLockCard(
            isStreaming = uiState.isStreaming,
            isWakeLockActive = uiState.isWakeLockActive,
            onWakeLockChanged = { enabled -> viewModel.setWakeLockEnabled(enabled) }
        )

        SectionLabel(text = "Réseau")

        NetworkDetectionSection(
            uiState = uiState,
            onRefresh = { viewModel.refreshNetworkDetection() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PortEditorCard(
    currentPort: Int,
    isStreaming: Boolean,
    onPortChanged: (String) -> Unit
) {
    var input by remember(currentPort) { mutableStateOf(currentPort.toString()) }
    val parsedPort = input.toIntOrNull()
    val isValidPort = parsedPort != null && parsedPort in 1..65535
    val isChanged = parsedPort != null && parsedPort != currentPort

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            EmojiText(
                emoji = "🔌",
                label = "Port du serveur WebRTC",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Port (1-65535)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        if (isStreaming) {
                            "Le nouveau port sera pris en compte au prochain démarrage du streaming."
                        } else {
                            "Port actuel: $currentPort"
                        }
                    )
                },
                isError = input.isNotBlank() && !isValidPort
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onPortChanged(input) },
                enabled = isValidPort && isChanged,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Appliquer le port")
            }
        }
    }
}

@Composable
private fun IpChangeNoticeCard(
    message: String?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiText(
                emoji = "📢",
                label = message.orEmpty(),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EmojiText(
    emoji: String,
    label: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = fontSize * 2f)) {
                append(emoji)
                append(" ")
            }
            append(label)
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}

@Composable
private fun NetworkDetectionSection(
    uiState: com.miseservice.camerastream.presentation.viewmodel.AdminUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Titre avec bouton refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiText(
                    emoji = "🔍",
                    label = "Détection Réseau",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rafraîchir",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Contenu
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (uiState.errorMessage != null) {
                EmojiText(
                    emoji = "⚠️",
                    label = uiState.errorMessage,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                // WiFi connecté
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WiFi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.isWifiConnected) {
                            val displayName = if (uiState.wifiNetworkName.isNullOrEmpty()) {
                                "Connecté (activez la localisation pour voir le SSID)"
                            } else {
                                uiState.wifiNetworkName
                            }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Non connecté",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Icon(
                        imageVector = if (uiState.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "WiFi",
                        tint = if (uiState.isWifiConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Adresse IP
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    EmojiText(
                        emoji = "📍",
                        label = "Adresse IP Locale",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.localIpAddress ?: "Détection en cours...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(isStreaming: Boolean) {
    val containerColor = if (isStreaming) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isStreaming) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
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
                    color = contentColor
                )
                Text(
                    text = if (isStreaming) "✓ Caméra en cours de diffusion" else "Cliquez pour démarrer",
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.9f)
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
    val colorScheme = MaterialTheme.colorScheme

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
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                disabledContainerColor = colorScheme.outline.copy(alpha = 0.24f),
                disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            enabled = !isStreaming,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Démarrer", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onStopStreaming,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer,
                disabledContainerColor = colorScheme.outline.copy(alpha = 0.24f),
                disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            enabled = isStreaming,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Arrêter", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CameraSelectionCard(
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            EmojiText(
                emoji = "🎥",
                label = "Sélection de la caméra",
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
                        containerColor = if (isFrontCamera) colorScheme.primary else colorScheme.surfaceVariant,
                        contentColor = if (isFrontCamera) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraFront,
                        contentDescription = "Caméra avant",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Avant")
                }

                Spacer(modifier = Modifier.size(12.dp))

                Button(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isFrontCamera) colorScheme.primary else colorScheme.surfaceVariant,
                        contentColor = if (!isFrontCamera) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraRear,
                        contentDescription = "Caméra arrière",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Arrière")
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
    streamingUrl: String?,
    onCopyUrl: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            EmojiText(
                emoji = "🌐",
                label = "Url de streaming",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))


            if (streamingUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
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
                            text = streamingUrl,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onCopyUrl,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier URL",
                            tint = colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    text = "URL non disponible",
                    fontSize = 14.sp,
                    color = colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WakeLockCard(
    isStreaming: Boolean,
    isWakeLockActive: Boolean,
    onWakeLockChanged: (Boolean) -> Unit
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
                EmojiText(
                    emoji = "⚡",
                    label = "Mode veille écran",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !isStreaming -> "Démarrez le streaming pour contrôler la veille"
                        isWakeLockActive -> "Écran maintenu allumé"
                        else -> "L'écran peut s'éteindre (streaming continu)"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isWakeLockActive,
                onCheckedChange = onWakeLockChanged,
                enabled = isStreaming
            )
        }
    }
}


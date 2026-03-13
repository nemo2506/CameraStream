# Configuration & Architecture - Streaming Caméra

## Structure du Projet

### Dossiers Clés

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/miseservice/camerastream/
│   │   ├── MainActivity.kt              # Entry point
│   │   ├── service/
│   │   │   └── CameraStreamService.kt   # Service foreground
│   │   ├── camera/
│   │   │   └── CameraManager.kt         # Camera2 API wrapper
│   │   ├── server/
│   │   │   └── StreamingHttpServer.kt   # HTTP server MJPEG
│   │   ├── viewmodel/
│   │   │   └── AdminViewModel.kt        # État UI
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   └── AdminScreen.kt       # Compose UI
│   │   │   └── theme/
│   │   │       ├── Color.kt
│   │   │       ├── Theme.kt
│   │   │       └── Type.kt
│   │   └── utils/
│   │       ├── NetworkUtils.kt          # IP, URL helpers
│   │       └── PermissionHelper.kt      # Permission checks
│   └── res/                             # Resources
└── build.gradle.kts
```

## Modules Détaillés

### 1. MainActivity.kt

**Responsabilités** :
- Point d'entrée de l'application
- Gestion des permissions runtime
- Initialisation de la Compose UI
- Création du ViewModel

**Flux** :
```
onCreate()
  ↓
setContent { CameraStreamTheme { Scaffold { MainContent } } }
  ↓
MainContent()
  ↓
requestPermissions()
  ↓
if (permissionsGranted) AdminScreen(viewModel)
```

**Permissions Requises** :
- CAMERA
- INTERNET
- ACCESS_NETWORK_STATE
- WAKE_LOCK
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_MEDIA_PROJECTION (API 33+)

### 2. CameraStreamService.kt

**Type** : Foreground Service (API 26+)

**Responsabilités** :
- Orchestration de la caméra et du serveur
- Gestion du WakeLock
- Notifications foreground
- Actions : START, STOP, SWITCH_CAMERA, TOGGLE_WAKE_LOCK

**Lifecycle** :
```
onCreate()
  ↓ startStreaming()
    ├─ CameraManager.initializeCamera()
    ├─ StreamingHttpServer.start()
    ├─ acquireWakeLock()
    └─ startForeground(notification)
  ↓
onStartCommand(intent)
  ├─ ACTION_START : startStreaming()
  ├─ ACTION_STOP : stopStreaming()
  ├─ ACTION_SWITCH_CAMERA : cameraManager.switchCamera()
  └─ ACTION_TOGGLE_WAKE_LOCK : toggleWakeLock()
  ↓
onDestroy() → stopStreaming()
```

**WakeLock** :
```kotlin
PowerManager.PARTIAL_WAKE_LOCK  // CPU reste actif, écran peut s'éteindre
```

### 3. CameraManager.kt

**Dépendance** : Camera2 API (androidx.camera:camera-camera2)

**Responsabilités** :
- Initialisation Camera2
- Capture frames en YUV420 (NV21)
- Changement caméra avant/arrière
- Exposition des frames via StateFlow

**Flux de Capture** :
```
initializeCamera()
  ├─ CameraManager (system service)
  ├─ CameraDevice.StateCallback
  ├─ ImageReader (1280×720, NV21)
  └─ ImageAnalysis (30 FPS target)
    ↓
ImageReader.onImageAvailable()
  ├─ Récupère Image (YUV420)
  ├─ Convertit en NV21 byte[]
  ├─ Stocke dans _latestFrameData (StateFlow)
  └─ Ferme l'image

switchCamera()
  ├─ isUsingFrontCamera = !isUsingFrontCamera
  ├─ release()
  └─ initializeCamera()
```

**Formats** :
```
Input  : YUV420 (NV21)  - Format natif caméra
Output : JPEG (85%)      - Compression pour MJPEG
```

### 4. StreamingHttpServer.kt

**Architecture** : Serveur TCP natif (sans dépendances)

**Responsabilités** :
- Écoute sur port 8080
- Gestion connexions client
- Streaming MJPEG
- Endpoint `/stream` et `/status`

**Protocole MJPEG** :
```
Connection HTTP:
--boundary
Content-Type: image/jpeg
Content-Length: 12345

[12345 bytes JPEG data]
--boundary
Content-Type: image/jpeg
Content-Length: 12346

[12346 bytes JPEG data]
...
```

**Conversion YUV420 → JPEG** :
```kotlin
YuvImage(nv21, ImageFormat.NV21, width, height, null)
  ↓
yuvImage.compressToJpeg(Rect(...), quality=85, output)
  ↓
ByteArray (JPEG)
```

**Threading** :
```
Main Thread       : Accepte connexions
Handler Threads   : Une par client
Frame Thread      : Conversion YUV→JPEG
```

### 5. AdminViewModel.kt

**Dépendances** : ViewModel + Coroutines + StateFlow

**Responsabilités** :
- Gestion état streaming (isStreaming, isFrontCamera, etc.)
- Récupération IP locale
- Génération URL streaming
- Communication avec CameraStreamService

**État** :
```kotlin
private val _isStreaming = MutableStateFlow(false)
private val _streamingUrl = MutableStateFlow<String?>(null)
private val _isFrontCamera = MutableStateFlow(true)
private val _isWakeLockActive = MutableStateFlow(false)
private val _localIp = MutableStateFlow<String?>(null)
```

**Actions** :
```
startStreaming()       : Intent(ACTION_START) → service
stopStreaming()        : Intent(ACTION_STOP) → service
switchCamera()         : Intent(ACTION_SWITCH_CAMERA) → service
toggleWakeLock()       : Intent(ACTION_TOGGLE_WAKE_LOCK) → service
copyUrlToClipboard()   : Copy streamingUrl to system clipboard
```

### 6. AdminScreen.kt

**Framework** : Jetpack Compose + Material3

**Composables** :
- `AdminScreen()` : Container principal
- `StatusCard()` : Affiche état streaming
- `ControlButtonsSection()` : Démarrer/Arrêter
- `CameraSelectionCard()` : Avant/Arrière
- `NetworkInfoCard()` : IP + URL
- `WakeLockCard()` : Toggle veille
- `InfoRow()` : Affichage générique clé/valeur

**Reactive** :
```kotlin
val isStreaming by viewModel.isStreaming.collectAsState()
// Recompose automatiquement si isStreaming change
```

### 7. NetworkUtils.kt

**Utilité** : Helpers réseau

```kotlin
getLocalIpAddress(context: Context): String?
  // Via WifiManager.connectionInfo.ipAddress
  // Retourne "192.168.1.100" ou null

getStreamingUrl(context: Context, port: Int): String?
  // Retourne "http://192.168.1.100:8080/stream"
```

### 8. PermissionHelper.kt

**Utilité** : Vérification permissions

```kotlin
hasPermission(context, permission): Boolean
  // Utilise ContextCompat.checkSelfPermission()

allPermissionsGranted(context, permissions): Boolean
  // Vérifie liste entière

hasCamera(), hasInternet(), hasWakeLock()
  // Helpers spécifiques
```

## Flux Complet

### Démarrage Complet

```
User lance app
  ↓
MainActivity.onCreate()
  ├─ setContent(Compose)
  └─ requestPermissions()
    ↓
User accepte permissions
  ↓
MainContent() affiche AdminScreen
  ├─ AdminViewModel créé
  ├─ IP locale récupérée
  └─ URL streaming générée
    ↓
User clique "Démarrer"
  ↓
viewModel.startStreaming()
  ├─ Intent(ACTION_START) → CameraStreamService
  ├─ Build.VERSION.SDK_INT >= O ?
  │  ├─ startForegroundService() (API 26+)
  │  └─ startService() (API <26)
  └─ _isStreaming = true
    ↓
CameraStreamService.onStartCommand()
  ├─ action == ACTION_START
  └─ startStreaming()
    ├─ CameraManager.initializeCamera()
    │  ├─ Ouvre caméra (front)
    │  ├─ Setup ImageReader
    │  └─ Setup ImageAnalysis → capture frames
    ├─ StreamingHttpServer.start()
    │  ├─ ServerSocket(8080)
    │  └─ Accepte connexions
    ├─ acquireWakeLock()
    └─ startForeground(notification)
      ↓
Streaming ACTIF
  ├─ CameraManager capture frames NV21
  ├─ ImageAnalysis.setAnalyzer() → _latestFrameData = nv21
  ├─ Client HTTP se connecte à /stream
  ├─ StreamingHttpServer lit _latestFrameData
  ├─ Convertit NV21 → JPEG
  ├─ Envoie MJPEG via HTTP
  └─ Client reçoit flux vidéo
```

### Arrêt

```
User clique "Arrêter"
  ↓
viewModel.stopStreaming()
  ├─ Intent(ACTION_STOP) → service
  └─ _isStreaming = false
    ↓
CameraStreamService.onStartCommand()
  ├─ action == ACTION_STOP
  └─ stopStreaming()
    ├─ httpServer.stop()
    │  ├─ serverSocket.close()
    │  └─ arrête boucle acceptation
    ├─ cameraManager.release()
    │  ├─ unbindAll()
    │  └─ ferme resources
    ├─ releaseWakeLock()
    │  └─ wake_lock.release()
    ├─ stopForeground(STOP_FOREGROUND_REMOVE)
    └─ stopSelf()
      ↓
Service arrêté
```

## Dépendances Clés

| Dépendance | Version | Utilisé pour |
|-----------|---------|--------------|
| androidx.camera:camera-core | 1.4.1 | Camera2 API |
| androidx.camera:camera-camera2 | 1.4.1 | Camera2 backend |
| androidx.camera:camera-lifecycle | 1.4.1 | Lifecycle binding |
| androidx.compose.material3:material3 | 1.2.0 | Material3 UI |
| androidx.lifecycle:lifecycle-viewmodel | 2.8.7 | ViewModel |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 | Integration Compose |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.8.0 | Async |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.8.0 | Main dispatcher |

Pas de dépendances pour le serveur HTTP (code natif avec Socket TCP)

## Configuration Build

```gradle
android {
    compileSdk = 36 (Android 15)
    targetSdk = 36 (Android 15)
    minSdk = 24 (Android 7.0)
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlinOptions {
    jvmTarget = "11"
}
```

## Variables d'Environnement

Aucune variable d'environnement requise.

Configuration fixe en code :
- Port HTTP : `8080`
- Résolution : `1280×720`
- FPS cible : `30`
- Qualité JPEG : `85`

## Points de Customization

Si vous voulez modifier :

### Résolution
`CameraManager.kt` ligne ~44:
```kotlin
.setTargetResolution(Size(1280, 720))
```

### Port HTTP
`CameraStreamService.kt` ligne ~105:
```kotlin
StreamingHttpServer(port = 9090)  // Par défaut 8080
```

### Qualité JPEG
`StreamingHttpServer.kt` ligne ~114:
```kotlin
val jpeg = yuv420ToJpeg(frameData, ..., 90)  // Par défaut 85
```

### FPS
`StreamingHttpServer.kt` ligne ~79:
```kotlin
val frameInterval = 66  // Par défaut 33 (30 FPS)
```

## Debugging

### Logs
```
CameraManager → Frame capture
StreamingHttpServer → Client connections
CameraStreamService → Service lifecycle
AdminViewModel → State changes
```

### Logcat
```bash
adb logcat *:S com.miseservice.camerastream:D
```

### Network
```bash
netstat -an | grep 8080  # Vérifier port écoute
```

## Sécurité

⚠️ **Attention** :
- Pas de chiffrement HTTPS
- Pas d'authentification
- À utiliser UNIQUEMENT sur réseau local privé
- Ne pas exposer sur internet

Pour production, ajouter :
- HTTPS avec certificats
- Authentication basique HTTP
- IP whitelist
- Rate limiting

## Performance

| Métrique | Valeur | Notes |
|----------|--------|-------|
| Capture FPS | 30 | ImageAnalysis |
| Latence réseau | ~100-500ms | WiFi dependent |
| CPU usage | 30-50% | Dépend device |
| RAM usage | 50-80MB | Stable |
| Batterie | ~15-20%/h | Avec WakeLock |

## Limitations Actuelles

1. Un seul client HTTP à la fois (architecture simple)
2. Pas de reconnexion automatique
3. Pas de buffer circulative (perte frames possible)
4. Format MJPEG pas optimal (utiliser H.264 pour meilleure bande)
5. HTTP non sécurisé

## Voir Aussi

- [README.md](README.md) - Documentation générale
- [QUICKSTART.md](QUICKSTART.md) - Guide utilisation
- [Android Camera2 Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)


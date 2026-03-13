# Application de Streaming Caméra Android

Une application Android complète pour le streaming de caméra en qualité maximale accessible via le réseau local avec un écran d'administration.

## Fonctionnalités

### 🎥 Streaming Caméra
- **Flux vidéo MJPEG** : Format optimisé pour qualité et compatibilité
- **Résolution 1280x720** : Qualité élevée tout en étant performante
- **Compression JPEG** : Qualité 85 pour un équilibre optimal entre qualité et bande passante
- **~30 FPS** : Fluidité du streaming

### 📱 Caméra Switchable
- **Caméra avant/arrière** : Changement rapide via l'interface
- **Gestion Camera2 API** : Utilisation de l'API moderne Android

### 🌐 Serveur HTTP Intégré
- **Streaming MJPEG** : Endpoint `/stream` pour visualiser en direct
- **Port 8080** : Accessible sur le réseau local
- **Socket TCP natif** : Sans dépendances externes
- **Endpoint `/status`** : Vérifier l'état du serveur

### ⚙️ Écran d'Administration
- **Démarrage/Arrêt du streaming** : Boutons de contrôle simples
- **Affichage IP locale** : Récupération automatique via WifiManager
- **URL de streaming** : Affichage et copie en presse-papiers
- **Sélection caméra avant/arrière** : Interface intuitive
- **Mode veille** : Activation/désactivation du WakeLock
- **Interface Material3** : Design moderne avec Jetpack Compose

### 🔋 Gestion Énergie
- **WakeLock PARTIAL** : Maintient le processeur actif sans allumer l'écran
- **Toggle veille** : Activation/désactivation facile via l'admin
- **Optimisation batterie** : Pas d'écran allumé inutilement

### 🔐 Permissions & Sécurité
- **CAMERA** : Accès à la caméra (avant/arrière)
- **INTERNET** : Communication réseau
- **ACCESS_NETWORK_STATE** : Détection WiFi
- **WAKE_LOCK** : Gestion de la veille
- **FOREGROUND_SERVICE** : Service avant-plan
- **Demandes runtime** : Permissions dynamiques à partir d'Android 6.0

## Architecture

```
CameraStream/
├── app/src/main/
│   ├── AndroidManifest.xml          # Permissions et déclaration service
│   ├── java/com/miseservice/camerastream/
│   │   ├── MainActivity.kt           # Activity principale + gestion permissions
│   │   ├── service/
│   │   │   └── CameraStreamService.kt # Service foreground qui orchestre tout
│   │   ├── camera/
│   │   │   └── CameraManager.kt      # Gestion Camera2 API
│   │   ├── server/
│   │   │   └── StreamingHttpServer.kt # Serveur HTTP MJPEG
│   │   ├── viewmodel/
│   │   │   └── AdminViewModel.kt     # Gestion état UI
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   └── AdminScreen.kt    # Écran administration Compose
│   │   │   └── theme/
│   │   │       └── ...               # Thème Material3
│   │   └── utils/
│   │       ├── NetworkUtils.kt       # Récupération IP locale
│   │       └── PermissionHelper.kt   # Vérification permissions
│   └── res/                          # Ressources (icônes, layouts, etc.)
└── build.gradle.kts                  # Configuration Gradle
```

## Flux de Fonctionnement

1. **Démarrage** : L'utilisateur accorde les permissions requises
2. **Interface Admin** : Affichage de l'écran d'administration
3. **Démarrage du streaming** :
   - Clic sur "Démarrer"
   - Initialisation de CameraStreamService
   - Initialisation CameraManager (Camera2 API)
   - Acquisition WakeLock
   - Démarrage serveur HTTP sur port 8080
4. **Streaming actif** :
   - CameraManager capture frames en NV21
   - Frames stockées dans MutableStateFlow
   - Serveur HTTP les récupère et les encode en JPEG
   - Envoi via MJPEG sur `/stream`
5. **Accès distant** :
   - URL : `http://<IP_LOCAL>:8080/stream`
   - Compatible navigateur, VLC, ffmpeg, etc.
   - Visible dans l'admin pour copie facile

## Utilisation

### Installation
```bash
./gradlew build  # Compiler l'APK
./gradlew installDebug  # Installer sur device
```

### Accès au streaming

1. Ouvrir l'app sur le téléphone
2. Accorder les permissions (caméra, réseau, etc.)
3. **Détecter l'IP automatiquement** ⭐ (Important!)
   ```bash
   # Linux/Mac
   ./detect-ip.sh
   
   # Windows
   detect-ip.bat
   ```
4. Cliquer "Démarrer" dans l'app
5. L'URL s'affiche : `http://192.168.x.x:8080/stream`
6. Copier l'URL et ouvrir dans navigateur/VLC sur n'importe quel device du réseau

### Scripts de Détection IP

L'application inclut des **scripts de détection automatique** pour trouver facilement l'adresse IP du téléphone :

- **`detect-ip.sh`** - Linux/Mac (Bash)
- **`detect-ip.bat`** - Windows (Batch)

Ces scripts :
- ✅ Trouvent automatiquement les devices Android connectés
- ✅ Détectent l'adresse IP locale
- ✅ Testent la connexion au serveur
- ✅ Créent un fichier de configuration
- ✅ Affichent l'URL de streaming prête à l'emploi

### Commandes curl pour tester
```bash
# Voir le flux (crée un fichier MJPEG)
curl http://192.168.x.x:8080/stream > stream.mjpeg

# Vérifier le statut
curl http://192.168.x.x:8080/status

# Avec ffmpeg
ffplay http://192.168.x.x:8080/stream
```

## Dépendances Clés

- **Jetpack Compose** : Interface moderne
- **CameraX** : API caméra moderne (CameraX)
- **Coroutines** : Programmation asynchrone
- **ViewModel** : Gestion état
- **Material3** : Design system

## Configuration

- **API minimum** : 24 (Android 7.0)
- **API cible** : 36 (Android 15)
- **Kotlin** : 2.0.21
- **Gradle** : 8.13.2

## Performance

- **Résolution** : 1280x720
- **FPS** : ~30
- **Qualité JPEG** : 85
- **CPU** : Optimisé (capture YUV natif)
- **Batterie** : WakeLock PARTIAL (pas d'écran)
- **RAM** : ~50-80MB (selon device)

## Limitations Connues

- Le streaming est accessible en HTTP (pas de chiffrement HTTPS)
- Un seul client connecté à la fois (architecture simple)
- La résolution est fixée (pas de configuration UI)
- WakeLock PARTIAL ne réveille pas l'écran (normal et voulu)

## Améliorations Futures Possibles

- Support HTTPS avec certificats autosignés
- Endpoint pour configuration résolution/FPS/qualité
- Support multi-client avec un serveur plus robuste
- Authentification HTTP Basic
- Interface HTML embarquée pour visualisation
- Support H.264/VP8 pour meilleure compression
- Streaming audio
- Enregistrement vidéo local
- Dashboard avec statistiques streaming

## Licence

Propriétaire - MISESERVICE

## Support

Pour toute question ou bug, contactez l'équipe de développement.


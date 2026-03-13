# Guide de Démarrage Rapide - Streaming Caméra

## Prérequis

- Android Studio Arctic Fox ou plus récent
- JDK 11+
- Android SDK 24+ (minimum)
- Un téléphone/émulateur Android avec caméra

## Installation Rapide

### 1. Compiler l'application

```bash
cd CameraStream
./gradlew build
```

### 2. Installer sur le device

```bash
./gradlew installDebug
```

### 3. Lancer l'app

- Ouvrir l'application "Camera Stream" sur le téléphone
- Accepter toutes les permissions demandées

---

## Détection Automatique de l'IP (Important!)

### Option A: Script Automatique (Recommandé)

**Linux/Mac:**
```bash
chmod +x detect-ip.sh
./detect-ip.sh
```

**Windows:**
```batch
detect-ip.bat
```

✅ Cela va:
1. Trouver votre device Android connecté
2. Détecter automatiquement l'IP locale
3. Tester la connexion
4. Afficher l'URL de streaming
5. Créer un fichier de configuration

**Exemple de sortie:**
```
✅ IP Address Detected Successfully!

📱 Device IP: 192.168.1.100
🌐 Streaming URL: http://192.168.1.100:8080/stream
📊 Status URL: http://192.168.1.100:8080/status
```

### Option B: Détection Manuelle

Si le script automatique ne fonctionne pas:

```bash
# Via ADB (device doit être connecté en USB et en débogage)
adb shell getprop dhcp.wlan0.ipaddress

# Ou via l'app elle-même
# L'IP s'affiche dans l'écran d'administration
```

---

## Première Utilisation

### Écran d'Administration

L'interface principal montre :

```
┌─────────────────────────────────────┐
│ Administration - Streaming Caméra   │
├─────────────────────────────────────┤
│                                     │
│    ┌─ Statut ─────────────────┐   │
│    │ Streaming ARRÊTÉ         │   │
│    │ Cliquez pour démarrer    │   │
│    └──────────────────────────┘   │
│                                     │
│  [  Démarrer   ] [  Arrêter  ]    │
│                                     │
│  ┌─ Sélection de la caméra ────┐  │
│  │ [  Avant (bleu) ]            │  │
│  │ [  Arrière (gris) ]          │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌─ Informations de connexion ──┐  │
│  │ Adresse IP: 192.168.1.100    │  │
│  │ URL: http://192.168.1.100... │  │
│  │                           [📋] │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌─ Mode veille ─────────────────┐ │
│  │ Veille désactivée      [✓] ON│  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Démarrage du Streaming

1. **Cliquez "Démarrer"** → Service foreground démarre
2. **Attendez 2-3 secondes** → CameraManager s'initialise
3. **Vérifiez le statut** → "Streaming ACTIF" s'affiche (vert)
4. **L'URL apparaît** → Copiez-la avec le bouton 📋

### Accès à la Vidéo

Depuis **un autre device sur le même réseau WiFi** :

#### Option 1 : Navigateur Web
```
Ouvrir URL directement :
http://192.168.x.x:8080/stream
```

#### Option 2 : VLC Media Player
```
Media → Ouvrir un flux réseau
Coller : http://192.168.x.x:8080/stream
```

#### Option 3 : ffplay (ffmpeg)
```bash
ffplay http://192.168.x.x:8080/stream
```

#### Option 4 : curl & ffmpeg
```bash
curl http://192.168.x.x:8080/stream | ffplay -f mjpeg -
```

## Changement de Caméra

1. Dans l'admin, appuyez sur le bouton **Avant** ou **Arrière**
2. Le flux bascule automatiquement (aucune interruption)
3. L'icône change de couleur pour indiquer la caméra active

## Gestion de la Veille

### Activation du Mode Veille
- **Toggle "Mode veille"** dans l'admin
- Le WakeLock est **désactivé**
- ⚠️ L'écran **peut s'éteindre** mais le streaming **continue**

### Désactivation du Mode Veille
- **Toggle "Mode veille"** à nouveau
- Le WakeLock est **acquis**
- L'écran **reste allumé** (consomme plus de batterie)

## Arrêt du Streaming

1. Cliquez **"Arrêter"**
2. Le service se ferme en 1-2 secondes
3. Le statut change à "Streaming ARRÊTÉ"
4. La connexion HTTP se ferme
5. L'app peut revenir en arrière-plan

## Dépannage

### "WiFi non connecté"
- Vérifiez que le téléphone est sur WiFi
- Vérifiez la connexion WiFi est active
- Relancez l'app

### URL vide
- Attendez 3 secondes après "Démarrer"
- Vérifiez le WiFi est connecté
- Redémarrez l'app

### Pas d'image dans le navigateur
- Vérifiez les deux devices sont sur **le même WiFi**
- Testez `http://IP:8080/status` dans curl
- Vérifiez le firewall n'est pas bloquant
- Essayez VLC au lieu du navigateur

### L'app plante au démarrage
- Accordez **toutes les permissions** demandées
- Vérifiez que Android 7.0+ est installé
- Dégagez la RAM de l'appareil
- Essayez une compilation `./gradlew clean build`

### Streaming très lent ou lag
- Réduisez d'autres usages du WiFi
- Réduisez la distance par rapport au routeur
- Changez de canal WiFi (2.4GHz → 5GHz ou vice-versa)

## Configuration Avancée

### Modifier la résolution
Dans `CameraManager.kt`, ligne ~30:
```kotlin
.setTargetResolution(Size(1280, 720))  // → Changer ici
```
Valeurs recommandées : 640×480, 1280×720, 1920×1080

### Modifier les FPS
Dans `StreamingHttpServer.kt`, ligne ~80:
```kotlin
val frameInterval = 33  // 1000/33 ≈ 30 FPS → Changer ici
```
Valeurs : 16ms=60FPS, 33ms=30FPS, 66ms=15FPS

### Modifier la qualité JPEG
Dans `StreamingHttpServer.kt`, ligne ~110:
```kotlin
val jpeg = yuv420ToJpeg(..., 85)  // Qualité 85 → Changer ici (0-100)
```

### Changer le port
Dans `CameraStreamService.kt`, ligne ~100:
```kotlin
StreamingHttpServer(port = 8080)  // → Changer le port ici
```

## Logs & Debugging

### Voir les logs en direct
```bash
./gradlew installDebug  # Compiler et installer
adb logcat | grep -i camerastream  # Voir les logs
```

### APK Release
```bash
./gradlew assembleRelease  # Build APK de release
# L'APK se trouve dans : app/build/outputs/apk/release/
```

## Points de Repère

| Métrique | Valeur |
|----------|--------|
| Résolution | 1280×720 |
| FPS | ~30 |
| Qualité JPEG | 85% |
| Port HTTP | 8080 |
| API minimum | 24 (Android 7.0) |
| API cible | 36 (Android 15) |
| Consommation RAM | ~50-80 MB |
| Consommation CPU | ~30-50% (selon device) |

## Cas d'Usage

### Home Security Camera
```
Téléphone monté en hauteur + streaming accessible partout
```

### Inspection à Distance
```
Caméra arrière pour inspection fine + affichage sur PC
```

### Live Streaming Local
```
Capturer dans OBS/Streamlabs via MJPEG HTTP
```

### Monitoring Réseau Local
```
Plusieurs téléphones actifs, affichage sur écran mural
```

## Troubleshooting Vidéo

Si vous avez des problèmes vidéo :

1. **Couleurs étranges** → Format YUV420 → JPEG (normal)
2. **Image figée** → Vérifiez les permissions caméra
3. **Très pixelisée** → Augmentez qualité JPEG à 90-95
4. **Décalage vidéo** → Normal, dépend du réseau

## Notes de Performance

- **LTE/4G** : Peut ne pas marcher ou être très lent
- **WiFi 2.4GHz** : Stable mais plus lent
- **WiFi 5GHz** : Meilleure vitesse et latence
- **WiFi 6** : Optimal

## Support & Contact

- 📧 Email : support@example.com
- 🐛 Bug reports : Issues sur le repository
- 💡 Suggestions : Contactez l'équipe dev

Enjoy your camera streaming! 🎥📱


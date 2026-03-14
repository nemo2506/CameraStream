# CameraStream (v1)

Application Android de streaming camera en WebRTC avec serveur HTTP integre.

## Caracteristiques

- Streaming video WebRTC depuis un telephone Android.
- Multi-client: plusieurs viewers peuvent se connecter en parallele.
- Viewer web integre expose sur `GET /viewer` (et `/`).
- Signaling HTTP via `POST /api/webrtc/offer`.
- Endpoint de statut via `GET /status` avec compteur de viewers.
- Reponse CORS et preflight `OPTIONS /api/webrtc/offer` gerees cote serveur.
- Service Android au premier plan (`CameraStreamService`) avec WakeLock CPU.

## SDK et compatibilite Android

Configuration actuelle dans `app/build.gradle.kts`:

- `versionName`: `v1`
- `versionCode`: `1`
- `minSdk`: `24` (Android 7.0+)
- `targetSdk`: `36`
- `compileSdk`: `36`
- Java/Kotlin JVM target: `11`

Permissions principales dans `app/src/main/AndroidManifest.xml`:

- `android.permission.CAMERA`
- `android.permission.INTERNET`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION`
- `android.permission.WAKE_LOCK`
- permissions reseau (`ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, etc.)

## Schema de communication (resume)

```
[Browser Viewer]
   |  GET /viewer
   |  POST /api/webrtc/offer (SDP offer)
   v
[WebRtcHttpServer sur Android :8080]
   |  createAnswer(offerSdp)
   v
[WebRtcEngine]
   |  reponse SDP answer
   v
[Browser Viewer]

Flux media: WebRTC direct entre Browser et WebRtcEngine.
```

Schema avec reverse proxy Apache (optionnel):

```
Browser -> https://phone.exemple.com -> Apache reverse proxy -> http://<phone-ip>:8080
```

## Endpoints HTTP

- `GET /` -> page viewer
- `GET /viewer` -> page viewer
- `POST /api/webrtc/offer` -> signaling offer/answer
- `OPTIONS /api/webrtc/offer` -> preflight CORS
- `GET /status` -> JSON `{"status":"streaming","protocol":"webrtc","viewers":N}`

## Usage rapide

1. Installer et lancer l'application sur le telephone.
2. Demarrer le streaming dans l'application (service foreground).
3. Recuperer l'IP locale du telephone (affichee dans la notification).
4. Ouvrir un navigateur sur le meme reseau et acceder a:
   - `http://<ip-telephone>:8080/viewer`
5. (Optionnel) Placer Apache en reverse proxy HTTPS vers `http://<ip-telephone>:8080`.

## Build local

```powershell
Set-Location "D:\PATH\apps\CameraStream"
.\gradlew.bat :app:assembleDebug
```

## Notes reseau

- En LAN, la connexion WebRTC est directe et simple a etablir.
- En acces Internet/NAT complexe, un serveur TURN peut etre necessaire pour fiabiliser la connexion media.


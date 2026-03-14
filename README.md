# CameraStream (v1) 📷🚀

Application Android de streaming camera en WebRTC avec serveur HTTP integre.

## Caracteristiques ✨

- 🎥 Streaming video WebRTC depuis un telephone Android.
- 👥 Multi-client: plusieurs viewers peuvent se connecter en parallele.
- 🌐 Viewer web integre expose sur `GET /viewer` (et `/`).
- 🔁 Signaling HTTP via `POST /api/webrtc/offer`.
- 📊 Endpoint de statut via `GET /status` avec compteur de viewers.
- 🛡️ Reponse CORS et preflight `OPTIONS /api/webrtc/offer` geres cote serveur.
- 🔋 Service Android au premier plan (`CameraStreamService`) avec WakeLock CPU.

## SDK et compatibilite Android 🤖

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

## Resume visuel (schema) 🧭

Protocole principal: **WebRTC** (ICE + DTLS + SRTP) pour le media, avec signaling HTTP/SDP.

### Variante HTML Bootstrap 🎨

<div style="border:1px solid #d0d7de;border-radius:12px;padding:16px;margin:12px 0;">
  <h4 style="margin:0 0 12px 0;">CameraStream - Schema de communication 🧭</h4>
  <div style="background:#ddf4ff;border:1px solid #54aeff;border-radius:8px;padding:10px;margin-bottom:14px;">
    Protocole media: <strong>WebRTC</strong> (ICE + DTLS + SRTP)<br />
    Signaling: HTTP/SDP via <code>/api/webrtc/offer</code>
  </div>

  <table style="width:100%;border-collapse:separate;border-spacing:10px;">
    <tr>
      <td style="vertical-align:top;border:1px solid #2f81f7;border-radius:10px;padding:10px;">
        <strong>[ID-01] 👤 Viewer</strong><br />
        Navigateur web<br /><br />
        <code>A1 GET /viewer</code>
      </td>
      <td style="vertical-align:top;border:1px solid #3fb950;border-radius:10px;padding:10px;">
        <strong>[ID-02] 📱 WebRtcHttpServer</strong><br />
        Android :8080<br /><br />
        <code>A2 POST /api/webrtc/offer</code><br />
        <code>C1 GET /status</code>
      </td>
      <td style="vertical-align:top;border:1px solid #d29922;border-radius:10px;padding:10px;">
        <strong>[ID-03] 🧠 WebRtcEngine</strong><br />
        <code>createAnswer(offerSdp)</code><br /><br />
        <code>A3 answer SDP</code>
      </td>
    </tr>
  </table>

  <p style="margin:10px 0 4px 0;"><strong>Flux principaux</strong></p>
  <ul>
    <li><strong>A1</strong> : ID-01 → ID-02 : <code>GET /viewer</code></li>
    <li><strong>A2</strong> : ID-01 → ID-02 : <code>POST /api/webrtc/offer</code> (offer SDP)</li>
    <li><strong>A2bis</strong> : ID-02 → ID-03 : <code>createAnswer(offerSdp)</code></li>
    <li><strong>A3</strong> : ID-03 → ID-02 → ID-01 : answer SDP (JSON)</li>
    <li><strong>B1</strong> : ID-01 ↔ ID-03 : media WebRTC (ICE + DTLS + SRTP)</li>
    <li><strong>C1</strong> : ID-01 → ID-02 : <code>GET /status</code> → <code>{"status","protocol","viewers"}</code></li>
  </ul>
</div>

## Schema avec Apache (optionnel) 🔐

```text
[ID-10] 👤 Browser
        |
        | HTTPS
        v
[ID-11] 🌍 Apache Reverse Proxy
        |
        | HTTP (LAN)
        v
[ID-02] 📱 Android :8080
```

## Endpoints HTTP 🔌

- `GET /` -> page viewer
- `GET /viewer` -> page viewer
- `POST /api/webrtc/offer` -> signaling offer/answer
- `OPTIONS /api/webrtc/offer` -> preflight CORS
- `GET /status` -> JSON `{"status":"streaming","protocol":"webrtc","viewers":N}`

## Usage rapide ▶️

1. Installer et lancer l'application sur le telephone.
2. Demarrer le streaming dans l'application (service foreground).
3. Recuperer l'IP locale du telephone (affichee dans la notification).
4. Ouvrir un navigateur sur le meme reseau et acceder a:
   - `http://<ip-telephone>:8080/viewer`
5. (Optionnel) Placer Apache en reverse proxy HTTPS vers `http://<ip-telephone>:8080`.

## Build local 🛠️

```powershell
Set-Location "D:\PATH\apps\CameraStream"
.\gradlew.bat :app:assembleDebug
```

## Notes reseau 📡

- 🏠 En LAN, la connexion WebRTC est directe et simple a etablir.
- 🌐 En acces Internet/NAT complexe, un serveur TURN peut etre necessaire pour fiabiliser la connexion media.

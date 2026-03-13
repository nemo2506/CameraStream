# Commandes curl pour Tester CameraStream

## Configuration Initiale

```bash
# Remplacer X.X.X.X par l'IP réelle du téléphone
IP=192.168.x.x
PORT=8080
STREAM_URL="http://$IP:$PORT/stream"
STATUS_URL="http://$IP:$PORT/status"
```

## 1. Test de Connectivité

### Vérifier que le serveur est accessible
```bash
curl -I http://192.168.x.x:8080/
```

**Réponse attendue:**
```
HTTP/1.1 404 Not Found
```

Cela signifie que le serveur répond (404 est normal pour `/`)

### Vérifier la route `/status`
```bash
curl http://192.168.x.x:8080/status
```

**Réponse attendue:**
```json
{"status":"streaming"}
```

ou

```json
{"status":"stopped"}
```

## 2. Test du Streaming MJPEG

### Télécharger le flux et l'enregistrer en fichier
```bash
# Enregistrer pendant 10 secondes (Ctrl+C pour arrêter)
curl --max-time 10 http://192.168.x.x:8080/stream > video.mjpeg
```

### Visualiser le flux en direct avec ffplay
```bash
# Nécessite ffmpeg installé
ffplay http://192.168.x.x:8080/stream
```

### Enregistrer en vidéo MP4 (recommandé)
```bash
# 30 secondes d'enregistrement
ffmpeg -t 30 -i http://192.168.x.x:8080/stream -c copy video.mp4

# Sans limite de temps (Ctrl+C pour arrêter)
ffmpeg -i http://192.168.x.x:8080/stream -c copy output.mp4
```

### Enregistrer en AVI (MJPEG)
```bash
ffmpeg -i http://192.168.x.x:8080/stream -c:v mpeg4 output.avi
```

## 3. Test avec différents Clients

### Navigateur Web
```
Simplement ouvrir dans le navigateur:
http://192.168.x.x:8080/stream
```

Fonctionnera sur:
- Chrome
- Firefox
- Safari
- Edge
- Opéra

### VLC Media Player
```bash
# Ligne de commande
vlc http://192.168.x.x:8080/stream

# Ou via GUI:
# Media → Ouvrir un flux réseau → http://192.168.x.x:8080/stream
```

### Navigateur avec VLC
```bash
# Sur Windows
start http://192.168.x.x:8080/stream

# Sur macOS
open http://192.168.x.x:8080/stream

# Sur Linux
xdg-open http://192.168.x.x:8080/stream
```

### mpv
```bash
mpv http://192.168.x.x:8080/stream
```

### Navigateur avec conversion
```bash
# Servir via HTTP local avec conversion en MP4
ffmpeg -i http://192.168.x.x:8080/stream -c:v libx264 -preset ultrafast -tune zerolatency -f mpegts http://localhost:8888/
```

## 4. Tests de Performance

### Mesurer la bande passante utilisée
```bash
# Linux/Mac
curl http://192.168.x.x:8080/stream 2>&1 | pv > /dev/null

# Avec timeout de 10 secondes
timeout 10 curl http://192.168.x.x:8080/stream 2>&1 | pv > /dev/null
```

### Monitoriser la connexion
```bash
# Linux - Voir les connexions établies
netstat -an | grep 8080

# Ou
ss -tan | grep 8080

# macOS
netstat -an | grep ESTABLISHED | grep 8080

# Windows (PowerShell)
Get-NetTCPConnection | Where-Object {$_.LocalPort -eq 8080}
```

## 5. Tests de Débit

### Enregistrer et afficher les statistiques
```bash
# Voir la taille téléchargée en temps réel
curl -w "Total: %{size_download} bytes\nSpeed: %{speed_download} B/s\n" \
  http://192.168.x.x:8080/stream > /dev/null
```

### Tester la latence
```bash
# Windows
curl -w "Time: %{time_total}s\n" -o /dev/null -s http://192.168.x.x:8080/status

# Linux/Mac
time curl -o /dev/null -s http://192.168.x.x:8080/status
```

## 6. Tests Avancés

### Enregistrement multi-client simultané
```bash
# Terminal 1
ffmpeg -i http://192.168.x.x:8080/stream -c copy video1.mp4 &

# Terminal 2
ffmpeg -i http://192.168.x.x:8080/stream -c copy video2.mp4 &

# Terminal 3
ffplay http://192.168.x.x:8080/stream &
```

### Extraction d'une frame JPEG
```bash
# Prendre une seule image JPEG du flux
ffmpeg -i http://192.168.x.x:8080/stream -vframes 1 -f image2 frame.jpg
```

### Extraire plusieurs frames
```bash
# Extraire 10 frames
ffmpeg -i http://192.168.x.x:8080/stream -vframes 10 frame_%03d.jpg
```

### Conversion MJPEG → MP4 haute qualité
```bash
ffmpeg -i http://192.168.x.x:8080/stream \
  -c:v libx264 \
  -preset fast \
  -crf 20 \
  -maxrate 2500k \
  -bufsize 5000k \
  output.mp4
```

### Streaming vers YouTube Live
```bash
# Remplacer STREAM_KEY par votre clé
STREAM_KEY="your-youtube-stream-key"
ffmpeg -i http://192.168.x.x:8080/stream \
  -c:v libx264 \
  -preset fast \
  -maxrate 2500k \
  -bufsize 5000k \
  -pix_fmt yuv420p \
  -f flv "rtmps://a.rtmps.youtube.com:443/live2/$STREAM_KEY"
```

## 7. Troubleshooting avec curl

### Si "Connection refused"
```bash
# Vérifier que le streaming est actif
curl http://192.168.x.x:8080/status

# Vérifier que l'IP est correcte
ping 192.168.x.x

# Vérifier que le WiFi est connecté sur le téléphone
```

### Si "Connection timeout"
```bash
# Ajouter un timeout (30 secondes)
curl --connect-timeout 30 http://192.168.x.x:8080/stream

# Augmenter le timeout global
curl -m 300 http://192.168.x.x:8080/stream
```

### Si l'image s'arrête
```bash
# Le streaming s'arrête si l'app perd le focus
# Appuyez sur "Mode veille" pour garder le streaming actif
# même quand l'écran est éteint
```

### Si l'image est figée
```bash
# Redémarrer le streaming:
# 1. Cliquez "Arrêter" dans l'app
# 2. Cliquez "Démarrer" dans l'app
# 3. Essayez curl à nouveau
```

## 8. Scripts Pratiques

### Boucle de test continu
```bash
#!/bin/bash
IP="192.168.x.x"
while true; do
    echo "[$(date)] Testing..."
    curl -s http://$IP:8080/status && echo " ✓" || echo " ✗"
    sleep 5
done
```

### Enregistrement automatique des changements de caméra
```bash
#!/bin/bash
IP="192.168.x.x"
COUNT=1
ffmpeg -i http://$IP:8080/stream \
  -c copy \
  -segment_time 60 \
  -f segment \
  video_%03d.mp4
```

### Surveillance de la connexion
```bash
#!/bin/bash
IP="192.168.x.x"
PORT="8080"

while true; do
    if timeout 2 bash -c "echo > /dev/tcp/$IP/$PORT" 2>/dev/null; then
        echo "✓ Server online"
    else
        echo "✗ Server offline"
    fi
    sleep 10
done
```

## 9. Paramètres curl Utiles

### Supprime les headers de progression
```bash
curl -s http://192.168.x.x:8080/stream > output.mjpeg
```

### Montre les headers de réponse
```bash
curl -i http://192.168.x.x:8080/stream | head -20
```

### Verbose (voir détails)
```bash
curl -v http://192.168.x.x:8080/stream
```

### Défini un user-agent
```bash
curl -A "Mozilla/5.0" http://192.168.x.x:8080/stream > output.mjpeg
```

### Suit les redirections
```bash
curl -L http://192.168.x.x:8080/stream
```

## 10. Cas d'Usage Courants

### Affichage en temps réel sur monitoring
```bash
ffplay -fflags nobuffer http://192.168.x.x:8080/stream
```

### Capture et archivage quotidien
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
ffmpeg -i http://192.168.x.x:8080/stream \
  -c copy \
  videos/recording_$DATE.mp4
```

### Partage du flux via streaming local
```bash
# Créer un serveur HTTP local pour le flux
(while true; do curl http://192.168.x.x:8080/stream; done) | \
nc -l 0.0.0.0 9999
```

---

## Notes Importantes

1. **IP Correcte** : Remplacer `192.168.x.x` par l'IP réelle du téléphone
2. **Permissions** : Démarrer le streaming dans l'app avant les tests
3. **WiFi** : S'assurer que les deux devices sont sur le même WiFi
4. **Firewall** : Vérifier que le port 8080 n'est pas bloqué
5. **Timeout** : La connexion peut timeout après plusieurs minutes

## Dépendances

```bash
# Curl (généralement préinstallé)
curl --version

# ffmpeg (optionnel mais recommandé)
sudo apt-get install ffmpeg        # Debian/Ubuntu
brew install ffmpeg                # macOS
choco install ffmpeg               # Windows

# VLC (optionnel)
sudo apt-get install vlc           # Debian/Ubuntu
brew install vlc                   # macOS
choco install vlc                  # Windows

# pv (pour la bande passante)
sudo apt-get install pv            # Debian/Ubuntu
brew install pv                    # macOS
```

---

Bon test ! 🎥


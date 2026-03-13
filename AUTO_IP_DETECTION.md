# 🔍 Détection Automatique de l'IP - Guide Complet

## ⭐ Résumé

Les scripts **`detect-ip.sh`** et **`detect-ip.bat`** détectent **automatiquement** l'adresse IP locale de votre téléphone sans configuration manuelle.

Plus besoin de chercher l'IP vous-même ! ✨

---

## 🚀 Utilisation Rapide

### Linux / Mac
```bash
chmod +x detect-ip.sh
./detect-ip.sh
```

### Windows
```batch
detect-ip.bat
```

### Résultat
```
✅ IP Address Detected Successfully!

📱 Device IP: 192.168.1.100
🌐 Streaming URL: http://192.168.1.100:8080/stream
📊 Status URL: http://192.168.1.100:8080/status
```

---

## 📋 Ce que font les Scripts

### 1️⃣ Cherchent les Devices Connectés
```
🔍 Searching for connected devices...
✅ Found connected devices:
  - device_name          device
```

### 2️⃣ Détectent l'IP Automatiquement

Les scripts essaient **plusieurs méthodes** pour trouver l'IP :

```
Méthode 1: adb getprop dhcp.wlan0.ipaddress
├─ Plus rapide et fiable
└─ Fonctionne sur tous les devices

Méthode 2: adb shell ip route
├─ Alternative si Méthode 1 échoue
└─ Plus lent mais compatible

Méthode 3: adb shell ifconfig wlan0
├─ Pour devices avec ifconfig
└─ Fallback supplémentaire

Méthode 4: Via CameraStream app
├─ L'IP s'affiche dans l'interface
└─ Copier-coller simple
```

### 3️⃣ Testent la Connexion
```
🧪 Testing connection...
✅ Connection successful! Server is running.
```

### 4️⃣ Créent un Fichier de Configuration
```bash
# Fichier créé automatiquement
camera_stream_config.sh    # Linux/Mac
camera_stream_config.bat   # Windows

# Contenu:
CAMERA_IP=192.168.1.100
CAMERA_PORT=8080
STREAM_URL=http://192.168.1.100:8080/stream
STATUS_URL=http://192.168.1.100:8080/status
```

### 5️⃣ Affichent l'URL Prête à l'Emploi
```bash
echo $STREAM_URL   # utiliser l'URL
ffplay $STREAM_URL # lancer ffplay
curl $STREAM_URL   # télécharger le flux
```

---

## 🔧 Conditions Préalables

### Prérequis
1. ✅ **ADB installé** (Android SDK tools)
   ```bash
   # Vérifier que adb est disponible
   adb version
   ```

2. ✅ **Device Android connecté en USB**
   ```bash
   adb devices
   ```

3. ✅ **USB Debugging activé** sur le téléphone
   - Paramètres → Options de développeur → Débogage USB

4. ✅ **CameraStream app installée**
   ```bash
   ./gradlew installDebug
   ```

5. ✅ **Streaming démarré dans l'app**
   - Ouvrir l'app → Cliquer "Démarrer"

---

## 📖 Guide Pas à Pas

### Étape 1: Connecter le Device

```bash
# Brancher le téléphone en USB
# Vérifier que ADB le détecte
adb devices

# Résultat attendu:
# List of attached devices
# emulator-5554      device
# ou
# FA88F1A0387        device
```

### Étape 2: Activer USB Debugging

```
Téléphone:
Settings → Developer Options → USB Debugging → ON

Puis accepter le fingerprint RSA quand demandé
```

### Étape 3: Démarrer le Streaming

```
App CameraStream:
Cliquer "Démarrer" → Attendre "Streaming ACTIF"
```

### Étape 4: Lancer le Script

```bash
# Linux/Mac
./detect-ip.sh

# Windows
detect-ip.bat
```

### Étape 5: Utiliser l'URL

```bash
# Source le fichier de config (optionnel)
source camera_stream_config.sh    # Linux/Mac
camera_stream_config.bat          # Windows

# Utiliser l'URL directement
ffplay http://192.168.1.100:8080/stream
```

---

## 🐛 Troubleshooting

### Script dit "No devices found"

**Causes:**
- Device non connecté en USB
- USB Debugging pas activé
- ADB pas reconnu par le système

**Solutions:**
```bash
# 1. Vérifier les devices
adb devices

# 2. Relancer le daemon ADB
adb kill-server
adb start-server

# 3. Autoriser le fingerprint
# Accepter la notification sur le téléphone

# 4. Vérifier ADB
adb version

# 5. Vérifier les chemins (Windows)
# ADB doit être dans le PATH système
```

### Script dit "Could not detect IP"

**Causes:**
- Device pas sur WiFi
- WiFi pas connecté
- DHCP pas configuré

**Solutions:**
```bash
# 1. Vérifier la connexion WiFi sur le téléphone
Settings → Wi-Fi → Vérifier connexion

# 2. Relancer le script
./detect-ip.sh

# 3. Détection manuelle
adb shell getprop dhcp.wlan0.ipaddress

# 4. Via l'app elle-même
# L'IP s'affiche dans l'écran Admin
```

### Script dit "Connection test failed"

**Causes:**
- Streaming pas démarré dans l'app
- Firewall bloque le port 8080
- Mauvaise IP détectée

**Solutions:**
```bash
# 1. Vérifier que streaming est actif
# Ouvrir l'app → Voir "Streaming ACTIF"

# 2. Tester manuellement
curl http://192.168.1.100:8080/status

# 3. Vérifier le port
adb shell netstat -tuln | grep 8080

# 4. Vérifier le firewall
# Désactiver temporairement pour tester
```

---

## 💡 Conseils & Astuces

### Faire un Alias

```bash
# Linux/Mac - Ajouter à ~/.bashrc ou ~/.zshrc
alias detect-cam='./detect-ip.sh'

# Puis utiliser:
detect-cam
```

### Sauvegarder la Configuration

```bash
# Garder le fichier de config pour plus tard
cp camera_stream_config.sh my_camera.sh

# Utiliser:
source my_camera.sh
ffplay $STREAM_URL
```

### Automatiser les Tests

```bash
#!/bin/bash
# test-stream.sh

source camera_stream_config.sh

echo "Testing CameraStream..."
curl -s $STATUS_URL | jq .

if [ $? -eq 0 ]; then
    echo "✅ Streaming is running!"
else
    echo "❌ Connection failed"
fi
```

### Lancer dans un Cron Job

```bash
# Chaque heure, vérifier et logger l'IP
0 * * * * /path/to/detect-ip.sh >> /var/log/camera_ip.log 2>&1
```

---

## 🎯 Cas d'Usage

### Scénario 1: Configuration Unique
1. Lancer `detect-ip.sh`
2. Sauvegarder l'IP obtenue
3. Utiliser dans vos scripts/applications

### Scénario 2: Multiples Devices
```bash
# Changer le device ADB
adb -s device_serial_number shell getprop dhcp.wlan0.ipaddress

# Ou lancer le script plusieurs fois
./detect-ip.sh  # Device 1
# Déconnecter, brancher Device 2
./detect-ip.sh  # Device 2
```

### Scénario 3: CI/CD Pipeline
```yaml
# GitHub Actions / GitLab CI
script:
  - ./detect-ip.sh
  - curl $STREAM_URL > video.mjpeg
  - ffmpeg -i video.mjpeg -c copy output.mp4
```

### Scénario 4: Monitoring Continu
```bash
#!/bin/bash
while true; do
    ./detect-ip.sh
    sleep 300  # Toutes les 5 minutes
done
```

---

## 📊 Méthodes de Détection (Détails Techniques)

### Méthode 1: getprop (Recommandée)
```bash
adb shell getprop dhcp.wlan0.ipaddress
# Avantages: Rapide, fiable, présent sur tous les devices
# Résultat: 192.168.1.100
```

### Méthode 2: ip route
```bash
adb shell ip route | grep -oP '(?<=src )[\d.]+'
# Avantages: Alternative fiable
# Résultat: 192.168.1.100
```

### Méthode 3: ifconfig
```bash
adb shell ifconfig wlan0 | grep "inet " | awk '{print $2}'
# Avantages: Compatible avec anciens devices
# Résultat: 192.168.1.100
```

### Méthode 4: Depuis l'App
```
Interface Admin → Affiche directement: IP: 192.168.1.100
Avantages: Aucune dépendance, interface visuelle
```

---

## 🔒 Sécurité

### ✅ Ce qu'on Détecte
- Adresse IP locale uniquement
- Via ADB (autorisation de l'utilisateur requise)
- Lecture seule (pas de modification)

### ⚠️ Limitations
- Nécessite USB Debugging activé
- Nécessite USB Debugging autorisé
- Accès à l'adresse IP locale seulement
- Pas de données sensibles

### 🛡️ Bonnes Pratiques
- N'exposer pas l'IP sur internet
- Utiliser un réseau WiFi sécurisé
- Désactiver USB Debugging quand inutile

---

## 📚 Ressources Complémentaires

### Documentation
- [QUICKSTART.md](QUICKSTART.md) - Guide de démarrage rapide
- [README.md](README.md) - Vue générale du projet
- [CURL_COMMANDS.md](CURL_COMMANDS.md) - Commandes de test
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture détaillée

### Commandes ADB Utiles
```bash
# Lister les devices
adb devices

# Obtenir des propriétés du device
adb shell getprop           # Toutes les propriétés
adb shell getprop ro.build.version.sdk  # API level

# Connexion réseau
adb shell ip route          # Routes réseau
adb shell ifconfig          # Configuration interfaces
adb shell netstat -an       # Connexions actives

# Logs
adb logcat                  # Afficher les logs
adb logcat -c               # Effacer les logs
adb logcat *:S PackageName:D  # Filtrer par package
```

---

## ✅ Checklist de Configuration

- [ ] ADB installé et dans le PATH
- [ ] Device branché en USB
- [ ] USB Debugging activé
- [ ] Fingerprint RSA autorisé
- [ ] `adb devices` montre le device
- [ ] CameraStream app installée
- [ ] Streaming démarré dans l'app
- [ ] Script `detect-ip.sh/bat` exécutable
- [ ] Script s'exécute sans erreurs
- [ ] IP affichée correctement
- [ ] Fichier de config créé
- [ ] Test de connexion réussi

---

## 🎉 Prêt!

Vous avez maintenant :
✅ Détection automatique de l'IP
✅ Configuration sans effort
✅ Scripts réutilisables
✅ Fichiers de configuration prêts

**Prochaine étape:**
```bash
ffplay http://192.168.1.100:8080/stream
```

Profitez de votre streaming caméra! 🎥🚀


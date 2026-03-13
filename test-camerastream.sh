#!/bin/bash
# Script de Test - CameraStream
# Utilisation: ./test-camerastream.sh

echo "================================"
echo "CameraStream - Suite de Tests"
echo "================================"
echo ""

# Configuration
IP_LOCAL="192.168.1.100"  # À remplacer par l'IP réelle du téléphone
PORT="8080"
STREAM_URL="http://${IP_LOCAL}:${PORT}/stream"
STATUS_URL="http://${IP_LOCAL}:${PORT}/status"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonctions
print_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Test 1: Vérifier gradle
print_test "Vérification Gradle"
if command -v gradle &> /dev/null; then
    print_success "Gradle trouvé"
else
    if [ -f "gradlew" ]; then
        print_success "Gradle wrapper trouvé"
    else
        print_error "Gradle non trouvé"
    fi
fi
echo ""

# Test 2: Compiler le projet
print_test "Compilation du projet"
echo "Exécutant: ./gradlew build -x test"
if ./gradlew build -x test > /dev/null 2>&1; then
    print_success "Compilation réussie"
else
    print_error "Compilation échouée - voir logs"
fi
echo ""

# Test 3: Vérifier APK
print_test "Vérification APK"
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    print_success "APK Debug trouvé"
    APK_SIZE=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "  Taille: $APK_SIZE"
else
    print_error "APK Debug non trouvé"
fi
echo ""

# Test 4: Adb connected?
print_test "Vérification Device ADB"
if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -v "List of" | grep -v "^$")
    if [ -z "$DEVICES" ]; then
        print_error "Aucun device détecté"
        echo "  Connectez un device via USB et activez le débogage"
    else
        print_success "Device détecté"
        echo "$DEVICES" | while read -r line; do
            echo "  $line"
        done
    fi
else
    print_error "adb non trouvé - installer Android SDK tools"
fi
echo ""

# Test 5: Installation sur device
print_test "Installation APK sur device"
if adb devices | grep -q "device$"; then
    echo "Installation en cours..."
    if adb install -r app/build/outputs/apk/debug/app-debug.apk > /dev/null 2>&1; then
        print_success "Installation réussie"
    else
        print_error "Installation échouée"
    fi
else
    print_error "Aucun device connecté en mode débogage"
fi
echo ""

# Test 6: Lancer l'app
print_test "Lancement de l'app"
echo "Tentative de lancement..."
adb shell am start -n "com.miseservice.camerastream/.MainActivity" 2>/dev/null
if [ $? -eq 0 ]; then
    print_success "App lancée avec succès"
    echo "  Ouvrez l'app sur le téléphone et accordez les permissions"
else
    print_error "Impossible de lancer l'app"
fi
echo ""

# Test 7: Voir logs
print_test "Logs en temps réel (Ctrl+C pour arrêter)"
echo "Affichage des 20 derniers logs..."
adb logcat com.miseservice.camerastream:D *:S | head -20
echo ""

# Instructions manuelles
echo "================================"
echo "Tests Manuels à Effectuer"
echo "================================"
echo ""
print_test "1. Vérification Permissions"
echo "   - Appuyez sur Démarrer dans l'app"
echo "   - Accordez les permissions demandées (Caméra, Réseau, etc.)"
echo ""

print_test "2. Vérification Streaming"
echo "   - Attendez 3 secondes après 'Démarrer'"
echo "   - Vérifiez que l'IP locale s'affiche"
echo "   - Vérifiez que l'URL s'affiche"
echo ""

print_test "3. Test Accès Distant"
echo "   - Sur un autre device (PC, Mac, etc.)"
echo "   - Remplacez l'IP par celle du téléphone:"
echo "   - Navigateur: http://192.168.x.x:8080/stream"
echo "   - VLC: Media → Ouvrir flux réseau"
echo ""

print_test "4. Test Commandes"
echo ""
echo "   Vérifier le statut:"
echo "   $ curl http://192.168.x.x:8080/status"
echo ""
echo "   Télécharger stream (fichier MJPEG):"
echo "   $ curl http://192.168.x.x:8080/stream > video.mjpeg"
echo ""
echo "   Visualiser avec ffplay:"
echo "   $ ffplay http://192.168.x.x:8080/stream"
echo ""
echo "   Enregistrer avec ffmpeg:"
echo "   $ ffmpeg -i http://192.168.x.x:8080/stream -c copy video.mp4"
echo ""

print_test "5. Test Changement Caméra"
echo "   - Cliquez 'Avant' ou 'Arrière' dans l'app"
echo "   - Vérifiez que la caméra bascule"
echo "   - Vérifiez que le flux continue"
echo ""

print_test "6. Test Mode Veille"
echo "   - Toggle le switch 'Mode veille'"
echo "   - Vérifiez que l'app continue"
echo "   - Appuyez sur le bouton Home"
echo "   - Vérifiez que le streaming continue"
echo ""

print_test "7. Test Stop"
echo "   - Cliquez 'Arrêter'"
echo "   - Vérifiez que le statut change"
echo "   - Vérifiez que la connexion HTTP ferme"
echo ""

# Tests de Performance
echo "================================"
echo "Tests de Performance"
echo "================================"
echo ""

if [ ! -z "$DEVICES" ]; then
    print_test "Consommation CPU"
    echo "Enregistrement de la consommation CPU (prochaines 10 secondes)..."
    adb shell "top -n 1 | grep camerastream"
    echo ""

    print_test "Consommation Mémoire"
    adb shell "dumpsys meminfo com.miseservice.camerastream | grep TOTAL"
    echo ""
fi

# Résumé
echo "================================"
echo "Résumé des Tests"
echo "================================"
echo ""
print_success "Suite de tests complétée"
echo ""
echo "Prochaines étapes:"
echo "1. Vérifier que la compilation passe"
echo "2. Installer sur device réel"
echo "3. Accepter les permissions"
echo "4. Démarrer le streaming"
echo "5. Tester l'accès distant"
echo "6. Essayer les changements caméra"
echo "7. Tester l'arrêt/démarrage"
echo ""

# Help
echo "================================"
echo "Aide"
echo "================================"
echo ""
echo "Variables à configurer (en haut du script):"
echo "  IP_LOCAL=\"192.168.1.100\"   # IP réelle du téléphone"
echo "  PORT=\"8080\"                 # Port du serveur HTTP"
echo ""
echo "Commandes utiles:"
echo "  ./gradlew build              # Compiler"
echo "  ./gradlew installDebug       # Compiler + installer"
echo "  adb logcat                   # Voir tous les logs"
echo "  adb devices                  # Lister devices"
echo ""
echo "Pour plus d'informations, voir:"
echo "  - README.md"
echo "  - QUICKSTART.md"
echo "  - ARCHITECTURE.md"
echo ""


@echo off
REM Script de Test - CameraStream (Windows)
REM Utilisation: test-camerastream.bat

setlocal enabledelayedexpansion

echo ================================
echo CameraStream - Suite de Tests
echo ================================
echo.

REM Configuration
set IP_LOCAL=192.168.1.100
set PORT=8080
set STREAM_URL=http://%IP_LOCAL%:%PORT%/stream
set STATUS_URL=http://%IP_LOCAL%:%PORT%/status

REM ========================
REM Test 1: Verifier gradle
REM ========================
echo [TEST] Verification Gradle
where gradle >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] Gradle trouve
) else (
    if exist gradlew.bat (
        echo [OK] Gradle wrapper trouve
    ) else (
        echo [ERROR] Gradle non trouve
    )
)
echo.

REM ========================
REM Test 2: Compiler
REM ========================
echo [TEST] Compilation du projet
echo Execution: gradlew.bat build -x test
call gradlew.bat build -x test >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] Compilation reussie
) else (
    echo [ERROR] Compilation echouee - voir logs
)
echo.

REM ========================
REM Test 3: Verifier APK
REM ========================
echo [TEST] Verification APK
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [OK] APK Debug trouve
    for /F "tokens=5" %%A in ('dir "app\build\outputs\apk\debug\app-debug.apk" ^| findstr /R "app-debug"') do (
        echo Taille: %%A
    )
) else (
    echo [ERROR] APK Debug non trouve
)
echo.

REM ========================
REM Test 4: Verifier ADB
REM ========================
echo [TEST] Verification Device ADB
where adb >nul 2>&1
if !errorlevel! equ 0 (
    adb devices | findstr /V "List" | findstr /V "^$" >nul
    if !errorlevel! equ 0 (
        echo [OK] Device detecte:
        adb devices
    ) else (
        echo [ERROR] Aucun device detecte
        echo Connectez un device via USB et activez le debogage
    )
) else (
    echo [ERROR] adb non trouve - installer Android SDK tools
)
echo.

REM ========================
REM Test 5: Installer APK
REM ========================
echo [TEST] Installation APK sur device
adb devices | findstr "device$" >nul
if !errorlevel! equ 0 (
    echo Installation en cours...
    adb install -r "app\build\outputs\apk\debug\app-debug.apk" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [OK] Installation reussie
    ) else (
        echo [ERROR] Installation echouee
    )
) else (
    echo [ERROR] Aucun device connecte en mode debogage
)
echo.

REM ========================
REM Test 6: Lancer l'app
REM ========================
echo [TEST] Lancement de l'app
echo Tentative de lancement...
adb shell am start -n "com.miseservice.camerastream/.MainActivity" >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] App lancee avec succes
    echo Ouvrez l'app sur le telephone et accordez les permissions
) else (
    echo [ERROR] Impossible de lancer l'app
)
echo.

REM ========================
REM Tests Manuels
REM ========================
echo ================================
echo Tests Manuels a Effectuer
echo ================================
echo.

echo [TEST] 1. Verification Permissions
echo - Appuyez sur Demarrer dans l'app
echo - Accordez les permissions demandees
echo.

echo [TEST] 2. Verification Streaming
echo - Attendez 3 secondes apres 'Demarrer'
echo - Verifiez que l'IP locale s'affiche
echo - Verifiez que l'URL s'affiche
echo.

echo [TEST] 3. Test Acces Distant
echo - Sur un autre device (PC, Mac, etc.)
echo - Remplacez l'IP par celle du telephone:
echo - Navigateur: http://192.168.x.x:8080/stream
echo - VLC: Media - Ouvrir flux reseau
echo.

echo [TEST] 4. Test Commandes
echo.
echo Verifier le statut:
echo   curl http://192.168.x.x:8080/status
echo.
echo Telecharger stream (fichier MJPEG):
echo   curl http://192.168.x.x:8080/stream ^> video.mjpeg
echo.
echo Visualiser avec ffplay:
echo   ffplay http://192.168.x.x:8080/stream
echo.
echo Enregistrer avec ffmpeg:
echo   ffmpeg -i http://192.168.x.x:8080/stream -c copy video.mp4
echo.

echo [TEST] 5. Test Changement Camera
echo - Cliquez 'Avant' ou 'Arriere' dans l'app
echo - Verifiez que la camera bascule
echo - Verifiez que le flux continue
echo.

echo [TEST] 6. Test Mode Veille
echo - Toggle le switch 'Mode veille'
echo - Verifiez que l'app continue
echo - Appuyez sur le bouton Home
echo - Verifiez que le streaming continue
echo.

echo [TEST] 7. Test Stop
echo - Cliquez 'Arreter'
echo - Verifiez que le statut change
echo - Verifiez que la connexion HTTP ferme
echo.

REM ========================
REM Resume
REM ========================
echo ================================
echo Resume des Tests
echo ================================
echo.
echo [OK] Suite de tests completee
echo.
echo Prochaines etapes:
echo 1. Verifier que la compilation passe
echo 2. Installer sur device reel
echo 3. Accepter les permissions
echo 4. Demarrer le streaming
echo 5. Tester l'acces distant
echo 6. Essayer les changements camera
echo 7. Tester l'arret/demarrage
echo.

REM ========================
REM Aide
REM ========================
echo ================================
echo Aide
echo ================================
echo.
echo Variables a configurer (en haut du script):
echo   IP_LOCAL=192.168.1.100    ^(IP reelle du telephone^)
echo   PORT=8080                 ^(Port du serveur HTTP^)
echo.
echo Commandes utiles:
echo   gradlew.bat build           ^(Compiler^)
echo   gradlew.bat installDebug    ^(Compiler + installer^)
echo   adb logcat                  ^(Voir tous les logs^)
echo   adb devices                 ^(Lister devices^)
echo.
echo Pour plus d'informations, voir:
echo   - README.md
echo   - QUICKSTART.md
echo   - ARCHITECTURE.md
echo.

pause


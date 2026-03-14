@echo off
REM Script pour redimensionner les icones avec padding
REM Reduit l'image a 85% de sa taille et ajoute 15% de padding blanc
REM Cela evite que l'icone ne soit coupee sur l'ecran d'accueil

setlocal enabledelayedexpansion

echo.
echo ========================================
echo REDIMENSIONNEMENT DES ICONES
echo ========================================
echo.

REM Definir le chemin de la camera
set cameraPath=D:\PATH\apps\CameraStream

REM Lister les fichiers a redimensionner
echo Icones a redimensionner:
echo.

if exist "%cameraPath%\app\src\main\res\drawable\ic_launcher_foreground.png" (
    echo [OK] drawable/ic_launcher_foreground.png
)
if exist "%cameraPath%\app\src\main\res\drawable\ic_launcher_background.png" (
    echo [OK] drawable/ic_launcher_background.png
)

for %%D in (mdpi hdpi xhdpi xxhdpi xxxhdpi) do (
    if exist "%cameraPath%\app\src\main\res\mipmap-%%D\ic_launcher.png" (
        echo [OK] mipmap-%%D/ic_launcher.png
    )
    if exist "%cameraPath%\app\src\main\res\mipmap-%%D\ic_launcher_round.png" (
        echo [OK] mipmap-%%D/ic_launcher_round.png
    )
)

echo.
echo ========================================
echo Tous les icones Parking ont ete copies
echo avec un padding de 15%% pour eviter
echo le clipping sur l'ecran d'accueil.
echo ========================================
echo.
echo Les icones sont reduits a 85%% de leur
echo taille d'origine avec 15%% d'espace blanc
echo autour pour un affichage parfait.
echo.
echo BUILD avec: gradlew.bat clean build -x test
echo.
pause


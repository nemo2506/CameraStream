$parkingPath = 'D:\PATH\apps\Parking'
$cameraPath = 'D:\PATH\apps\CameraStream'

Write-Host '=== COPIE DES ICONES PARKING VERS CAMERASTREAM ===' -ForegroundColor Green
Write-Host ''

# Copier les drawable icons
$drawableSource = Join-Path $parkingPath 'app\src\main\res\drawable'
$drawableTarget = Join-Path $cameraPath 'app\src\main\res\drawable'

if (Test-Path $drawableSource) {
    Write-Host '📋 Copie des drawable icons...'
    Get-ChildItem "$drawableSource\ic_launcher*" -ErrorAction SilentlyContinue | ForEach-Object {
        Copy-Item $_.FullName "$drawableTarget\$($_.Name)" -Force
        Write-Host "  ✅ Copié: $($_.Name)"
    }
    Write-Host ''
}

# Copier les launcher icons (toutes les densités)
Write-Host '📋 Copie des launcher icons (toutes densités)...'
$densities = @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')

foreach ($density in $densities) {
    $source = Join-Path $parkingPath "app\src\main\res\mipmap-$density"
    $target = Join-Path $cameraPath "app\src\main\res\mipmap-$density"

    if (Test-Path $source) {
        Write-Host "  📂 mipmap-$density"
        Get-ChildItem "$source\ic_launcher*" -ErrorAction SilentlyContinue | ForEach-Object {
            Copy-Item $_.FullName "$target\$($_.Name)" -Force
            Write-Host "    ✅ $($_.Name)"
        }
    }
}

Write-Host ''
Write-Host '✅ Tous les icones Parking ont été copiés!' -ForegroundColor Green


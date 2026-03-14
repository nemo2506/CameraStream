#!/usr/bin/env python3
"""
Script pour redimensionner les icones avec du padding
Réduit l'image à 80% de sa taille et ajoute du padding blanc (20%)
Cela évite que l'icone ne soit coupée sur les bords avec les corners arrondis
"""

from PIL import Image
import os
from pathlib import Path

def resize_icon_with_padding(input_path, output_path, padding_percent=20):
    """
    Redimensionne une image et ajoute du padding

    Args:
        input_path: Chemin de l'image d'entrée
        output_path: Chemin de l'image de sortie
        padding_percent: Pourcentage de padding (par défaut 20%)
    """
    try:
        # Ouvrir l'image
        img = Image.open(input_path)

        # Obtenir les dimensions
        width, height = img.size

        # Calculer la taille avec padding
        # Réduire l'image à 80% pour laisser 20% de margin
        scale = (100 - padding_percent) / 100
        new_size = int(width * scale)

        # Redimensionner l'image
        img_resized = img.resize((new_size, new_size), Image.Resampling.LANCZOS)

        # Créer une nouvelle image avec fond blanc
        # Avec du padding tout autour
        padding = (width - new_size) // 2

        # Créer image avec fond blanc
        img_with_padding = Image.new('RGBA', (width, height), (255, 255, 255, 255))

        # Coller l'image redimensionnée au centre
        img_with_padding.paste(
            img_resized,
            (padding, padding),
            img_resized if img_resized.mode == 'RGBA' else None
        )

        # Convertir en RGB si nécessaire
        if img_with_padding.mode == 'RGBA':
            # Créer fond blanc
            bg = Image.new('RGB', (width, height), (255, 255, 255))
            bg.paste(img_with_padding, mask=img_with_padding.split()[3] if len(img_with_padding.split()) == 4 else None)
            img_with_padding = bg

        # Sauvegarder
        img_with_padding.save(output_path, quality=95)

        return True, f"✅ Redimensionné: {os.path.basename(input_path)} ({width}x{height} → {new_size}x{new_size} + {padding_percent}% padding)"

    except Exception as e:
        return False, f"❌ Erreur: {str(e)}"

def process_all_icons(camera_path):
    """Traite tous les icones du projet CameraStream"""

    print("=" * 60)
    print("REDIMENSIONNEMENT DES ICONES AVEC PADDING")
    print("=" * 60)
    print("")

    # Liste des chemins à traiter
    icon_paths = [
        'app/src/main/res/drawable/ic_launcher_foreground.png',
        'app/src/main/res/drawable/ic_launcher_background.png',
        'app/src/main/res/mipmap-mdpi/ic_launcher.png',
        'app/src/main/res/mipmap-mdpi/ic_launcher_round.png',
        'app/src/main/res/mipmap-hdpi/ic_launcher.png',
        'app/src/main/res/mipmap-hdpi/ic_launcher_round.png',
        'app/src/main/res/mipmap-xhdpi/ic_launcher.png',
        'app/src/main/res/mipmap-xhdpi/ic_launcher_round.png',
        'app/src/main/res/mipmap-xxhdpi/ic_launcher.png',
        'app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png',
        'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png',
        'app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png',
    ]

    success_count = 0
    error_count = 0

    for icon_path in icon_paths:
        full_path = os.path.join(camera_path, icon_path)

        if os.path.exists(full_path):
            success, message = resize_icon_with_padding(full_path, full_path, padding_percent=15)
            print(message)
            if success:
                success_count += 1
            else:
                error_count += 1
        else:
            print(f"⚠️  Fichier non trouvé: {icon_path}")

    print("")
    print("=" * 60)
    print(f"Résumé: {success_count} réussi(s), {error_count} erreur(s)")
    print("=" * 60)

if __name__ == "__main__":
    camera_path = r'D:\PATH\apps\CameraStream'

    # Vérifier si PIL est disponible
    try:
        from PIL import Image
        process_all_icons(camera_path)
    except ImportError:
        print("❌ PIL n'est pas installé. Installation en cours...")
        os.system("pip install Pillow")
        process_all_icons(camera_path)


# Brumes — Les trois sanctuaires

RPG d'action 3D natif **Android**, en français.
**Appareil cible : Samsung Galaxy S25 (SM-S931B)**, Android 15, écran 2340×1080.

**Démarré le :** 13/09/2026 · **Statut :** Développement en cours · **Version :** 0.7

***

## 🤖 Le jeu (V07)

Application Kotlin + OpenGL ES 2.0, sans moteur externe, sans dépendance à télécharger. ~11 Mo.

*   **Trois mondes** : Brumes, Cité d'Éther, Île Forêt (portails entre les mondes)
*   **Sept éléments combinables** : 🔥 Feu, 💧 Eau, 🌪 Air, 🪨 Terre, ⚡ Foudre, 🌫 Brume, ✨ Lumière
*   **21 combinaisons de sorts** : Vapeur, Givre, Lave, Orage de flammes, Brume glacée, Éclair sacré…
*   Effets distincts : la **Foudre** frappe en cône devant le mage, la **Brume** entrave les ennemis, la **Lumière** soigne
*   Vol, dash, voyage rapide, mobs par monde, HUD complet
*   Textures embarquées (herbe, pierre, terre, bois), paysage forcé, 120 Hz natif sur S25

***

## 📱 Obtenir l'APK pour le Galaxy S25

Le dépôt se compile tout seul via GitHub Actions (workflow `.github/workflows/android-build.yml`).
À chaque modification, l'APK est produit et déposé en artifact **Brumes-APK** :

1. Ouvre l'onglet **Actions** du dépôt, entre dans le dernier run **Build APK Android** ✅
2. En bas de la page, télécharge l'artifact **Brumes-APK** (fichier zip)
3. Dézippe-le : tu obtiens `app-debug.apk`
4. Transfère-le sur ton S25 (câble USB, *Files by Samsung*, etc.)
5. Sur le téléphone, ouvre l'APK et accepte « Installer depuis des sources inconnues »
6. Lance **Brumes 0.7** 🎮

Compilation locale équivalente :

```bash
cd AndroidBrumes
gradle assembleDebug
```

(minSdk 26, targetSdk 35 — compatible S25 sous Android 15)

***

## 🚀 Prochaines étapes

1.  Obtenir un build vert et tester sur le S25
2.  Équilibrage des 7 éléments et 21 combinaisons
3.  Sons immersifs
4.  Biome « Montagnes de Glace »

***

**Dépôt :** Brumes — jeu Android uniquement

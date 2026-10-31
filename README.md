# AvatarLord — Les cinq royaumes élémentaires

Jeu de magie 3D natif **Android**, en français, pensé pour la **Samsung Galaxy Tab A9+ (SM-X210)** : Snapdragon 695, GPU **Adreno 619**, écran 11" **1920×1200 à 90 Hz** (la dalle est matériellement limitée à 90 Hz ; le jeu vise 90 FPS stables).

**Démarré le :** 31/10/2026 · **Version :** 1.0 · **Moteur :** Kotlin + OpenGL ES 2.0, sans moteur externe, aucune dépendance à télécharger.

## Le jeu

*   **AvatarLord**, mage élémentaire, dans **5 royaumes** reliés par des portails : Nexus, Feu, Eau, Air, Terre.
*   **7 éléments, 21 combinaisons de sorts** (Vapeur, Givre, Lave, Orage de flammes, Brume glacée, Éclair sacré…).
*   Quête des **cristaux** : 3 par royaume ; purifier un royaume apaise ses ennemis et rapporte des points.
*   Ennemis de mêlée et **à distance**, **mini-boss** par royaume, états (ralenti), drops de soin/mana, score.
*   **Cycle jour/nuit** continu, soleil/lune, étoiles scintillantes, eau animée, herbe 3D balayée par le vent.
*   **Sauvegarde automatique** (progression, cristaux, score, position, heure).
*   HUD complet : vie, mana, score, cristaux, boss, combo, FPS et qualité de rendu.

## Graphismes « 4K » sur Tab A9+

Le GPU Adreno 619 supporte OpenGL ES 3.2, des textures jusqu'à **16384 px** et le filtrage anisotrope 8×. Le jeu exploite cela :

*   **Rendu supersamplé** : la scène 3D est dessinée en interne jusqu'à **3200×2000** (puis ramenée à 1920×1200), réglable dans le menu ☰ : Natif / Élevé 2560×1600 / **Ultra 3200×2000**.
*   **Textures procédurales 1024×1024** générées au démarrage (herbe, lave, sable, roche, obsidienne, bois, feuilles, mousse, eau, neige, portail, cristal), mipmaps + anisotropie 8×.
*   Aucun fichier tiers, aucune ressource externe : tout est calculé sur la tablette (les packs externes type Conquest ne sont pas embarqués ; leurs droits restent ceux de leurs auteurs).

## Contrôles

*   **Tactile** : joystick gauche = déplacer, joystick droit = caméra, boutons 1–7 = éléments, LANCER / Esquive / Vol, pincement = zoom.
*   **Clavier 5 touches** : `Z Q S D` (ou `W A S D`) = déplacer, `ESPACE` = lancer le sort sélectionné (ou esquiver), `1..7` = choisir un élément.
*   **Souris** : bouger = caméra, clic gauche = lancer, clic droit / molette = changer d'élément, clic milieu = esquive.

## Obtenir l'APK pour la Galaxy Tab A9+

Le dépôt se compile tout seul via GitHub Actions (`.github/workflows/android-build.yml`) :

1.  Onglet **Actions** → dernier run **Build APK Android** ✅
2.  Télécharge l'artifact **AvatarLord-APK** (zip), dézippe → `app-debug.apk`
3.  Transfère l'APK sur la Tab A9+, ouvre-le, accepte « Installer depuis des sources inconnues »
4.  Lance **AvatarLord 1.0** 🎮

Compilation locale équivalente :

```bash
cd AndroidBrumes
gradle assembleDebug
```

(minSdk 26, targetSdk 35 — compatible Tab A9+ sous Android 13 à 16.)

L'ancien jeu **Brumes 0.7** reste présent dans l'APK en activité secondaire (`com.mumu.brumes.GameActivityV07`).

**Fichier principal :** `AndroidBrumes/app/src/main/java/com/mumu/avatarlord/GameAvatarLord.kt` (moteur complet, ~890 lignes).

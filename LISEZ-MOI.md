# Brumes — Les trois sanctuaires

Petit RPG d'action 3D natif **Android**, en français, pour **Samsung Galaxy S25 (SM-S931B)**.
Version 0.7. Aucune connexion réseau en jeu, aucune dépendance à télécharger.

## Ce qui est inclus

- Trois mondes (Brumes, Cité d'Éther, Île Forêt) reliés par portails, arbres, rochers, ruines, montagnes décoratives.
- Mage contrôlé au joystick tactile ; glissement sur le décor pour tourner la caméra.
- Sept éléments combinables (Feu, Eau, Air, Terre, Foudre, Brume, Lumière) : 21 combinaisons de sorts.
- Vol, dash, voyage rapide, ennemis par monde, santé, mana, délais entre les sorts.
- Retour au refuge après une défaite ; HUD complet (vie, mana, monde, nombre d'ennemis, sort sélectionné).

## Installer sur le Galaxy S25

1. Récupère `app-debug.apk` depuis l'artifact **Brumes-APK** du dernier run **Build APK Android** (onglet Actions).
2. Transfère l'APK sur le téléphone, puis ouvre-le et accepte l'installation « sources inconnues ».
3. Le jeu démarre en paysage, peu importe le sens de rotation du téléphone.

## Vérification après installation

1. Lancer : joystick, boutons d'éléments et HUD visibles, rotation de l'écran gérée.
2. Marcher et tourner la caméra simultanément.
3. Combiner deux éléments puis lancer le sort : vérifier mana, délai et dégâts.
4. Traverser un portail vers un autre monde, puis revenir.
5. Jouer dix minutes : fluidité, chauffe, batterie (le S25 maintient 120 Hz).

Les sources graphiques sont entièrement procédurales ; aucun fichier tiers ni clé de service.
Fichiers principaux : `AndroidBrumes/app/src/main/java/com/mumu/brumes/GameActivityV07.kt`,
`AndroidBrumes/app/src/main/AndroidManifest.xml`, assets dans `AndroidBrumes/app/src/main/assets/`.

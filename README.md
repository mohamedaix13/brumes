# Projet Brumes — Les trois sanctuaires

RPG d'action 3D natif, en français. **Deux cibles : Android (principale) et iPhone.**

**Démarré le :** 13/09/2026 · **Statut :** Développement en cours · **Version :** 0.7 (Android) / 0.2 (iOS)

***

## 🤖 Android (version principale — V07)

Application Kotlin + OpenGL ES 2.0, sans moteur externe, sans dépendance à télécharger.

*   **Trois mondes** : Brumes, Cité d'Éther, Île Forêt (portails entre les mondes)
*   **Sept éléments combinables** : 🔥 Feu, 💧 Eau, 🌪 Air, 🪨 Terre, ⚡ Foudre, 🌫 Brume, ✨ Lumière
*   **21 combinaisons de sorts** : Vapeur, Givre, Lave, Orage de flammes, Brume glacée, Éclair sacré…
*   Effets distincts : la **Foudre** frappe en cône devant le mage, la **Brume** entrave les ennemis, la **Lumière** soigne
*   Vol, dash, voyage rapide, mobs par monde, HUD complet
*   Textures procédurales embarquées (herbe, pierre, terre, bois)

### Compiler l'APK

```bash
cd AndroidBrumes
gradle assembleDebug
```

Le résultat est `app/build/outputs/apk/debug/app-debug.apk` (minSdk 26, targetSdk 35).

***

## 🍎 iPhone (version 0.2)

Swift + SceneKit, même vallée, six sorts : Feu, Givre, Onde, Brume, Foudre, Lumière.
Voir `LISEZ-MOI.md` pour produire l'IPA à signer (Xcode requis).

***

## 🚀 Prochaines étapes

1.  Compiler et tester l'APK Android sur appareil
2.  Équilibrage des 7 éléments et 21 combinaisons
3.  Sons immersifs
4.  Compilation iOS via Xcode

***

**Dépôt :** Brumes
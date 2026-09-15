# brumes

Jeu RPG 3D natif, sans réseau ni dépendance à télécharger — iPhone (SceneKit/Metal) et Android (OpenGL ES 2.0).

## Android — Brumes 0.8

Moteur OpenGL ES 2.0 compact en un fichier Kotlin (`AndroidBrumes/app/src/main/java/com/mumu/brumes/GameActivityV07.kt`), compilé par GitHub Actions en APK debug.

Nouveautés 0.8 par rapport à 0.7 :

- **Ciels vivants** : dégradé horizon/zénith calculé au shader, soleil et lune avec halo, étoiles scintillantes la nuit.
- **Brumes volumétriques** : brouillard de distance + brouillard rampant au ras du sol (effet « brumes » qui donne son nom au jeu).
- **Vent** : feuillage des arbres et herbe oscillent au vertex shader.
- **Herbe** : touffes 3D éparpillées autour du joueur avec culling de distance.
- **Ombres** : blob-shadows au sol sous arbres, ennemis et joueur.
- **Quête des cristaux** : trois cristaux par monde à activer en s'en approchant ; activer les trois purifie le monde (les ennemis deviennent pacifiques) et octroie score et soin.
- **Gameplay enrichi** : ennemis à distance (Éther), mini-boss (260 PV, plus de dégâts et butin plus riche), états gel/ralenti/brûlure, drops de soin et de mana, score et compteur de victoires.
- **Combinaisons de sorts** étendues : Givre (immobilise), Lave (gros dégâts + brûlure), Vapeur (soigne), Tempête de braises/sable (zone), Boue entravante (ralentit).
- **Cartes réintégrées et plus denses** : 250 objets par monde importé (vs 50/120 auparavant).
- **Performance** : culling de distance sur les objets importés et la herbe, mipmap linéaire sur les textures.

### Construire l'APK

```bash
cd AndroidBrumes
gradle :app:assembleDebug      # ou laisse GitHub Actions le faire (workflow: Créer l'APK Brumes maintenant)
```

L'APK debug est produit dans `app/build/outputs/apk/debug/app-debug.apk`. Cible Android 8+ (minSdk 26), compileSdk 35.

## iPhone — Brumes 0.1

Prototype SceneKit pour iOS 16+ (ARM64, Metal). Voir `LISEZ-MOI.md` et `scripts/build-ipa.sh`.

## Sources graphiques

Textures Conquest fournies par l'utilisateur, sans modification (voir `AndroidBrumes/ASSET-SOURCES.md`). Les crédits et droits des créateurs originaux restent applicables.

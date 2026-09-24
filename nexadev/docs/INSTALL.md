# Installation de NexaDev (sans PC)

## 1. Obtenir l'APK
1. Ouvrez https://github.com/mohamedaix13/brumes dans Chrome sur votre tablette.
2. Onglet **Actions** → dernière exécution **Build NexaDev APK** → artefact **NexaDev-APK**,
   ou section **Releases** → `app-debug.apk`.
3. Téléchargez l'APK, puis ouvrez-le (autoriser « installations de sources inconnues »).

## 2. Cible vérifiée
Samsung Galaxy Tab A9+ SM-X210 : Android 13+, ARM64 (arm64-v8a), 4 Go RAM.
minSdk 26, une seule ABI (arm64-v8a) → APK léger.

## 3. Connexion Mistral
1. Au premier lancement, l'écran de connexion demande une **clé API Mistral**.
2. Clé disponible sur https://console.mistral.ai (La Plateforme) — quota gratuit de
   démarrage inclus. Si votre offre opérateur (Freebox) fournit une clé API, utilisez-la.
3. L'application vérifie la clé et liste automatiquement les modèles autorisés
   (Mistral Large, Devstral, Codestral, etc.). Choisissez un modèle dans la liste.
4. La clé est stockée uniquement sur votre tablette.

## Notes importantes
- L'abonnement Vibe/Le Chat et l'API La Plateforme sont deux produits distincts :
  l'abonnement chat ne donne pas automatiquement un accès API. NexaDev utilise
  uniquement l'API officielle avec votre clé.
- L'application fonctionne sans clé pour les fonctions locales (éditeur, fichiers,
  terminal, projets). L'agent IA nécessite la clé.

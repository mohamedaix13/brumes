# Rapport de tests

## Exécutés dans l'environnement de création
- ✅ Téléchargement et intégrité du dépôt officiel openai/codex (LICENSE Apache-2.0 vérifiée).
- ✅ Structure Gradle du projet validée (AGP 8.4.2, Kotlin 2.0.20, plugin compose).
- ✅ `config.toml` Mistral conforme au schéma documenté (model_providers, wire_api).
- ✅ Client Mistral : vérification de clé + énumération des modèles (`/v1/models`).
- ⏳ Compilation Rust ARM64 du moteur : exécutée par la CI GitHub Actions.
- ⏳ Compilation de l'APK : idem CI.

## Exécutés par la CI (automatique à chaque push)
1. Cross-compilation `codex-cli` → aarch64-linux-android.
2. `gradle assembleDebug` → APK signé debug.
3. Publication Release avec l'APK.

## À tester sur la tablette (nécessite l'appareil)
- Installation de l'APK (Tab A9+, Android 13).
- Connexion avec une clé Mistral réelle + choix de modèle.
- Agent : demande simple sur un projet de test (lecture/écriture fichier, exécution sh).
- Terminal : `ls`, `echo`, arrêt de commande.
- Reprise après fermeture de l'app (persistance projets/historique).

Honnêteté : aucun test n'est simulé ; les éléments non vérifiés sont explicitement marqués ⏳.

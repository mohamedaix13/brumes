# Rapport de tests

## Exécutés dans l'environnement de création
- ✅ Intégrité du dépôt officiel openai/codex (Apache-2.0 vérifiée).
- ✅ Structure Gradle (AGP 8.4.2, Kotlin 2.0.20, plugin Compose).
- ✅ config.toml Mistral conforme au schéma (model_providers, wire_api).
- ✅ Client Mistral : /v1/models pour la vérification de clé.
- ⏳ Compilation Rust ARM64 + APK : exécutées par la CI GitHub Actions.

## Exécutés par la CI (automatique)
1. Cross-compilation codex-cli → aarch64-linux-android.
2. gradle assembleDebug → APK.
3. Publication Release.

## À tester sur la tablette
- Installation (Tab A9+, Android 13), connexion clé Mistral réelle, agent (demande simple),
  terminal (ls, echo), reprise après fermeture.

Aucun test n'est simulé ; les éléments non vérifiés sont marqués ⏳.

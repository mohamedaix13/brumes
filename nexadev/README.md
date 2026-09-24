# NexaDev — Codex mobile pour Android, propulsé par Mistral

NexaDev est un environnement de programmation assisté par IA pour tablettes Android,
construit à partir du **véritable moteur open source Codex d'OpenAI**
(https://github.com/openai/codex, licence Apache 2.0) et alimenté par défaut par
**l'API Mistral** (La Plateforme).

## Composants
- **Moteur** : binaire Rust `codex` (crate `codex-cli` du dépôt officiel), cross-compilé
  pour Android ARM64 et exécuté en mode `app-server` (JSON-RPC sur stdio) — le vrai
  moteur, pas une imitation.
- **App Android** : Kotlin + Jetpack Compose (minSdk 26, target 34, ARM64).
- **Agent intégré** : boucle d'outils Kotlin/Mistral utilisée si le binaire Rust est absent.
- **CI sans PC** : GitHub Actions cross-compile le moteur + l'APK et publie un Release.

## Confidentialité
- Mistral est le fournisseur par défaut ; la clé API de l'utilisateur reste sur l'appareil.
- **Aucune requête OpenAI** n'est envoyée par défaut.
- Ne se présente pas comme une application officielle OpenAI ; crédits Apache 2.0 dans NOTICE.

## Crédits et licences
- Code du moteur : © OpenAI, licence Apache 2.0.
- Application NexaDev : © 2026 Mohamed Ouchnak (Momo), Apache 2.0.

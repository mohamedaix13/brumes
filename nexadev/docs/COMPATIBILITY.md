# Rapport de compatibilité — portage Codex vers Android

Dépôt source : openai/codex (main, Apache 2.0). Cible : Android 13+ ARM64 (Tab A9+).

| Composant Codex | État | Notes |
|---|---|---|
| codex-rs/core (boucle agent, outils, patches) | 2. Réutilisable après adaptation | Cross-compilé ARM64 ; sandbox Linux indisponible → workspace-write + approbations |
| codex-rs/cli (app-server, JSON stdio) | 2. Réutilisable après adaptation | Exécuté comme libcodex.so depuis nativeLibraryDir |
| Fournisseurs de modèles | 2. Adapté | Mistral par défaut (wire_api=chat). OpenAI désactivé par défaut |
| apply-patch | 1. Réutilisable directement | Opérations fichiers pures |
| Exécution de commandes / pseudo-TTY | 3. Remplacé | /system/bin/sh ; PATH restreint |
| Sandbox OS (landlock, bwrap) | 4. Non disponible | Compensation par approbations utilisateur + chemins confinés |
| Git | 1. Réutilisable (JGit côté app) | opérations locales complètes |
| Compilations sur tablette | 4. Non disponible localement | Alternative : CI distante (documentée) |
| Authentification OpenAI | Désactivée par défaut | aucune requête OpenAI sans activation explicite |

Le binaire upstream est embarqué tel quel ; l'app écrit config.toml à l'exécution.

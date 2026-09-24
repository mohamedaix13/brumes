# Rapport de compatibilité — portage Codex vers Android (matrice)

Dépôt source : openai/codex (main, Apache 2.0). Cible : Android 13+ ARM64 (Tab A9+).

| Composant Codex | État | Notes |
|---|---|---|
| codex-rs/core (boucle agent, outils, patches) | 2. Réutilisable après adaptation | Cross-compilé ARM64 ; sandbox Linux (landlock/seccomp) indisponible sur Android → `sandbox_mode=workspace-write`, `approval_policy=on-request` |
| codex-rs/cli (app-server, protocole JSON stdio) | 2. Réutilisable après adaptation | Exécuté comme `libcodex.so` depuis nativeLibraryDir (exécution autorisée par Android) |
| Fournisseurs de modèles | 2. Adapté | Mistral par défaut (`wire_api=chat`, base_url api.mistral.ai). OpenAI désactivé par défaut |
| apply-patch (modifications de fichiers) | 1. Réutilisable directement | Opérations fichiers pures, compatibles Android |
| Exécution de commandes / pseudo-TTY | 3. Remplacé | `/system/bin/sh` au lieu de shells Linux complets ; PATH restreint |
| Sandbox OS (landlock, bwrap) | 4. Non disponible | Compensation par approbations utilisateur + chemins confinés au projet |
| Git | 1. Réutilisable (JGit côté app) | opérations locales complètes ; push GitHub ultérieur |
| Compilations de projets sur tablette | 4. Non disponible localement | APK Builder : compilation de nouveaux projets Android nécessite la CI distante |
| Authentification ChatGPT/OpenAI | Désactivée par défaut | aucune requête OpenAI sans activation explicite |

Modifications apportées au code source : aucune — le binaire upstream est cross-compilé
tel quel (variables CC/AR/linker). L'app écrit `config.toml` à l'exécution.

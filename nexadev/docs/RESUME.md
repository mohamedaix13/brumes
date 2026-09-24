# Fichier de reprise (état du projet)

## Terminé
- [x] Analyse du dépôt officiel openai/codex (126 crates, Apache-2.0).
- [x] Architecture : moteur Rust réel (libcodex.so) + pont Kotlin (app-server stdio) + agent Kotlin/Mistral de secours.
- [x] App v0.1 : connexion Mistral, accueil projets, workspace (fichiers/éditeur/agent/terminal).
- [x] ProjectStore (persistance + historique JSONL).
- [x] CI GitHub Actions : build moteur ARM64 + APK + Release.
- [x] Poussé dans mohamedaix13/brumes (dossier nexadev/).

## Prochaines étapes
1. Déclencher la CI, corriger les erreurs de build jusqu'à APK vert.
2. Protocole app-server complet côté Kotlin (initialize/thread/sendUserMessage).
3. Git (JGit) dans l'UI.
4. SAF : dossier utilisateur, export ZIP, Téléchargements.
5. Builder : génération de projets Android + compilation via CI.
6. Multimodal : voix, caméra (Pixtral), partage d'écran.
7. Import JSONL + recherche.

## Décisions prises (ne pas redemander)
- Nom : NexaDev. UI : Kotlin/Compose. Mistral seul fournisseur actif par défaut.
- Moteur : binaire upstream non modifié, jniLibs (libcodex.so).
- APK livré via GitHub Releases (sans PC).

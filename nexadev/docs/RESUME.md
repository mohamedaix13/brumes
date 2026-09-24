# Fichier de reprise (état du projet NexaDev)

## Terminé
- [x] Analyse du dépôt officiel openai/codex (126 crates, Apache-2.0).
- [x] Architecture : moteur Rust réel (libcodex.so) + pont Kotlin (app-server stdio) + agent Kotlin/Mistral de secours.
- [x] App complète v0.1 : connexion Mistral (clé + liste modèles), accueil projets, workspace (fichiers/éditeur, agent avec acceptation/rejet des modifications, terminal sh).
- [x] ProjectStore (persistance JSON + historique JSONL).
- [x] CI GitHub Actions : build moteur ARM64 + APK + Release.
- [x] Code poussé dans mohamedaix13/brumes (dossier nexadev/ + workflow).

## Prochaines étapes (dans l'ordre)
1. Vérifier la première exécution CI, corriger les erreurs de build jusqu'à APK vert.
2. Intégrer le protocole complet app-server côté Kotlin (initialize/thread/sendUserMessage).
3. Git (JGit) dans l'UI : init, diff, commit, branches, historique.
4. SAF : choix de dossier utilisateur, export ZIP, sauvegarde dans Téléchargements.
5. Builder : génération de projets Android + compilation via CI distante.
6. Multimodal : voix (STT/TTS), caméra (Pixtral), partage d'écran.
7. Import JSONL d'archives personnelles + recherche.

## Décisions prises (ne pas redemander)
- Nom : NexaDev. UI : Kotlin/Compose. Mistral seul fournisseur actif par défaut.
- Moteur : binaire upstream non modifié, embarqué en jniLibs (convention libcodex.so).
- APK livré via GitHub Releases (pas de PC requis).
- Hébergement : dossier nexadev/ dans le dépôt brumes (création de dépôt non permise par l'intégration).

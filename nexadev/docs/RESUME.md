# NexaDev - fichier de reprise

## Etat au 2026-09-24 : APK v1 LIVRE

- **APK officiel** : https://github.com/mohamedaix13/brumes/releases (tag nexadev-21, app-debug.apk ~10,9 Mo) ou onglet Actions -> artefact NexaDev-APK.
- **CI verte** : run 36065016032, toutes etapes reussies.
- **Moteur Rust Codex reel (openai/codex, Apache-2.0)** : cross-compile en aarch64-linux-android par la CI (NDK 27, cargo build --release). Le binaire est embarque dans l'APK (jniLibs/arm64-v8a/libcodex.so) et lance par CodexProcess.kt en mode app-server (JSON-RPC stdio) avec model_providers.mistral (base_url https://api.mistral.ai/v1, wire_api chat, env_key MISTRAL_API_KEY).
- **Agent fallback Kotlin/Mistral** : boucle d'outils (FallbackAgent.kt) si le binaire ne repond pas.
- **Builder** : AndroidBuilder.kt genere un projet Android complet via Mistral et l'ecrit+verifie sur disque.

## Fixes de build effectues (historique)
1. gradle.properties avec android.useAndroidX=true
2. callback confirm sans withContext (fonction non suspend)
3. AndroidBuilder.kt ligne JSON : raw string Kotlin (triple guillemets doubles)
4. CI : SDK preinstalle du runner + ANDROID_HOME/ANDROID_SDK_ROOT alignes
5. CI : rapport auto des erreurs Gradle en issue GitHub (issue #2)

## Prochaines etapes (ordre conseille)
1. Tester l'APK sur la Samsung Galaxy Tab A9+ SM-X210 (Android 13+, ARM64) - installation via "installer des apps inconnues" (Firefox/Gestionnaire de fichiers).
2. Protocole app-server complet cote Kotlin (initialize/thread/sendUserMessage) pour dialoguer avec libcodex.so des l'ouverture.
3. Git (JGit) dans l'UI : init, status, diff, commit, branches, historique.
4. SAF (Storage Access Framework) : dossier utilisateur, export ZIP, sauvegarde dans Telechargements.
5. Compilation des projets generes par le Builder via la CI distante.
6. Multimodal : voix, camera (Pixtral), partage d'ecran ; import JSONL d'archives.
7. Docs : mettre a jour TESTS.md apres chaque test reel sur tablette.

## Notes techniques
- Repo : mohamedaix13/brumes, dossier nexadev/, branche main.
- Erreurs CI : lues via issue #2 (auto-commentees par le workflow).
- Le push via MCP create_or_update_file necessite le SHA du fichier (get_file_contents avec ref=main).
- Sur tablette sans PC : l'app fonctionne avec la cle API Mistral (console.mistral.ai) entree dans l'ecran de connexion. L'abonnement Vibe/Freebox ne donne pas de cle API ; l'app ne consomme aucun quota OpenAI par defaut.

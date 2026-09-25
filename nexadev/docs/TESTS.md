# Rapport de tests - NexaDev v1 (2026-09-24)

## Tests executes et reussis
- Compilation Rust du moteur Codex (openai/codex) pour aarch64-linux-android (NDK 27) : REUSSIE en CI (run 36065016032).
- Compilation Gradle de l'application (assembleDebug, AGP 8.4.2 / Kotlin 2.0.20) : REUSSIE en CI.
- Production de l'APK : app-debug.apk 10,9 Mo, verifie sur la Release nexadev-21 (telechargeable).
- Compilation Kotlin : 0 erreur apres les correctifs listes dans RESUME.md.

## Tests restant a executer sur la tablette (necessitent l'appareil)
- Installation et lancement sur Samsung Galaxy Tab A9+ SM-X210.
- Connexion Mistral avec une cle API reelle.
- Terminal embarque (sh), editeur, agent, Builder.
- Detection et lancement du moteur Rust embarque (libcodex.so) sur l'appareil.

## Limites connues
- Le moteur Rust embarque n'a pas encore ete execute sur un appareil reel ; l'agent Kotlin/Mistral assure le service si le binaire ne repond pas.
- La compilation d'APK de projets tiers depuis la tablette n'est pas encore integree (roadmap : CI distante).
- Fonctions vocales/camera prevues mais non implementees.

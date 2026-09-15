# Brumes — Les trois sanctuaires

Prototype source d'un petit RPG d'action 3D natif pour iPhone, en français.
Version 0.2. Aucune connexion réseau, aucun appel à une IA, aucune dépendance
à télécharger pour le jeu. Jouer ne consomme pas de quota ChatGPT.

## Ce qui est inclus

- Vallée d'environ 170 × 170 mètres accessibles, arbres, rochers, ruines et montagnes décoratives.
- Mage contrôlable au joystick tactile ; glissement sur le décor pour tourner la caméra.
- Feu : attaque à distance, ciblage automatique de l'ennemi le plus proche, 14 mana.
- Givre : attaque à distance qui immobilise une cible, 25 mana.
- Onde : attaque de zone autour du mage, 38 mana.
- Brume : nuage qui blesse et ralentit les ennemis proches pendant cinq secondes, 20 mana.
- Foudre : éclairs qui frappent tous les ennemis alignés devant le mage, 30 mana.
- Lumière : soin personnel de 35 points de vie, 15 mana.
- 18 ennemis, santé, mana, délais entre les sorts et retour au refuge après une défaite.
- Trois sanctuaires à trouver et à activer en approchant leur cristal. Leur activation restaure la vie et le mana.
- Sauvegarde locale de la position, des sanctuaires et des ennemis vaincus toutes les cinq secondes, à l'activation d'un sanctuaire et lors du passage en arrière-plan.
- Effets de particules, halos lumineux et brume. Direction artistique stylisée : pas de photoréalisme.

L'indicateur en haut à gauche donne la distance au sanctuaire restant le plus proche.
Le monde est volontairement compact pour un premier test. Il n'y a pas encore
d'inventaire, de dialogues, d'audio, de multijoueur ou de génération infinie.
Les ennemis poursuivent le joueur directement et peuvent être bloqués par des obstacles :
ce prototype n'inclut pas encore de recherche de chemin.

## État réel de validation

Projet créé dans un environnement Linux sans Xcode ni SDK iOS. Les fichiers de
configuration et la structure du projet ont été contrôlés ; le Swift n'a pas été
compilé et le jeu n'a pas été exécuté sur iPhone ou simulateur. La première compilation
peut révéler des corrections à faire. Aucun IPA n'est fourni dans cette archive.

Le projet cible iOS 16 minimum, iPhone ARM64 avec Metal. La compatibilité avec ta
version exacte d'iOS, notamme
nt une bêta, reste à tester avec un Xcode adapté.
SceneKit est utilisé pour ce prototype compact ; son choix devra être réévalué
pour une production à long terme. La cible de rendu est 30 images/s, sans garantie
de performance avant mesure sur ton appareil. Les géométries simples, le nombre
limité d'ennemis et les effets temporaires réduisent la charge graphique.

## Produire le fichier IPA à signer

Ta signature sur iPhone intervient APRÈS la compilation. Elle ne transforme pas
les sources Swift en application. Sur un Mac local ou distant équipé de Xcode :

```bash
cd Brumes
bash scripts/build-ipa.sh
```

Le résultat attendu est `dist/Brumes-unsigned.ipa`. Télécharge-le sur l'iPhone,
puis utilise ton outil de signature avec tes propres certificat et profil.
L'identifiant initial est `com.mumu.brumes` ; adapte-le aux exigences de ton profil
ou de ton outil. Ne transmets pas tes clés privées dans une conversation.
L'IPA non signé seul ne s'installe pas. Le script n'achète aucun service et ne
publie rien sur l'App Store.

Pour lancer depuis Xcode : ouvre `Brumes.xcodeproj`, choisis le schéma Brumes,
sélectionne ton équipe dans Signing & Capabilities et ton appareil, puis Run.
Pour le simulateur, sélectionne un simulateur iPhone ; sa compilation ne produit
pas un IPA utilisable sur un vrai appareil.

## Vérification après compilation

1. Lancer en paysage : joystick, six boutons et HUD doivent être visibles.
2. Marcher et tourner la caméra simultanément, approcher un arbre pour vérifier le blocage.
3. Tester les trois sorts sur des ennemis ; vérifier mana, délais et dégâts.
4. Activer les trois cristaux puis fermer et relancer : progression conservée.
5. Passer l'application en arrière-plan : reprise sans déplacement involontaire.
6. Jouer dix minutes sur l'appareil cible et mesurer fluidité, chauffe et batterie.

Les sources graphiques sont entièrement procédurales ; aucun fichier tiers ni
aucune clé de service n'est nécessaire. Fichiers principaux : `Brumes/Game.sw
ift`,
`Brumes/Info.plist`, `Brumes/PrivacyInfo.xcprivacy` et `Brumes.xcodeproj`.

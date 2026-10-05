# Outil en ligne de commande (`worldedit-cli`)

[← Retour à l'accueil du fork](README.md)

`worldedit-cli` exécute des commandes WorldEdit sur un fichier de schematic depuis un terminal, sans
serveur Minecraft. Référence complète : [`worldedit-cli/README.md`](../../worldedit-cli/README.md).

## Compiler

```sh
./gradlew :worldedit-cli:shadowJar
```

Le jar exécutable est `worldedit-cli/build/libs/worldedit-cli-<version>-dist.jar`.

## Options

```
java -jar worldedit-cli.jar [options]
```

| Option | Description |
|--------|-------------|
| `-f`, `--file <chemin>` | Fichier à ouvrir. Sans cette option, une boîte de dialogue de choix de fichier s'ouvre |
| `-s`, `--script <chemin>` | Fichier de commandes, une par ligne ; lignes vides et lignes commençant par `#` ignorées |
| `-n`, `--non-interactive` | Quitter à la fin du script au lieu de lire d'autres commandes sur l'entrée standard (exige `--script`) |
| `-h`, `--help` | Afficher l'aide et quitter |

## Fichiers acceptés

- Schematics Sponge (`.schem`), MCEdit (`.schematic`) et structures vanilla (`.nbt`) ; le format
  est détecté d'après le **contenu** du fichier.
- Le fichier est réécrit dans **son propre format**. Les formats en lecture seule (MCEdit
  `.schematic`, Sponge v1) ne peuvent pas être réenregistrés : la CLI l'annonce **dès le
  chargement**, puis le premier échec d'enregistrement affiche une erreur (« Your changes are NOT
  saved ») **une seule fois** ; les échecs suivants sont silencieux, avec un court rappel à la
  sortie. Le fichier d'origine reste intact et la CLI se termine avec le code **1**. Les commandes
  qui ne modifient rien (inspection, `//count`...) restent utilisables. Pour modifier un tel fichier,
  convertissez-le d'abord en `.schem` (v2 ou v3) ou `.nbt` en jeu.
- Les fichiers `level.dat` (mondes) ne sont **pas encore** pris en charge.
- Les modifications sont **enregistrées dans le fichier** après chaque commande réussie et à la
  sortie. L'écriture passe par un fichier temporaire : un échec ne laisse jamais de schematic tronquée.
  Une erreur d'enregistrement n'est affichée en entier qu'une fois (jusqu'au prochain enregistrement
  réussi), puis rappelée brièvement à la sortie si elle persiste.

## Utilisation

Les commandes se tapent comme en jeu (`//set minecraft:stone`). Le `/` initial d'une commande à un
seul slash peut être omis : `cli selectworld` = `/cli selectworld`. Tapez `stop` (ou fermez l'entrée
standard) pour quitter.

Il n'y a pas de joueur : les sélections se font par commande. Commandes propres à la CLI :

| Commande | Effet |
|----------|-------|
| `/cli selectworld` | Sélectionne toute la schematic |
| `/cli await` | Attend la fin des tâches en arrière-plan |

### Mode interactif

```sh
java -jar worldedit-cli.jar --file maison.schem
> /cli selectworld
> //replace minecraft:oak_planks minecraft:spruce_planks
> stop
```

### Mode script

`remplacer-planches.txt` :

```
# Remplace les planches par du sapin
cli selectworld
//replace minecraft:oak_planks minecraft:spruce_planks
```

```sh
java -jar worldedit-cli.jar --file maison.schem --script remplacer-planches.txt --non-interactive
```

Sans `--non-interactive`, la CLI continue de lire l'entrée standard après le script (sauf si le
script se termine par `stop`).

## Codes de sortie

| Code | Signification |
|------|---------------|
| 0 | Succès |
| 1 | Erreur : fichier absent ou non pris en charge, échec du démarrage, modifications impossibles à enregistrer (format en lecture seule, erreur disque), ou (en mode non interactif) commande de script non reconnue |
| 2 | Arguments de ligne de commande invalides ; l'usage est affiché |

## Données

La configuration et les données de WorldEdit sont stockées dans un dossier `worldedit` du
répertoire courant. À la première utilisation de chaque version de données Minecraft, les données de
blocs et d'objets de cette version sont téléchargées depuis EngineHub dans `worldedit/cli-data` :
le premier lancement nécessite donc un accès à Internet.

## Intégration continue / automatisation

Le mode script + `--non-interactive` + codes de sortie permet d'utiliser la CLI dans un pipeline,
par exemple pour normaliser des schematics avant de les publier :

```sh
for f in schematics/*.schem; do
  java -jar worldedit-cli.jar --file "$f" --script normaliser.txt --non-interactive || exit 1
done
```

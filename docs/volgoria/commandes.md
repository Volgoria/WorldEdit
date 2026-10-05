# Nouvelles commandes

[← Retour à l'accueil du fork](README.md)

Cette page liste uniquement les commandes **ajoutées** (ou modifiées) par le fork Volgoria. Les
commandes d'origine sont documentées sur <https://worldedit.enginehub.org/>.

## Conventions

- `<arg>` : argument obligatoire ; `[arg]` : argument facultatif (valeur par défaut indiquée).
- `-x` : option booléenne (*switch*) ; `-x <valeur>` : option avec valeur (*flag*). Placez les
  options avant les arguments positionnels, comme dans les exemples.
- **Pattern** / **masque** : toute syntaxe WorldEdit, y compris les nouvelles
  ([patterns-et-masques.md](patterns-et-masques.md)).
- **Rayons multiples** (`@Radii`) : une seule valeur pour tous les axes, ou une valeur par axe
  séparées par des virgules, sans espace (`5,3`).
- **Direction** : `me` (là où vous regardez), `up`, `down`, `north`, `south`, `east`, `west`,
  `left`, `right`, `back`, `forward`, etc. (même syntaxe que `//stack`).
- La « position de placement » est votre position, ou `pos1` après `//placement pos1`
  (voir `//placement`).
- Toutes les commandes qui modifient le monde sont annulables avec `//undo` et respectent
  `//limit`, `//gmask` et les rayons maximaux de la configuration (`max-radius`,
  `max-brush-radius`).

## Sommaire

| Commande | Alias | Permission |
|----------|-------|------------|
| [`//torus`](#torus) | `//donut` | `worldedit.generation.torus` |
| [`//disk`](#disk) | `//disc` | `worldedit.generation.disk` |
| [`//dome`](#dome) | — | `worldedit.generation.dome` |
| [`//helix`](#helix) | `//spiral` | `worldedit.generation.helix` |
| [`//arch`](#arch) | — | `worldedit.generation.arch` |
| [`//wireframe`](#wireframe) | `//edges` | `worldedit.region.wireframe` |
| [`//surface`](#surface) | — | `worldedit.region.surface` |
| [`//history`](#history) | `//listhistory` | `worldedit.history.list` (+ `.other`) |
| [`//paste -r`](#paste--r) | — | `worldedit.clipboard.paste` (inchangée) |
| [`//text`](#text) | — | `worldedit.build.text` |
| [`//symmetry`](#symmetry) | `//mirrorhalf` | `worldedit.build.symmetry` |
| [`//flatten`](#flatten) | — | `worldedit.build.flatten` |
| [`//terrain`](#terrain) | — | `worldedit.build.terrain` |
| [`//path`](#path) | `//road` | `worldedit.build.path` |
| [`//cave`](#cave) | `//caves` | `worldedit.build.cave` |
| [`//staircase`](#staircase) | `//stairs` | `worldedit.build.staircase` |
| [`//spiralstairs`](#spiralstairs) | `//spiralstaircase` | `worldedit.build.spiralstairs` |
| [`//randomize`](#randomize) | `//texture` | `worldedit.build.randomize` |
| [`//image`](#image) | `//pixelart` | `worldedit.image.paste` |
| [`//topview`](#topview) | `//imageexport`, `//mapexport` | `worldedit.image.export` |
| [`//heightmap import`](#heightmap-import) | `//hmap import`, `... load` | `worldedit.image.heightmap.import` |
| [`//heightmap export`](#heightmap-export) | `//hmap export`, `... save` | `worldedit.image.heightmap.export` |
| [`/schem info`](#schem-info) | `/schem i` | `worldedit.schematic.info` |
| [`/schem rename`](#schem-rename) | `/schem move`, `/schem mv` | `worldedit.schematic.rename` |
| [`/schem copy`](#schem-copy) | `/schem cp` | `worldedit.schematic.copy` |
| [`/schem list`](#schem-list-filtres) (filtres) | `/schem all`, `/schem ls` | `worldedit.schematic.list` (inchangée) |
| [`/schem save` / `load` / `formats`](#nouveaux-formats-dans-schem-save--load--formats) | — | inchangées |

Les brosses et outils sont sur [brosses-et-outils.md](brosses-et-outils.md), le menu `/wegui` sur
[gui.md](gui.md).

---

## Génération

Toutes ces formes sont centrées sur (ou partent de) votre position de placement.

### `//torus`

```
//torus [-h] <pattern> <rayonPrincipal> <rayonTube> [direction=up]
```

Alias : `//donut` — Permission : `worldedit.generation.torus`

| Argument | Description |
|----------|-------------|
| `pattern` | Blocs à poser |
| `rayonPrincipal` | Distance du centre au milieu du tube (≥ 0, décimal accepté) |
| `rayonTube` | Rayon du tube (≥ 0) |
| `direction` | Direction de l'axe qui traverse le trou. Défaut `up` (tore couché) |
| `-h` | Tore creux |

`rayonPrincipal + rayonTube` doit respecter le rayon maximal. Vous êtes déplacé hors des blocs
générés si besoin.

```
//torus stone 10 3
//torus -h glass 8 2 north
```

### `//disk`

```
//disk [-h] <pattern> <rayons> [direction=me] [épaisseur=1]
```

Alias : `//disc` — Permission : `worldedit.generation.disk`

| Argument | Description |
|----------|-------------|
| `rayons` | Un rayon, ou deux séparés par une virgule : 1er = horizontal, 2e = vertical (N/S pour un disque à plat) |
| `direction` | Direction vers laquelle le disque fait face. Défaut : là où vous regardez |
| `épaisseur` | Épaisseur en blocs (≥ 1) |
| `-h` | Seulement le bord (un anneau) |

```
//disk white_concrete 6 up          (disque horizontal de rayon 6)
//disk -h stone 8,4 north 2         (anneau elliptique vertical de 2 blocs d'épaisseur)
```

### `//dome`

```
//dome [-h] [-i] <pattern> <rayons>
```

Permission : `worldedit.generation.dome`

| Argument | Description |
|----------|-------------|
| `rayons` | Un rayon, ou trois séparés par des virgules dans l'ordre N/S, haut/bas, E/O |
| `-h` | Dôme creux, ouvert en bas (seule la surface courbe est posée) |
| `-i` | Dôme inversé (un bol) |

Demi-ellipsoïde coupée horizontalement au niveau de la position de placement.

```
//dome -h glass 12
//dome -i stone 10,5,10
```

### `//helix`

```
//helix [-d] <pattern> <rayon> <hauteur> [tours=1] [épaisseur=1]
```

Alias : `//spiral` — Permission : `worldedit.generation.helix`

| Argument | Description |
|----------|-------------|
| `rayon` | Rayon de l'hélice (≥ 0) |
| `hauteur` | Hauteur en blocs (≥ 1) ; l'hélice monte depuis la position de placement |
| `tours` | Nombre de tours (≥ 0, décimal accepté) |
| `épaisseur` | Épaisseur du brin (≥ 1) |
| `-d` | Double hélice (deux brins opposés) |

```
//helix quartz_block 6 30 3
//helix -d red_wool 5 40 4 2
```

### `//arch`

```
//arch <pattern> <largeur> <hauteur> [épaisseur=1] [profondeur=1] [direction=me]
```

Permission : `worldedit.generation.arch`

| Argument | Description |
|----------|-------------|
| `largeur` | Largeur extérieure (arrondie à l'impair supérieur pour être symétrique) |
| `hauteur` | Hauteur extérieure |
| `épaisseur` | Épaisseur de la bande de l'arche |
| `profondeur` | Profondeur le long du sens de passage |
| `direction` | Sens dans lequel on traverse l'arche (doit être horizontal ; seule la composante horizontale compte) |

Arche semi-elliptique posée au sol : la position de placement est le **bas-centre** de l'arche.

```
//arch stone_bricks 9 6
//arch stone_bricks 15 10 2 3 east
```

---

## Région, historique et presse-papiers

### `//wireframe`

```
//wireframe <pattern>
```

Alias : `//edges` — Permission : `worldedit.region.wireframe`

Pose les **12 arêtes** de la boîte englobante de la sélection (quel que soit le type de sélection).
Pratique pour tracer le cadre d'une construction.

```
//wireframe glowstone
```

### `//surface`

```
//surface [-m <masqueSol>] <pattern> [profondeur=1]
```

Permission : `worldedit.region.surface`

Remplace les `profondeur` couches supérieures du sol exposé de la sélection. Contrairement à
`//overlay`, qui pose des blocs **sur** le sol, `//surface` remplace le sol lui-même.

| Argument | Description |
|----------|-------------|
| `profondeur` | Nombre de couches remplacées (≥ 1) |
| `-m <masque>` | Seuls les blocs correspondant à ce masque comptent comme sol. Défaut : tout bloc non-air |

```
//surface sand 3
//surface -m grass_block,dirt podzol 2
```

### `//history`

```
//history [joueur]
```

Alias : `//listhistory` — Permission : `worldedit.history.list` ; consulter l'historique d'un autre
joueur demande en plus `worldedit.history.list.other`.

Liste les modifications de l'historique, de la plus récente à la plus ancienne :
`Undo #n: <blocs> blocks changed in <monde>`. Les modifications déjà annulées apparaissent comme
`Redo #n (undone)`. Le numéro indique combien de `//undo` (ou `//redo`) il faut pour l'atteindre.

```
//history
//history Alex
```

### `//paste -r`

```
//paste [-a] [-v] [-o] [-s] [-n] [-e] [-b] [-r] [-m <masque>]
```

Nouvelle option `-r` : fait pivoter le collage d'un multiple aléatoire de 90° autour de l'axe Y
(0, 90, 180 ou 270 ; l'angle choisi est affiché). La transformation de votre presse-papiers n'est
**pas** modifiée : chaque `//paste -r` tire un nouvel angle. Permission inchangée
(`worldedit.clipboard.paste`).

```
//paste -a -r
```

---

## Construction

### `//text`

```
//text [-f] [-s <taille>] [-t <épaisseur>] [-d <direction>] <pattern> <texte...>
```

Permission : `worldedit.build.text`

Écrit du texte en blocs avec une police 5×7 intégrée, depuis la position de placement, dans le sens
de lecture donné. `\n` commence une nouvelle ligne.

| Option | Défaut | Description |
|--------|--------|-------------|
| `-s <taille>` | 1 | Taille d'un pixel de police, en blocs |
| `-t <épaisseur>` | 1 | Épaisseur des lettres |
| `-d <direction>` | `right` | Sens de lecture ; doit être horizontal (`right`, `left`, `north`, `east`...) |
| `-f` | — | Texte couché au sol au lieu d'être debout |

```
//text gold_block Bienvenue
//text -s 2 -f -d east stone VOLGORIA\nSPAWN
```

### `//symmetry`

```
//symmetry [-a] [direction=me]
```

Alias : `//mirrorhalf` — Permission : `worldedit.build.symmetry`

Le plan miroir passe par le centre de la sélection ; la moitié située dans `direction` est écrasée
par l'image miroir de la moitié opposée. `-a` : ne copie pas l'air.

```
//symmetry east
//symmetry -a
```

### `//flatten`

```
//flatten [-m <masque>] [-p <pattern>] [hauteur]
```

Permission : `worldedit.build.flatten`

Aplatit le terrain de la sélection : ce qui dépasse la hauteur est retiré, les creux en dessous sont
comblés. Sans hauteur, la hauteur moyenne de la surface de la sélection est utilisée. La hauteur doit
être comprise dans la sélection.

| Option | Description |
|--------|-------------|
| `-m <masque>` | Blocs qui constituent le terrain (défaut : tout bloc non-air) |
| `-p <pattern>` | Pattern de remplissage des creux (défaut : les blocs de la colonne) |

```
//flatten
//flatten -p dirt 64
```

### `//terrain`

```
//terrain [-k] [-t <patternDessus>] [-o <octaves>] [-s <graine>] <pattern> [échelle=32] [amplitude]
```

Permission : `worldedit.build.terrain`

Génère du relief par bruit de Perlin : chaque colonne est remplie depuis le bas de la sélection
jusqu'à une hauteur donnée par le bruit.

| Argument / option | Défaut | Description |
|-------------------|--------|-------------|
| `échelle` | 32 | Échelle horizontale des collines, en blocs (> 0) |
| `amplitude` | hauteur de la sélection | Hauteur maximale du terrain (≥ 0) |
| `-t <pattern>` | — | Pattern de la couche supérieure |
| `-o <octaves>` | 4 | Nombre d'octaves (rugosité), entre 1 et 30 |
| `-s <graine>` | aléatoire | Graine du bruit (entier) |
| `-k` | — | Conserve les blocs au-dessus du terrain au lieu de les vider |

```
//terrain stone
//terrain -t grass_block -o 6 -s 42 stone 48 20
```

### `//path`

```
//path [-l] [-f] [-h <dégagement>] <pattern> [largeur=3]
```

Alias : `//road` — Permission : `worldedit.build.path`

Trace un chemin passant par les points de la sélection, **dans l'ordre où ils ont été sélectionnés**.
Nécessite une sélection convexe (`//sel convex`) ou polygonale (`//sel poly`) d'au moins 2 points.
Par défaut, le chemin suit la surface du terrain (recherche sur ±16 blocs).

| Option | Défaut | Description |
|--------|--------|-------------|
| `largeur` | 3 | Largeur du chemin (≥ 1) |
| `-h <n>` | 0 | Nombre de blocs dégagés (air) au-dessus du chemin |
| `-l` | — | Boucle : relie le dernier point au premier |
| `-f` | — | Ne suit pas le terrain : relie les points en ligne droite (pont) |

```
//sel poly
//path dirt_path 3
//path -f -h 3 stone_bricks 5
```

### `//cave`

```
//cave [-n <nombre>] [-s <graine>] [rayon=3] [longueur=64]
```

Alias : `//caves` — Permission : `worldedit.build.cave`

Creuse des tunnels sinueux (« vers de Perlin ») dans la sélection. Le premier part du centre de la
sélection, les autres d'endroits aléatoires.

| Argument / option | Défaut | Description |
|-------------------|--------|-------------|
| `rayon` | 3 | Rayon des tunnels (≥ 0,5) |
| `longueur` | 64 | Longueur de chaque tunnel en blocs (≥ 1) |
| `-n <nombre>` | 1 | Nombre de tunnels (1 à 64) |
| `-s <graine>` | aléatoire | Graine |

```
//cave
//cave -n 5 -s 1234 2.5 100
```

### `//staircase`

```
//staircase [-s <patternSupport>] <pattern> <marches> [largeur=1] [direction=me]
```

Alias : `//stairs` — Permission : `worldedit.build.staircase`

Escalier droit qui monte depuis la position de placement. La direction doit être horizontale
(nord, sud, est, ouest). Les blocs ayant une propriété `facing` (escaliers) sont tournés vers la
montée. `-s` : pattern de remplissage sous les marches.

```
//staircase oak_stairs 10
//staircase -s cobblestone stone_brick_stairs 12 3 north
```

### `//spiralstairs`

```
//spiralstairs [-c] [-p <patternPilier>] [-t <marchesParTour>] <pattern> <rayon> <marches>
```

Alias : `//spiralstaircase` — Permission : `worldedit.build.spiralstairs`

Escalier en colimaçon autour de la position de placement.

| Argument / option | Défaut | Description |
|-------------------|--------|-------------|
| `rayon` | — | Rayon extérieur (≥ 1) |
| `marches` | — | Nombre de marches (≥ 1) |
| `-p <pattern>` | — | Pilier central |
| `-t <n>` | 16 | Marches par tour complet (≥ 2) |
| `-c` | — | Monte dans le sens antihoraire |

```
//spiralstairs -p stone_bricks oak_slab 4 32
```

### `//randomize`

```
//randomize [-m <masque>] [-s <graine>] <pattern> [pourcentage=20]
```

Alias : `//texture` — Permission : `worldedit.build.randomize`

Remplace au hasard un pourcentage (0 à 100) des blocs **exposés** (touchant de l'air) de la
sélection. `-m` : ne remplace que les blocs correspondant au masque.

```
//randomize mossy_cobblestone 25
//randomize -m stone_bricks cracked_stone_bricks,mossy_stone_bricks 15
```

---

## Images

Les images sont lues et écrites dans le dossier `images` de WorldEdit (`plugins/WorldEdit/images/`
sur Bukkit). Voir [formats.md](formats.md#dossier-images) pour les conventions.

### `//image`

```
//image [-v] [-d] [-s] <fichier> [largeur]
```

Alias : `//pixelart` — Permission : `worldedit.image.paste`

Construit un pixel art : chaque pixel devient le bloc de la palette intégrée dont la couleur est la
plus proche. Les pixels transparents sont ignorés.

| Argument / option | Description |
|-------------------|-------------|
| `fichier` | Nom de l'image dans le dossier `images` (`.png` ajouté si aucune extension ; png, jpg, jpeg, gif, bmp acceptés) |
| `largeur` | Largeur en blocs ; la hauteur suit les proportions. Défaut : largeur de l'image. Ignoré avec `-s` |
| `-v` | Image debout (verticale) au lieu d'être à plat |
| `-d` | Tramage Floyd-Steinberg |
| `-s` | Étire l'image sur la sélection au lieu de la placer devant vous |

Sans `-s`, l'image est posée à plat au niveau du sol devant vous (ou debout, 2 blocs devant vous
avec `-v`), centrée sur votre position et orientée selon votre regard. Avec `-s`, elle couvre
l'emprise horizontale de la sélection (à plat, au niveau Y minimal) ou, avec `-v`, sa plus grande face
verticale. Taille maximale : **1024 × 1024 blocs**. Les images sources sont limitées à 4096 × 4096
pixels. Hors joueur (console), `-s` est obligatoire.

```
//image logo.png 64
//image -v -d portrait.jpg 48
//image -s carte
```

### `//topview`

```
//topview [-s] [-f] <fichier>
```

Alias : `//imageexport`, `//mapexport` — Permission : `worldedit.image.export`

Enregistre une carte vue de dessus de la sélection en PNG : un pixel par colonne, coloré d'après le
bloc le plus haut ; le nord est en haut. `-s` : ombrage des pentes façon carte en jeu ;
`-f` : écrase un fichier existant. Taille maximale : 4096 × 4096 colonnes.

```
//topview -s spawn
```

### `//heightmap import`

```
//heightmap import [-c] <fichier> [hauteurMax] [pattern]
```

Alias : `//hmap import`, `//heightmap load` — Permission : `worldedit.image.heightmap.import`

Construit du terrain sur la sélection à partir d'une image en niveaux de gris, étirée sur l'emprise
horizontale de la sélection. Noir = bas de la sélection ; blanc = `hauteurMax` blocs au-dessus.
Les colonnes sont remplies depuis le bas de la sélection et ne dépassent jamais son sommet.

| Argument / option | Défaut | Description |
|-------------------|--------|-------------|
| `hauteurMax` | hauteur de la sélection | Hauteur des pixels blancs, en blocs (≥ 1) |
| `pattern` | herbe / 3 de terre / pierre | Pattern de remplissage des colonnes |
| `-c` | — | Vide les blocs au-dessus du terrain, dans la sélection |

Les images en niveaux de gris 8 ou 16 bits sont lues en pleine précision ; les images couleur sont
converties en luminance. Sélection limitée à 4096 × 4096 colonnes.

```
//heightmap import -c ile.png
//heightmap import montagne.png 80 stone
```

### `//heightmap export`

```
//heightmap export [-f] <fichier>
```

Alias : `//hmap export`, `//heightmap save` — Permission : `worldedit.image.heightmap.export`

Enregistre la hauteur de surface de chaque colonne de la sélection en PNG **16 bits** niveaux de
gris : noir = colonne vide, blanc = sommet de la sélection. Réimporter l'image sur la même sélection
reconstruit les mêmes hauteurs. `-f` : écrase un fichier existant.

```
//heightmap export -f relief
```

---

## Schematics

Les noms de fichiers sont relatifs au dossier des schematics (`plugins/WorldEdit/schematics/`) ;
les sous-dossiers sont autorisés (`maisons/ferme`).

### `/schem info`

```
/schem info <fichier>
```

Alias : `/schem i` — Permission : `worldedit.schematic.info`

Affiche, **sans charger** la schematic dans le presse-papiers : format détecté, dimensions et volume,
nombre de blocs non-air, d'entités de bloc et d'entités, décalage par rapport à l'origine, *data
version* Minecraft et taille du fichier. La lecture se fait en arrière-plan.

```
/schem info maisons/ferme
```

### `/schem rename`

```
/schem rename [-f] <fichier> <nouveauNom>
```

Alias : `/schem move`, `/schem mv` — Permission : `worldedit.schematic.rename`

Renomme ou déplace une schematic. Le nouveau nom est relatif au dossier des schematics et **garde
l'extension** du fichier d'origine (elle est ajoutée si absente ou différente). Écraser un fichier
existant demande `-f` **et** la permission `worldedit.schematic.delete`.

```
/schem rename ferme maisons/ferme_v2
```

### `/schem copy`

```
/schem copy [-f] <fichier> <nomCopie>
```

Alias : `/schem cp` — Permission : `worldedit.schematic.copy`

Comme `rename`, mais conserve l'original. Mêmes règles pour l'extension et l'écrasement.

```
/schem copy maisons/ferme archives/ferme_2026
```

### `/schem list` (filtres)

```
/schem list [-d | -n] [-p <page>] [-f <format>] [filtre]
```

Alias : `/schem all`, `/schem ls` — Permission : `worldedit.schematic.list` (inchangée)

Nouveautés :

- `-f <format>` : ne liste que les fichiers dont l'extension correspond à ce format (nom ou alias,
  voir `/schem formats`) ; un format inconnu affiche une erreur.
- `filtre` : ne liste que les fichiers dont le chemin contient ce texte (insensible à la casse).
- Le format affiché pour chaque fichier fonctionne aussi pour les extensions en majuscules.
- La pagination cliquable conserve les filtres.

```
/schem list -f structure
/schem list -n maison
```

### Nouveaux formats dans `/schem save` / `load` / `formats`

```
/schem save [-f] <fichier> [format=sponge]
/schem load <fichier> [format=sponge]
/schem formats
```

Nouveaux formats (détails dans [formats.md](formats.md)) :

| Format | Alias | Extension | Lecture | Écriture |
|--------|-------|-----------|---------|----------|
| `MINECRAFT_STRUCTURE` | `structure`, `nbt`, `vanilla` | `.nbt` | oui | oui |
| `WAVEFRONT_OBJ` | `obj`, `wavefront` | `.obj` (+ `.mtl`) | non | oui |
| `JSON` | `json` | `.json` | non | oui |

- `/schem formats` indique `(export only)` ou `(load only)` à côté des formats concernés.
- `/schem save` refuse un format en lecture seule (par ex. `mcedit`) ; `/schem load` refuse un
  format en export seul avec un message clair.
- Au chargement, le format est détecté d'après le contenu du fichier quand c'est possible.

```
/schem save -f ferme structure
/schem save ferme obj
/schem load ferme.nbt
```

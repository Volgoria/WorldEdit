# Formats de fichiers

[← Retour à l'accueil du fork](README.md)

## Récapitulatif des formats de schematics

`/schem formats` liste les formats disponibles et leurs alias. Le nom de format se donne en dernier
argument de `/schem save` et `/schem load` (défaut `sponge`) ; au chargement, le format est de
préférence détecté d'après le contenu du fichier.

| Format | Alias | Extension | Charger | Enregistrer | Origine |
|--------|-------|-----------|---------|-------------|---------|
| `SPONGE_V3_SCHEMATIC` | `sponge.3`, `sponge`, `schem` | `.schem` | oui | oui (défaut) | upstream |
| `SPONGE_V2_SCHEMATIC` | `sponge.2` | `.schem` | oui | oui | upstream |
| `SPONGE_V1_SCHEMATIC` | `sponge.1` | `.schem` | oui | non | upstream |
| `MCEDIT_SCHEMATIC` | `mcedit`, `mce`, `schematic` | `.schematic` | oui | non | upstream |
| `MINECRAFT_STRUCTURE` | `structure`, `nbt`, `vanilla` | `.nbt` | oui | oui | **fork** |
| `WAVEFRONT_OBJ` | `obj`, `wavefront` | `.obj` + `.mtl` | non | oui | **fork** |
| `JSON` | `json` | `.json` | non | oui | **fork** |

`/schem formats` affiche `(export only)` pour OBJ et JSON, `(load only)` pour les formats qui ne
peuvent pas être écrits. `/schem save` refuse un format non inscriptible, `/schem load` refuse un
format en export seul.

## Sponge (`.schem`)

Format par défaut de WorldEdit, inchangé par le fork : `/schem save <nom>` écrit en Sponge v3.
Il conserve blocs, entités de bloc (NBT), entités et biomes, ainsi que l'origine du presse-papiers.

```
/schem save maison
/schem save maison sponge.2      (pour les outils qui ne lisent que la v2)
```

## Structure vanilla (`.nbt`)

Le format des **blocs de structure**, de `/place template` et des *data packs*.

```
/schem save -f maison structure
/schem load maison.nbt
```

**Écriture**

- Fichier NBT compressé en GZip, avec `DataVersion` (version de données du serveur), `size`,
  `palette`, `blocks` (avec le NBT des entités de bloc, sans `x`/`y`/`z`) et `entities`.
- Les blocs `structure_void` sont omis, comme en vanilla ; l'air est écrit (il remplacera donc les
  blocs existants au placement, sauf s'il est ignoré par l'outil de placement).
- Les **biomes ne font pas partie du format** et sont perdus.
- L'origine WorldEdit est conservée dans un compound supplémentaire `WorldEdit`
  (`Version`, `Origin`, `Offset`), ignoré par le jeu. Un fichier écrit puis relu par WorldEdit
  retrouve donc la même origine.

**Lecture**

- Les positions absentes du fichier (vides de structure) deviennent de l'air.
- S'il y a plusieurs palettes (ex. épaves), la première est utilisée.
- Les blocs, propriétés ou entités inconnus sont ignorés avec un avertissement dans la console.
- Les données plus anciennes sont converties par le *data fixer* ; un fichier d'une version plus
  récente que le serveur provoque un avertissement.
- Sans compound `WorldEdit` (fichier vanilla), l'origine est le coin minimal de la structure.

Pour utiliser un fichier en jeu avec `/place template minecraft:<nom>`, copiez-le dans
`<monde>/generated/minecraft/structures/<nom>.nbt`.

## Wavefront OBJ (`.obj` + `.mtl`) — export seul

Pour les logiciels 3D (Blender, etc.).

```
/schem save maison obj
```

- Écrit `maison.obj` **et** `maison.mtl` à côté (la bibliothèque de matériaux est référencée par
  `mtllib`). En cas d'échec, les deux fichiers sont supprimés.
- `/schem rename` et `/schem copy` emportent le `.mtl` avec le `.obj` (et mettent à jour `mtllib`) ;
  `/schem delete maison.obj` supprime aussi `maison.mtl`.
- Seules les **faces visibles** sont exportées : une face est cachée si le bloc voisin la masque
  (cube plein opaque) ou est du même type (verre contre verre).
- Un groupe d'objets et un matériau (`usemtl`) **par type de bloc** ; les faces coplanaires d'un même
  type sont fusionnées en rectangles (*greedy meshing*).
- Coordonnées en blocs, relatives au coin minimal du presse-papiers, Y vers le haut.
- Pas de textures : chaque matériau a une couleur diffuse (`Kd`) **approximative** (couleur de
  teinture pour laine, béton, verre..., matériaux courants, sinon une couleur stable dérivée de
  l'identifiant) et une opacité (`d`).
- Ne peut pas être rechargé dans WorldEdit.

## JSON (`.json`) — export seul

Pour les visionneuses web et outils maison.

```
/schem save maison json
```

Structure du document (version 1) :

```json
{
  "format": "worldedit:json",
  "version": 1,
  "dataVersion": 3953,
  "size": [w, h, l],
  "origin": [x, y, z],
  "offset": [x, y, z],
  "palette": ["minecraft:air", "minecraft:oak_stairs[facing=north,half=bottom,...]"],
  "blocks": [0, 1, ...],
  "blockEntities": [{"pos": [x, y, z], "id": "minecraft:chest", "snbt": "{...}"}],
  "entities": [{"id": "minecraft:pig", "pos": [x, y, z], "rotation": [yaw, pitch], "snbt": "{...}"}]
}
```

| Champ | Signification |
|-------|---------------|
| `format`, `version` | Toujours `"worldedit:json"` et `1` |
| `dataVersion` | Version de données Minecraft du serveur |
| `size` | Largeur (X), hauteur (Y), longueur (Z) |
| `origin` | Origine du presse-papiers (coordonnées monde) |
| `offset` | Coin minimal relatif à l'origine |
| `palette` | États de bloc complets, avec propriétés |
| `blocks` | Un index de palette par bloc, ordonné par Y, puis Z, puis X : l'index de la position relative `(x, y, z)` est `(y * l + z) * w + x` |
| `blockEntities` | Position relative au coin minimal, identifiant, NBT au format SNBT (sans `x`/`y`/`z`) |
| `entities` | Identifiant, position relative au coin minimal (décimale), rotation `[yaw, pitch]`, NBT en SNBT |

Ne peut pas être rechargé dans WorldEdit.

## Dossier `images`

Utilisé par `//image`, `//topview`, `//heightmap` et le menu Images.

- Emplacement : dossier `images` du répertoire de travail de WorldEdit, soit
  `plugins/WorldEdit/images/` sur Bukkit/Paper. Il est créé automatiquement à la première image
  enregistrée ; créez-le à la main pour y déposer vos images.
- Les noms sont relatifs à ce dossier ; les sous-dossiers sont permis, mais pas les chemins qui en
  sortent. Les règles de noms de fichiers de WorldEdit s'appliquent (comme pour les schematics).
- **Lecture** : `png`, `jpg`, `jpeg`, `gif`, `bmp`. Sans extension, `.png` est supposé. Images
  limitées à 2048 × 2048 pixels (4 mégapixels ; plus grandes, elles sont refusées avant décodage).
- **Écriture** : toujours en PNG (`.png` ajouté si absent). Un fichier existant n'est remplacé
  qu'avec `-f`.
- Le menu `/wegui images` liste les fichiers de ce dossier (sous-dossiers inclus).

### Pixel art (`//image`)

- Palette intégrée de blocs pleins, opaques, sans gravité ni entité de bloc (laines, bétons,
  terres cuites, etc.), réduite aux blocs existant sur le serveur. Chaque pixel prend le bloc le plus
  proche en couleur (`-d` active le tramage Floyd-Steinberg).
- Les pixels transparents ne posent rien.
- Taille construite maximale : 1024 × 1024 blocs.

### Carte vue de dessus (`//topview`)

- Un pixel par colonne (X → droite, Z → bas, nord en haut), couleur du bloc le plus haut ;
  `-s` ombre les pentes comme une carte en jeu.
- Sélection limitée à 4096 × 4096 colonnes.

### Cartes de hauteur (`//heightmap`)

- **Export** : PNG **16 bits** en niveaux de gris. Noir = colonne vide, blanc = sommet de la
  sélection ; l'échelle est la hauteur de la sélection (rappelée dans le message de confirmation).
- **Import** : images en niveaux de gris 8 ou 16 bits lues en pleine précision ; images couleur
  converties en luminance. L'image est étirée sur l'emprise horizontale de la sélection ; noir = bas
  de la sélection, blanc = `hauteurMax` blocs au-dessus (défaut : hauteur de la sélection).
- Exporter puis réimporter sur la même sélection reconstruit les mêmes hauteurs.

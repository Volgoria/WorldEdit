# Nouvelles brosses et nouveaux outils

[← Retour à l'accueil du fork](README.md)

## Rappels

- Une brosse se lie à l'**objet tenu en main principale** (pas un bloc) : `/brush <type> ...`
  (alias `/br`, `//brush`, `//br`). On l'utilise par **clic droit** en visant un bloc.
- `/brush none` (ou `/brush unbind`) retire la brosse de l'objet.
- Les réglages habituels restent valables sur toutes les nouvelles brosses : `/size`, `/mask`,
  `/material`, `/range`, `/tracemask`.
- Les rayons sont limités par `max-brush-radius` dans la configuration. Toutes les modifications
  sont annulables avec `//undo`.
- `<arg>` obligatoire, `[arg=défaut]` facultatif, `-x` option. Placez les options avant les
  arguments positionnels.

## Sommaire des brosses

| Brosse | Alias | Permission |
|--------|-------|------------|
| [`blob`](#blob) | — | `worldedit.brush.blob` |
| [`line`](#line) | — | `worldedit.brush.line` |
| [`overlay`](#overlay) | `cover` | `worldedit.brush.overlay` |
| [`fill`](#fill) | `filldown` | `worldedit.brush.fill` |
| [`drain`](#drain) | — | `worldedit.brush.drain` |
| [`spline`](#spline) | `curve` | `worldedit.brush.spline` |
| [`copypaste`](#copypaste-brosse) | `cp` | `worldedit.brush.copypaste` |
| [`shatter`](#shatter) | `crack` | `worldedit.brush.shatter` |
| [`surfacesplatter`](#surfacesplatter) | `ssplatter` | `worldedit.brush.surfacesplatter` |
| [`layer`](#layer) | `layers` | `worldedit.brush.layer` |
| [`command`](#command) | `cmd` | `worldedit.brush.command` ⚠️ |
| [`populateschem`](#populateschem) | `popschem` | `worldedit.brush.populateschem` ⚠️ |

---

### `blob`

```
/brush blob <pattern> [rayon=5] [rugosité=50]
```

Sphère organique déformée par un bruit de Perlin : chaque utilisation donne une forme différente,
toujours pleine (pas de fragments flottants). `rugosité` entre 0 et 100.

```
/brush blob stone 6 60
/brush blob moss_block 4
```

### `line`

```
/brush line [-c] [-h] <pattern> [épaisseur=0]
```

Trace des lignes entre les blocs visés. Le **premier clic** marque le point de départ sans rien
modifier, le **second** trace la ligne. `épaisseur` est le rayon de la ligne (0 = 1 bloc).

| Option | Description |
|--------|-------------|
| `-c` | Chaîne : chaque ligne commence là où la précédente s'est arrêtée (clôtures, câbles, murets) |
| `-h` | Ne trace que l'enveloppe des lignes épaisses |

```
/brush line -c oak_fence
/brush line -h stone 2
```

### `overlay`

```
/brush overlay [-r] <pattern> [rayon=5] [couches=1]
```

Alias : `cover`. Pour chaque colonne dans le rayon, trouve le bloc exposé le plus haut entre
`y - rayon` et `y + rayon`, puis pose `couches` couches du pattern **par-dessus**. Avec `-r`, remplace
le bloc de surface et ceux en dessous sur `couches` blocs. Les colonnes obstruées (surplombs, grottes)
ne sont pas touchées.

```
/brush overlay -r grass_block 6
/brush overlay snow 8 1
```

### `fill`

```
/brush fill <pattern> [rayon=5] [profondeur=5]
```

Alias : `filldown`. Comble trous et dépressions jusqu'au niveau du bloc visé : pour chaque colonne,
l'air est rempli vers le bas jusqu'au sol, **seulement** si le sol est trouvé à moins de
`profondeur` blocs (jamais de couche flottante au-dessus d'une falaise ou d'un gouffre).

```
/brush fill water 8 10          (transforme un cratère en lac)
/brush fill dirt 5 3
```

### `drain`

```
/brush drain [-w] [rayon=5]
```

Retire tous les liquides dans une sphère. `-w` : retire aussi l'eau des blocs *waterlogged*.
Contrairement à `//drain` (remplissage par propagation), seule la zone de la brosse est affectée.

```
/brush drain -w 6
```

### `spline`

```
/brush spline [-h] [-t <tension>] <pattern> [rayon=0]
```

Alias : `curve`. Chaque clic ajoute le bloc visé comme point de contrôle. Recliquer sur le
**dernier point** (ou un bloc voisin) construit un tube lisse (spline de Catmull-Rom) passant par
tous les points, puis recommence une nouvelle courbe. Recliquer alors qu'il n'y a qu'un point
l'efface.

| Option | Défaut | Description |
|--------|--------|-------------|
| `rayon` | 0 | Rayon du tube |
| `-t <tension>` | 0 | Tension de la courbe, entre -1 et 1 |
| `-h` | — | Seulement l'enveloppe du tube |

```
/brush spline stone 2
/brush spline -t 0.5 -h glass 3
```

### `copypaste` (brosse)

```
/brush copypaste [-a] [-k] [rayon=5]
```

Alias : `cp`. Le premier clic copie une sphère centrée sur le bloc visé (avec les données NBT), le
clic suivant la colle. Par défaut, chaque collage vide la copie (le clic suivant recopie).

| Option | Description |
|--------|-------------|
| `-a` | Ne colle pas l'air |
| `-k` | Garde la copie pour la coller plusieurs fois |

```
/brush copypaste -a -k 4
```

### `shatter`

```
/brush shatter [-w <largeur>] <pattern> [rayon=6] [fragments=8]
```

Alias : `crack`. Découpe la sphère en cellules de Voronoï autour de points aléatoires et remplace
les blocs non-air situés à la frontière entre deux cellules : effet de roche brisée ou de sol fissuré.

| Option | Défaut | Description |
|--------|--------|-------------|
| `fragments` | 8 | Nombre de fragments, de 2 à 64 |
| `-w <largeur>` | 1 | Largeur des fissures (> 0, décimal accepté) |

```
/brush shatter air 8 10          (ouvre des crevasses)
/brush shatter -w 1.5 magma_block 6
```

### `surfacesplatter`

```
/brush surfacesplatter <pattern> [rayon=8] [taches=6] [tailleTache=3]
```

Alias : `ssplatter`. Peint des taches irrégulières sur les blocs de surface : `taches` centres
choisis au hasard (1 à 64 par clic), chacun avec un rayon aléatoire jusqu'à `tailleTache` (≥ 1) et
un bord irrégulier. Idéal pour casser un sol uniforme.

```
/brush surfacesplatter gravel,coarse_dirt 10 6 3
```

### `layer`

```
/brush layer [-c] <rayon> <pattern1> [pattern2 ...]
```

Alias : `layers`. Applique jusqu'à 64 patterns en couches (séparés par des espaces).

- **Mode surface** (défaut) : les blocs non-air de la sphère reçoivent un pattern selon leur
  profondeur sous l'air le plus proche : le 1er pattern pour les blocs touchant l'air, le 2e un bloc
  plus bas, etc. Les blocs plus profonds ne sont pas modifiés.
- **Mode concentrique** (`-c`) : tous les blocs de la sphère sont posés ; le 1er pattern forme le
  noyau, le dernier la coquille extérieure (comme une planète).

`/material` n'a pas d'effet sur cette brosse.

```
/brush layer 6 grass_block dirt dirt
/brush layer -c 8 magma_block stone dirt grass_block
```

### `command`

```
/brush command [-s <taille>] <commandes...>
```

Alias : `cmd` — Permission : `worldedit.brush.command` ⚠️

Exécute des **commandes WorldEdit** au bloc visé, en tant que le joueur qui utilise la brosse
(ses permissions et limites s'appliquent). Plusieurs commandes se séparent par `;`. Avant chaque
exécution, ces variables sont remplacées :

| Variable | Valeur |
|----------|--------|
| `{x}` `{y}` `{z}` | Coordonnées du bloc visé |
| `{size}` | Taille de la brosse (`-s`, défaut 0, modifiable avec `/size`) |
| `{world}` | Nom du monde |
| `{player}` | Nom du joueur |

Mettez les commandes entre guillemets si elles contiennent des options (`-h`...). Le `/` initial
est ajouté s'il manque. Seules les commandes WorldEdit sont exécutées (les commandes d'autres plugins
ne passent pas par ce mécanisme).

```
/brush command -s 3 "//pos1 {x},{y},{z}; //pos2 {x},{y},{z}; //outset {size}"
/brush cmd -s 4 "//sphere -h glass {size}"
```

⚠️ Un clic peut déclencher plusieurs opérations lourdes : à réserver aux joueurs de confiance.

### `populateschem`

```
/brush populateschem [-r] [-a] [-s <espacement>] <schematics> [rayon=8] [densité=5]
```

Alias : `popschem` — Permission : `worldedit.brush.populateschem` ⚠️

Disperse des schematics au hasard sur la surface (arbres, rochers, décors).

| Argument / option | Défaut | Description |
|-------------------|--------|-------------|
| `schematics` | — | Fichiers ou **dossiers** du dossier schematics, séparés par `,` (sans espace) ; `#clipboard` = votre presse-papiers |
| `rayon` | 8 | Rayon de la brosse |
| `densité` | 5 | Chance (0 à 100 %) qu'une colonne de surface reçoive une schematic |
| `-s <n>` | 4 | Distance minimale entre deux schematics (≥ 0) |
| `-r` | — | Rotation aléatoire de chaque schematic (multiple de 90°) |
| `-a` | — | Ne colle pas l'air des schematics |

Chaque schematic est centrée horizontalement sur sa colonne, sa couche la plus basse posée sur la
surface. Un dossier ajoute tous les fichiers de schematic qu'il contient directement (non récursif).
Au plus **64** schematics au total ; chacune doit tenir dans `max-brush-radius`. Les fichiers sont
chargés en arrière-plan ; la brosse est liée une fois le chargement terminé.

```
/brush populateschem -r trees 12 5
/brush popschem -r -a -s 6 rochers,#clipboard 10 3
```

⚠️ Lit des fichiers du serveur et peut coller de gros volumes : à réserver aux joueurs de confiance.

---

## Nouveaux outils

Les outils se lient à l'objet tenu en main avec `/tool <nom>` et se retirent avec `/tool none`.
Ces trois outils n'existent **que** sous la forme `/tool <nom>` (pas de commande globale dépréciée).

| Outil | Alias | Permission |
|-------|-------|------------|
| [`/tool measure`](#tool-measure) | `/tool ruler` | `worldedit.tool.measure` |
| [`/tool inspect`](#tool-inspect) | — | `worldedit.tool.inspect` |
| [`/tool copypaste`](#tool-copypaste) | — | `worldedit.tool.copypaste` |

### `/tool measure`

```
/tool measure
```

Outil de mesure à distance :

- **Clic gauche** : démarre une nouvelle mesure au bloc visé.
- **Clic droit** : ajoute le bloc visé au tracé.

Après chaque point : longueur du dernier segment (avec dx, dy, dz) et du tracé total, taille et
volume de la boîte englobante de tous les points, et, à partir de 3 points, l'aire horizontale du
polygone formé. Au plus 256 points par mesure.

### `/tool inspect`

```
/tool inspect
```

**Clic droit** sur un bloc : affiche son identifiant et sa position, ses propriétés d'état, un
résumé de ses données NBT (cliquable pour copier le NBT complet), son biome, la lumière de bloc et la
lumière au-dessus.

### `/tool copypaste`

```
/tool copypaste [-a]
```

- **Clic gauche** : copie votre sélection dans le presse-papiers, avec le bloc visé comme
  **origine**.
- **Clic droit** : colle le presse-papiers de sorte que son origine tombe sur le bloc visé.

`-a` : ne colle pas l'air. Les collages sont annulables avec `//undo` ; la copie respecte
`//limit`.

```
//sel cuboid
/tool copypaste -a
```

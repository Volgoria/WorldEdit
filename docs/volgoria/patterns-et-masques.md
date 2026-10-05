# Nouveaux patterns et masques

[← Retour à l'accueil du fork](README.md)

Ces syntaxes s'utilisent partout où WorldEdit attend un pattern (`//set`, `//replace`, brosses,
`//sphere`...) ou un masque (`//replace`, `//gmask`, `/mask`, `-m`...). Les syntaxes d'origine
(`50%stone,50%dirt`, `#copy`, `^oak_log`, `>grass_block`, `!air`, `$plains`...) sont inchangées.

## Syntaxe commune : arguments entre crochets

Les nouvelles syntaxes ont la forme `#nom[arg1][arg2]...` :

- Chaque argument est entre crochets ; un argument peut contenir lui-même des crochets équilibrés
  (états de bloc, patterns ou masques imbriqués) : `#checker[oak_stairs[facing=north]][stone]`.
- Les **listes de patterns** se séparent par des virgules **hors crochets** :
  `#gradient[stone,#checker[dirt][sand],andesite]`.
- Le nom est insensible à la casse. Un nombre d'arguments incorrect affiche l'usage attendu.
- La complétion par tabulation propose le nom, puis chaque argument (patterns, masques, valeurs
  courantes).
- Les nombres décimaux doivent être finis ; les bornes sont vérifiées (« out of range » sinon).

Rappel shell : dans une commande, un argument se termine au premier espace. Écrivez les patterns
et masques **sans espace**, ou entourez-les de guillemets.

---

## Patterns

| Syntaxe | Rôle |
|---------|------|
| [`#gradient`](#gradient) | Dégradé vertical tramé entre plusieurs patterns |
| [`#stripes`](#stripes) | Bandes selon un ou plusieurs axes |
| [`#checker`](#checker) | Damier 3D |
| [`#noise`](#noise) | Taches naturelles (bruit de Perlin) |
| [`#linear`](#linear) | Alternance bloc par bloc |
| [`#offset`](#offset-pattern) | Pattern évalué à une position décalée |
| [`#mask`](#mask-pattern-conditionnel) | Pattern conditionnel (si masque alors... sinon...) |
| [`#existing`](#existing) | Garde le bloc existant |

### `#gradient`

```
#gradient[<pattern>,<pattern>,...]
#gradient[<pattern>,<pattern>,...][<yDébut>]
#gradient[<pattern>,<pattern>,...][<yDébut>][<yFin>]
```

Fondu le long de l'axe Y : le premier pattern à `yDébut`, le dernier à `yFin`, les autres répartis
régulièrement. La transition entre deux patterns voisins est **tramée** de façon déterministe (la
même position donne toujours le même résultat). `yDébut > yFin` inverse le dégradé ; en dehors de
la plage, le pattern de l'extrémité la plus proche est utilisé.

- Avec un seul argument, la plage est celle de **votre sélection actuelle** (du bloc le plus bas au
  plus haut) : il faut donc une sélection au moment où le pattern est lu.
- Avec `yDébut` seul, le dégradé va de `yDébut` jusqu'au **haut de votre sélection** si vous en
  avez une dans ce monde, sinon jusqu'au **haut du monde**.

```
//set #gradient[stone,andesite,diorite]
//set #gradient[stone,andesite,diorite][64]
//sphere #gradient[deepslate,stone,cobblestone][40][80] 20
```

### `#stripes`

```
#stripes[<axes>][<pattern>,<pattern>,...]
#stripes[<axes>][<pattern>,<pattern>,...][<épaisseur>]
```

Alterne les patterns par bandes de `épaisseur` blocs (défaut 1, ≥ 1). `axes` combine `x`, `y`, `z`,
chacun pouvant être précédé de `-` :

| Axes | Résultat |
|------|----------|
| `y` | Couches horizontales |
| `x` ou `z` | Bandes verticales |
| `xz` | Diagonales |
| `x-z` | Diagonales dans l'autre sens |
| `xyz` | Diagonales 3D |

La bande d'une position est `(x·ax + y·ay + z·az) / épaisseur` modulo le nombre de patterns.

```
//set #stripes[y][white_terracotta,orange_terracotta,red_terracotta][2]
//walls #stripes[xz][quartz_block,smooth_quartz]
```

### `#checker`

```
#checker[<pattern>][<pattern>]
#checker[<pattern>][<pattern>][<taille>]
```

Damier 3D de cellules de `taille` blocs de côté (défaut 1, ≥ 1).

```
//set #checker[black_concrete][white_concrete]
//faces #checker[stone_bricks][polished_andesite][2]
```

### `#noise`

```
#noise[<échelle>][<pattern>,<pattern>,...]
```

Choisit entre les patterns grâce à un bruit de Perlin (graine aléatoire à chaque lecture du pattern),
ce qui donne des **taches organiques** au lieu d'un mélange bloc par bloc. Le bruit est égalisé pour
que chaque pattern couvre à peu près la même proportion. `échelle` entre 0,01 et 10000 : plus elle
est grande, plus les taches sont grandes.

```
//set #noise[8][grass_block,moss_block,podzol]
/brush sphere #noise[4][stone,andesite,tuff] 5
```

### `#linear`

```
#linear[<pattern>,<pattern>,...]
```

Fait défiler les patterns bloc par bloc selon `(x + y + z)` modulo le nombre de patterns : deux blocs
voisins diffèrent toujours et le résultat ne dépend pas de l'ordre de pose.

```
//set #linear[red_wool,white_wool]
```

### `#offset` (pattern)

```
#offset[<x>][<y>][<z>][<pattern>]
```

Utilise le bloc que donne `pattern` à la position décalée de `(x, y, z)`. Combiné à `#existing`,
recopie un bloc voisin.

```
//set #offset[0][1][0][#existing]         (chaque bloc devient celui du dessus)
//set #offset[5][0][0][#copy]             (presse-papiers décalé de 5 en X)
```

### `#mask` (pattern conditionnel)

```
#mask[<masque>][<pattern>]
#mask[<masque>][<pattern>][<patternSinon>]
```

Utilise `pattern` là où le masque correspond, `patternSinon` ailleurs. Sans troisième argument, les
autres blocs sont **laissés tels quels**. Le masque peut être une intersection (masques séparés par
des espaces à l'intérieur des crochets ; dans ce cas, entourez l'argument de guillemets).

```
//set #mask[#angle[40][90]][stone][grass_block]          (pierre sur les pentes raides, herbe ailleurs)
//replace grass_block #mask[#y[*][62]][sand]              (herbe → sable sous Y 63)
```

### `#existing`

```
#existing
```

Renvoie le bloc déjà présent (ne change rien). Utile dans `#offset`, `#mask` ou une liste pondérée :

```
//set 20%moss_block,80%#existing        (mousse sur 20 % des blocs, le reste intact)
```

---

## Masques

| Syntaxe | Rôle |
|---------|------|
| [`#x` `#y` `#z`](#x-y-z) | Plage de coordonnées |
| [`#angle`](#angle) | Pente du terrain |
| [`#adjacent`](#adjacent) | Nombre de voisins correspondant à un masque |
| [`#offset`](#offset-masque) | Bloc décalé correspondant à un masque |
| [`#radius`](#radius) | Distance à la position de placement |
| [`#wall` `#floor`/`#top` `#ceiling`/`#roof`](#wall-floor-ceiling) | Blocs exposés à l'air sur un côté, dessus, dessous |
| [`#liquid` `#opaque` `#transparent`](#liquid-opaque-transparent) | Selon le matériau |
| [`%<pourcentage>`](#modifications-des-masques-existants) | Désormais avec décimales |

### `#x` `#y` `#z`

```
#y[<min|*>][<max|*>]
```

Blocs dont la coordonnée sur l'axe est dans la plage **inclusive**. `*` = pas de limite. Les bornes
peuvent être données dans n'importe quel ordre.

```
//replace #y[*][62] sand                        (tout bloc non-air sous Y 63 devient du sable)
//gmask #y[64][*]                               (ne modifier que Y ≥ 64)
//set #mask[#x[0][*]][red_wool][blue_wool]      (rouge côté X ≥ 0, bleu ailleurs)
```

Attention : `//replace` prend un seul masque ; pour combiner, utilisez une intersection entre
guillemets : `//replace "#y[*][62] stone" sand`.

### `#angle`

```
#angle[<min°>][<max°>]
#angle[<min°>][<max°>][<distance>]
```

Blocs **solides** dont la pente locale du terrain (en degrés, 0 = plat, 90 = vertical) est dans la
plage. La pente est mesurée en comparant la hauteur de surface de la colonne avec celle des quatre
colonnes situées à `distance` blocs (1 à 16, défaut 1) en X et Z ; la plus forte différence compte.
Angles de 0 à 90, dans n'importe quel ordre.

```
//replace #angle[40][90] stone
/brush sphere grass_block 6
/mask #angle[0][20]
```

### `#adjacent`

```
#adjacent[<masque>]
#adjacent[<masque>][<min>]
#adjacent[<masque>][<min>][<max>]
```

Blocs dont le nombre de voisins (6 faces) correspondant au masque est entre `min` (défaut 1) et
`max` (défaut 6), bornes incluses, de 0 à 6.

```
//replace #adjacent[water] sand                  (blocs au contact de l'eau)
//replace "stone #adjacent[air][3]" cobblestone   (pierre très exposée)
```

### `#offset` (masque)

```
#offset[<x>][<y>][<z>][<masque>]
```

Blocs dont le bloc situé au décalage `(x, y, z)` correspond au masque. Généralise `>masque`
(`#offset[0][-1][0][masque]`) et `<masque` (`#offset[0][1][0][masque]`).

```
//replace "#offset[0][2][0][air] grass_block" moss_block
```

### `#radius`

```
#radius[<max>]
#radius[<min>][<max>]
```

Blocs dont la distance à votre **position de placement** (comme `//sphere`, voir `//placement`) est
entre `min` (défaut 0) et `max`, bornes incluses. Le centre est **figé au moment où le masque est
lu** : un `//gmask #radius[30]` reste centré là où vous étiez.

```
//gmask #radius[50]
//replace #radius[10][12] glass
```

### `#wall` `#floor` `#ceiling`

| Masque | Alias | Blocs non-air ayant de l'air... |
|--------|-------|-------------------------------|
| `#wall` | — | sur au moins un côté horizontal (N, S, E, O) |
| `#floor` | `#top` | juste au-dessus |
| `#ceiling` | `#roof` | juste en dessous |

```
//replace #floor grass_block
//replace "#wall stone" mossy_cobblestone
```

### `#liquid` `#opaque` `#transparent`

| Masque | Blocs |
|--------|-------|
| `#liquid` | Liquides (eau, lave...) |
| `#opaque` | Blocs opaques |
| `#transparent` | Blocs non opaques (**y compris l'air**) |

```
//replace #liquid air
//gmask #opaque
```

### Modifications des masques existants

- **Intersections** : les masques séparés par des espaces sont coupés **hors crochets** seulement,
  ce qui permet des masques imbriqués contenant des espaces, par exemple
  `"#mask[stone #y[0][64]][sand]"` (guillemets nécessaires à cause de l'espace).
- **`%<pourcentage>`** accepte désormais les décimales (`%12.5`), entre 0 et 100.
- Les préfixes `!`, `>`, `<`, `~`, `=`, `$` et `%` ont été réécrits sur une base commune ; leur syntaxe
  ne change pas.

---

## Combinaisons utiles

```
# Texturer une montagne : pierre sur les pentes, herbe ailleurs
//set #mask[#angle[40][90]][stone][grass_block]

# Pentes en dégradé, sol plat inchangé
//replace #angle[35][90] #gradient[stone,andesite,tuff]

# Stries de canyon sur les falaises seulement
//replace "#wall #angle[60][90]" #stripes[y][terracotta,orange_terracotta,brown_terracotta][2]

# Couche de neige sur les sommets au-dessus de Y 150
//replace "#floor #y[150][*]" snow_block

# Mousse naturelle sur les toits
//replace #floor #noise[6][moss_block,#existing]

# Brosse qui ne peint que les sols plats, sous le niveau 100
/brush sphere #noise[5][grass_block,podzol] 6
/mask "#angle[0][25] #y[*][100]"
```

## Presets dans le menu

Le menu [`/wegui presets`](gui.md#patterns--masques) construit automatiquement la plupart de ces
syntaxes à partir des blocs choisis dans le sélecteur de blocs.

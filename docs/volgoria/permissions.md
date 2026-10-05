# Permissions ajoutées par le fork

[← Retour à l'accueil du fork](README.md)

Les permissions d'origine de WorldEdit sont inchangées (voir
<https://worldedit.enginehub.org/en/latest/permissions/>). Le tableau ci-dessous liste **toutes**
les permissions introduites par le fork.

- Seule `worldedit.gui` est déclarée dans `plugin.yml`, avec la valeur par défaut `op`. Les autres
  n'ont pas de valeur par défaut : seuls les opérateurs (ou `*`) les possèdent tant qu'aucun plugin de
  permissions ne les accorde.
- ⚠️ = permission sensible (charge serveur importante, accès aux fichiers, ou suppression de
  données). À réserver aux administrateurs ou aux bâtisseurs de confiance.

## Tableau complet

### Interface

| Permission | Accorde |
|------------|---------|
| `worldedit.gui` | Ouvrir `/wegui` et `//menu`. Chaque bouton exige en plus la permission de la commande qu'il exécute |

### Génération

| Permission | Commande |
|------------|----------|
| `worldedit.generation.torus` | `//torus` |
| `worldedit.generation.disk` | `//disk` |
| `worldedit.generation.dome` | `//dome` |
| `worldedit.generation.helix` | `//helix` |
| `worldedit.generation.arch` | `//arch` |

### Région et historique

| Permission | Commande |
|------------|----------|
| `worldedit.region.wireframe` | `//wireframe` |
| `worldedit.region.surface` | `//surface` |
| `worldedit.history.list` | `//history` (son propre historique) |
| `worldedit.history.list.other` | `//history <joueur>` (en plus de `worldedit.history.list`) — révèle l'activité des autres |

`//paste -r` n'a pas de permission propre (`worldedit.clipboard.paste`).

### Construction

| Permission | Commande |
|------------|----------|
| `worldedit.build.text` | `//text` |
| `worldedit.build.symmetry` | `//symmetry` |
| `worldedit.build.flatten` | `//flatten` |
| `worldedit.build.terrain` | `//terrain` |
| `worldedit.build.path` | `//path` |
| `worldedit.build.cave` | `//cave` |
| `worldedit.build.staircase` | `//staircase` |
| `worldedit.build.spiralstairs` | `//spiralstairs` |
| `worldedit.build.randomize` | `//randomize` |

### Images

| Permission | Commande | Remarque |
|------------|----------|----------|
| `worldedit.image.paste` ⚠️ | `//image` | Jusqu'à 1024 × 1024 blocs par commande, sans permission par taille : seuls `//limit` / la limite de blocs de la configuration la bornent |
| `worldedit.image.export` | `//topview` | Écrit des PNG dans `images/` (écrasement avec `-f`) ; sélections jusqu'à 4096 × 4096 colonnes |
| `worldedit.image.heightmap.import` ⚠️ | `//heightmap import` | Peut remplir toute la hauteur d'une sélection jusqu'à 4096 × 4096 colonnes |
| `worldedit.image.heightmap.export` | `//heightmap export` | Écrit des PNG dans `images/` (écrasement avec `-f`) |

### Schematics

| Permission | Commande | Remarque |
|------------|----------|----------|
| `worldedit.schematic.info` | `/schem info` | Lecture seule |
| `worldedit.schematic.rename` ⚠️ | `/schem rename` | Déplace / renomme des fichiers partagés |
| `worldedit.schematic.copy` | `/schem copy` | Crée des fichiers |

Rappels sur les permissions d'origine liées :

- ⚠️ `worldedit.schematic.delete` permet `/schem delete` **et** est exigée pour **écraser** un
  fichier existant avec `/schem save -f`, `/schem rename -f` et `/schem copy -f`.
- Les nouveaux formats (`structure`, `obj`, `json`) passent par les permissions existantes
  `worldedit.schematic.save` / `worldedit.clipboard.save` et `worldedit.schematic.load` /
  `worldedit.clipboard.load`.
- Les filtres de `/schem list` utilisent `worldedit.schematic.list`.

### Brosses

| Permission | Brosse |
|------------|--------|
| `worldedit.brush.blob` | `/brush blob` |
| `worldedit.brush.line` | `/brush line` |
| `worldedit.brush.overlay` | `/brush overlay` |
| `worldedit.brush.fill` | `/brush fill` |
| `worldedit.brush.drain` | `/brush drain` |
| `worldedit.brush.spline` | `/brush spline` |
| `worldedit.brush.copypaste` | `/brush copypaste` |
| `worldedit.brush.shatter` | `/brush shatter` |
| `worldedit.brush.surfacesplatter` | `/brush surfacesplatter` |
| `worldedit.brush.layer` | `/brush layer` |
| `worldedit.brush.command` ⚠️ | `/brush command` — chaque clic peut lancer plusieurs commandes WorldEdit (avec les permissions du joueur) |
| `worldedit.brush.populateschem` ⚠️ | `/brush populateschem` — lit des fichiers du dossier schematics et colle jusqu'à 64 schematics différentes |

### Outils

| Permission | Outil |
|------------|-------|
| `worldedit.tool.measure` | `/tool measure` |
| `worldedit.tool.inspect` | `/tool inspect` (affiche le NBT complet des blocs, ex. contenu des coffres) |
| `worldedit.tool.copypaste` | `/tool copypaste` |

---

## Configuration LuckPerms conseillée

Deux groupes : **builder** (bâtisseurs) et **admin** (hérite de builder). Les permissions d'origine
de WorldEdit dont les bâtisseurs ont besoin (sélection, `//set`, presse-papiers, historique, brosses
de base...) sont à ajouter selon vos habitudes ; seules les nouvelles sont listées ici. LuckPerms
gère les jokers (`worldedit.build.*`) par défaut.

### Groupe `builder`

```sh
lp creategroup builder

# Menu
lp group builder permission set worldedit.gui true

# Génération, région, historique
lp group builder permission set worldedit.generation.torus true
lp group builder permission set worldedit.generation.disk true
lp group builder permission set worldedit.generation.dome true
lp group builder permission set worldedit.generation.helix true
lp group builder permission set worldedit.generation.arch true
lp group builder permission set worldedit.region.wireframe true
lp group builder permission set worldedit.region.surface true
lp group builder permission set worldedit.history.list true

# Construction
lp group builder permission set worldedit.build.* true

# Brosses (sans command ni populateschem)
lp group builder permission set worldedit.brush.blob true
lp group builder permission set worldedit.brush.line true
lp group builder permission set worldedit.brush.overlay true
lp group builder permission set worldedit.brush.fill true
lp group builder permission set worldedit.brush.drain true
lp group builder permission set worldedit.brush.spline true
lp group builder permission set worldedit.brush.copypaste true
lp group builder permission set worldedit.brush.shatter true
lp group builder permission set worldedit.brush.surfacesplatter true
lp group builder permission set worldedit.brush.layer true

# Outils
lp group builder permission set worldedit.tool.measure true
lp group builder permission set worldedit.tool.inspect true
lp group builder permission set worldedit.tool.copypaste true

# Schematics (sans rename ni delete)
lp group builder permission set worldedit.schematic.info true
lp group builder permission set worldedit.schematic.copy true

# Images : exports seulement
lp group builder permission set worldedit.image.export true
lp group builder permission set worldedit.image.heightmap.export true
```

Pour limiter la charge, fixez aussi une limite de blocs : `limits.max-blocks-changed.default` et
`limits.max-blocks-changed.maximum` dans `config.yml` (s'applique à tous ceux qui n'ont pas
`worldedit.limit.unrestricted` ; ne donnez pas cette dernière aux bâtisseurs).

### Groupe `admin`

```sh
lp creategroup admin
lp group admin parent add builder

# Permissions sensibles ⚠️
lp group admin permission set worldedit.brush.command true
lp group admin permission set worldedit.brush.populateschem true
lp group admin permission set worldedit.image.paste true
lp group admin permission set worldedit.image.heightmap.import true
lp group admin permission set worldedit.schematic.rename true
lp group admin permission set worldedit.schematic.delete true      # suppression ET écrasement (-f)
lp group admin permission set worldedit.history.list.other true
```

Variante plus simple pour les administrateurs de confiance totale :

```sh
lp group admin permission set worldedit.* true
```

### Attribuer les groupes

```sh
lp user Alex parent add builder
lp user Steve parent add admin
```

### Conseils

- `worldedit.gui` seul ne donne rien : un bâtisseur sans `worldedit.region.set` verra le bouton
  *Set* du menu échouer avec le message de permission habituel.
- Si des bâtisseurs ont besoin de `//image`, accordez `worldedit.image.paste` avec une limite de blocs
  raisonnable (1024 × 1024 = plus d'un million de blocs possibles).
- `worldedit.tool.inspect` révèle le NBT des blocs (contenu des coffres, livres...) : retirez-la des
  bâtisseurs si c'est sensible sur votre serveur.

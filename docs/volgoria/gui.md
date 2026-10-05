# Menu `/wegui` (`//menu`)

[← Retour à l'accueil du fork](README.md)

Un menu en coffre qui donne accès aux fonctions de WorldEdit sans taper de commandes. Disponible
uniquement sur **Bukkit / Spigot / Paper / Folia**.

## Ouvrir le menu

```
/wegui [section]
//menu [section]
```

| Section | Synonymes acceptés | Menu ouvert |
|---------|--------------------|-------------|
| *(aucune)* | — | Menu principal |
| `brushes` | `brush` | [Brosses](#brosses) |
| `tools` | `tool` | [Outils](#outils) |
| `blocks` | `patterns`, `pattern` | [Sélecteur de blocs](#sélecteur-de-blocs) |
| `presets` | `masks`, `mask` | [Patterns & Masques](#patterns--masques) |
| `settings` | `session` | [Réglages](#réglages) |
| `selection` | `region` | [Sélection & Région](#sélection--région) |
| `generation` | `generate`, `gen` | [Génération](#génération) |
| `build` | `builder` | [Construction](#construction) |
| `schematics` | `schematic`, `schem` | [Schematics](#schematics) |
| `images` | `image` | [Images](#images) |

La complétion par tabulation propose les sections. Seuls les joueurs peuvent ouvrir le menu.

## Permissions

- **`worldedit.gui`** (défaut : `op`) est nécessaire pour ouvrir le menu.
- Le menu **n'accorde aucun droit supplémentaire** : chaque bouton exécute une vraie commande
  WorldEdit au nom du joueur (via le gestionnaire de commandes de WorldEdit), donc la permission de
  cette commande, `//limit`, `//gmask`, l'historique et `//undo` s'appliquent normalement. Un bouton
  dont la permission manque affiche simplement l'erreur habituelle de WorldEdit.
- La commande exécutée est affichée en gris foncé dans la description de chaque bouton.

## Principes d'utilisation

### Clics

- Tous les clics et glisser-déposer sont **annulés** tant qu'un menu est ouvert : impossible de
  prendre une icône ou de déposer un objet, y compris depuis son propre inventaire (shift-clic,
  touches numériques). Le double-clic est ignoré.
- Le clic est traité au tick suivant (un son de bouton confirme l'action).
- Sauf mention contraire, **clic gauche** et **clic droit** font la même chose. Les boutons qui
  distinguent les clics l'indiquent en jaune dans leur description.
- Boutons **−1 / +1** (vitre rouge / verte) : clic = ±1, **shift-clic = ±5**. Les tailles sont
  limitées à `[1, max]`, où `max` est la limite correspondante de la configuration WorldEdit
  (`max-brush-radius` pour les brosses, `max-radius` pour le reste), plafonnée à **100**.
- Bouton **Retour** (flèche) : revient au menu principal (ou au menu d'origine) ; **Fermer** (barrière).
- Les fichiers et listes longues sont **paginés** : flèches *Previous page* / *Next page* en bas à
  gauche et à droite, numéro de page au milieu.

### Comportement des boutons d'action

| Type | Effet |
|------|-------|
| Exécuter | Lance la commande et laisse le menu ouvert (rafraîchi) |
| Exécuter et fermer | Ferme le menu pour que vous puissiez lire le résultat dans le chat, puis lance la commande |
| Lier | Vérifie l'objet tenu, ferme le menu et lie la brosse / l'outil à l'objet tenu |

### Questions dans le chat

Certains boutons demandent une valeur (nom de fichier, texte, nombre, pattern) :

1. Le menu se ferme et la question s'affiche dans le chat.
2. Tapez la réponse dans le chat : le message n'est **pas** diffusé aux autres joueurs.
3. Tapez **`cancel`** pour annuler et revenir au menu.
4. Sans réponse au bout de **60 secondes**, la question est annulée et le menu se rouvre.

Les réponses sont validées avant d'être transformées en commande ; une réponse invalide affiche une
erreur et rouvre le menu :

| Type de valeur | Règle |
|----------------|-------|
| Texte libre | Non vide, au plus 200 caractères, sans guillemet `"`, antislash `\`, code couleur `§` ni caractère de contrôle ; transmis comme un seul argument entre guillemets |
| Nouveau nom de fichier | Lettres, chiffres, `_` et `-`, sous-dossiers séparés par `/`, **sans extension**, 64 caractères max |
| Fichier existant | Comme ci-dessus, plus une extension facultative (`ile.png`), 80 caractères max |
| Liste de fichiers | Fichiers ou dossiers séparés par `,` (ou `#clipboard`), 256 caractères max |
| Nombre | Entier ≥ -1 (-1 = pas de limite) |
| Pattern / masque | Un seul argument **sans espace**, 256 caractères max (WorldEdit valide ensuite la syntaxe) |

Conséquence : les intersections de masques (qui contiennent des espaces) ne peuvent pas être
saisies dans le menu ; utilisez la commande directement.

### Choix mémorisés

Le menu retient pour chaque joueur, **en mémoire uniquement** (perdu à la déconnexion ou au
redémarrage) : le pattern choisi (défaut `stone`), le masque de remplacement, la taille de brosse
(défaut 3), le mode creux des brosses, la taille de génération (défaut 5) et son mode creux, la
portée des outils (défaut 10), la taille de construction (défaut 5), la valeur des presets
(défaut 4), le format d'enregistrement (défaut `sponge`) et les recherches.

### Lier une brosse ou un outil (survie et créatif)

WorldEdit ne peut pas lier un outil à la main vide ni à un bloc. Avant de lier :

- si vous tenez déjà un objet qui n'est pas un bloc (bâton, os...), il est utilisé ;
- en **créatif**, une *blaze rod* nommée « WorldEdit Tool » est placée dans l'emplacement de main
  vide ou le premier emplacement libre de la barre d'action, puis sélectionnée ;
- en **survie / aventure**, le menu ne donne **jamais** d'objet : il vous demande de tenir un
  objet non-bloc (ou de libérer un emplacement). Aucune autre différence : en survie, ce sont les
  permissions WorldEdit qui décident de ce qui est possible.

---

## Menu principal

En haut, une hache en bois résume : pattern, masque, taille de brosse, taille de génération.

| Bouton | Ouvre |
|--------|-------|
| Brushes (pinceau) | [Brosses](#brosses) |
| Tools (bâton de blaze) | [Outils](#outils) |
| Blocks (icône du pattern) | [Sélecteur de blocs](#sélecteur-de-blocs) |
| Patterns & Masks (verre teinté) | [Patterns & Masques](#patterns--masques) |
| Settings (comparateur) | [Réglages](#réglages) |
| Selection & Region (hache en or) | [Sélection & Région](#sélection--région) |
| Generation (balise) | [Génération](#génération) |
| Build (briques) | [Construction](#construction) |
| Schematics (bibliothèque) | [Schematics](#schematics) |
| Images (tableau) | [Images](#images) |

## Barre du bas des menus d'action

Les menus Brosses, Outils, Construction, Sélection et Génération partagent une ligne du bas :

| Emplacement | Bouton |
|-------------|--------|
| 1 | Retour |
| 2 | Pattern courant (ouvre le sélecteur de blocs) — si un bouton du menu utilise un pattern |
| 3 / 4 / 5 | −1, taille courante, +1 |
| 6 | Interrupteur **Hollow** (creux) — brosses et génération |
| 7 | Masque de remplacement (Sélection) ou bouton « Unbind » (Brosses : `/brush none`, Outils : `/tool none`) |
| 9 | Fermer |

Survoler un bouton d'action montre sa description, le pattern / masque / taille utilisés et
l'aperçu de la commande.

## Brosses

Chaque bouton **lie** la brosse à l'objet tenu (`/brush ...`). Taille : « Brush size ».
`{h}` = `-h ` si *Hollow* est activé.

| Bouton | Commande exécutée |
|--------|-------------------|
| Sphere | `/brush sphere {h}<pattern> <taille>` |
| Cylinder | `/brush cylinder {h}<pattern> <taille> <taille>` |
| Smooth | `/brush smooth <taille> 4` |
| Blob | `/brush blob <pattern> <taille>` |
| Splatter | `/brush splatter <pattern> <taille>` |
| Overlay | `/brush overlay <pattern> <taille>` |
| Fill | `/brush fill <pattern> <taille>` |
| Drain | `/brush drain <taille>` |
| Line | `/brush line {h}<pattern> <taille-1>` |
| Raise / Lower | `/brush raise sphere <taille>` / `/brush lower sphere <taille>` |
| Erode / Dilate | `/brush erode <taille>` / `/brush dilate <taille>` |
| Gravity | `/brush gravity <taille>` |
| Snow | `/brush snow sphere <taille>` |
| Extinguish | `/brush extinguish <taille>` |
| Forest | `/brush forest sphere <taille> 20 oak` |
| Butcher | `/brush butcher <taille>` |
| Clipboard | `/brush clipboard -a` |
| Spline | `/brush spline {h}<pattern> <taille-1>` |
| Copy-paste | `/brush copypaste -a <taille>` |
| Shatter | `/brush shatter <pattern> <taille> 8` |
| Surface splatter | `/brush surfacesplatter <pattern> <taille> 6 3` |
| Layer | `/brush layer <taille> <blocs choisis séparés par des espaces>` (ou le pattern tapé à la main) |
| Populate schematics | `/brush populateschem -r -a <réponse> <taille> 5` — demande dans le chat la liste des schematics / dossiers (ou `#clipboard`) |
| Command | `/brush command -s <taille> "<réponse>"` — demande dans le chat les commandes séparées par `;` (`{x} {y} {z} {size}` sont remplacés au clic) |

## Outils

Chaque bouton **lie** l'outil (`/tool ...`). Taille : « Range ».

| Bouton | Commande |
|--------|----------|
| Selection wand | `/tool selwand` |
| Navigation wand | `/tool navwand` |
| Far wand | `/tool farwand` |
| Block info | `/tool info` |
| Inspector | `/tool inspect` |
| Measuring tape | `/tool measure` (clic gauche : nouvelle mesure, clic droit : ajouter un point) |
| Tree planter | `/tool tree` |
| Replacer | `/tool repl <pattern>` |
| Long-range builder | `/tool lrbuild <pattern> air` |
| Data cycler | `/tool cycler` |
| Flood fill | `/tool floodfill <pattern> <range>` |
| Floating tree remover | `/tool deltree` |
| Stacker | `/tool stacker <range>` |
| Copy-paste tool | `/tool copypaste` |

Remarque : avec WorldEdit, `/tool lrbuild <a> <b>` pose `<a>` au **clic droit** et `<b>` au
**clic gauche** ; le bouton « Long-range builder » pose donc votre pattern au clic droit et de l'air
au clic gauche.

## Construction

Boutons « exécuter et fermer ». Taille : « Size ». `<tiers>` = taille / 3 (au moins 1).

| Bouton | Commande |
|--------|----------|
| Text | `//text -s <tiers> <pattern> "<réponse>"` — demande le texte dans le chat |
| Symmetry | `//symmetry` |
| Symmetry (skip air) | `//symmetry -a` |
| Flatten | `//flatten` |
| Terrain | `//terrain <pattern> 32 <taille>` |
| Path | `//path <pattern> <taille>` |
| Cave | `//cave <tiers>` |
| Staircase | `//staircase <pattern> <taille>` |
| Spiral stairs | `//spiralstairs <pattern> <taille> 32` |
| Randomize | `//randomize <pattern> 20` |

## Sélection & Région

| Bouton | Commande | Menu |
|--------|----------|------|
| Wand | `//wand` | se ferme |
| Position 1 here / Position 2 here | `//pos1` / `//pos2` | reste ouvert |
| Position 1 (target) / Position 2 (target) | `//hpos1` / `//hpos2` | reste ouvert |
| Cuboid selection / Polygon selection | `//sel cuboid` / `//sel poly` | reste ouvert |
| Clear selection | `//desel` | reste ouvert |
| Expand vertically | `//expand vert` | reste ouvert |
| Selection size | `//size` | se ferme |
| Block distribution | `//distr` | se ferme |
| Set | `//set <pattern>` | reste ouvert |
| Replace | `//replace [masque] <pattern>` (sans masque : tout bloc non-air) | reste ouvert |
| Walls / Faces / Wireframe | `//walls` / `//faces` / `//wireframe <pattern>` | reste ouvert |
| Undo / Redo | `//undo` / `//redo` | reste ouvert |
| History | `//history` | se ferme |

Bouton **masque de remplacement** (verre) : clic gauche pour taper un masque dans le chat, clic droit
pour l'effacer.

## Génération

Boutons « exécuter et fermer », à votre position. Taille : « Size » ; une rangée de raccourcis
fixe la taille à 3, 5, 8, 10, 15, 20 ou 30 (dans la limite autorisée). `<double>` = 2 × taille.

| Bouton | Commande |
|--------|----------|
| Sphere | `//sphere {h}<pattern> <taille>` |
| Cylinder | `//cyl {h}<pattern> <taille> <taille>` |
| Cone | `//cone {h}<pattern> <taille> <double>` |
| Pyramid | `//pyramid {h}<pattern> <taille>` |
| Torus | `//torus {h}<pattern> <taille> <tiers>` |
| Dome | `//dome {h}<pattern> <taille>` |
| Disk | `//disk {h}<pattern> <taille>` (face à votre regard ; creux = anneau) |
| Helix | `//helix <pattern> <taille> <double> 3` |
| Arch | `//arch <pattern> <double> <taille>` |

## Sélecteur de blocs

Liste paginée de tous les blocs ayant une forme d'objet, triés par nom. Le bloc sélectionné brille.

| Clic sur un bloc | Effet |
|------------------|-------|
| Clic gauche | Utiliser **uniquement** ce bloc comme pattern |
| Shift-clic | **Ajouter** au mélange aléatoire (jusqu'à 9 blocs, ex. `stone,andesite,tuff`) |
| Clic droit | Utiliser comme **masque** de `//replace` |

Contrôles du bas :

| Bouton | Effet |
|--------|-------|
| Retour | Revient au menu d'où vous venez |
| Search (panneau) | Clic gauche : filtrer par nom (tapé dans le chat, les espaces deviennent `_`) ; clic droit : effacer le filtre |
| Pattern courant | Réinitialise le pattern à `stone` |
| Type a pattern (livre) | Taper n'importe quel pattern WorldEdit dans le chat (`50%stone,50%andesite`, `##wool`, `#gradient[...]`...) |
| Masque de remplacement | Clic gauche : taper un masque ; clic droit : effacer |
| Fermer | — |

## Patterns & Masques

Des patterns et masques prêts à l'emploi, construits à partir des blocs choisis dans le sélecteur
(les blocs du mélange, même si un pattern tapé à la main est actif). La **valeur** (1 à 64, défaut 4,
réglable avec −1/+1) sert d'épaisseur, de taille de cellule, d'échelle ou de rayon selon le preset.
Les presets qui exigent plusieurs blocs indiquent « Pick at least N blocks first » tant que le
mélange est insuffisant (shift-cliquez des blocs dans le sélecteur).

**Patterns** (clic = devient votre pattern) :

| Preset | Syntaxe générée | Blocs requis |
|--------|-----------------|--------------|
| Gradient | `#gradient[<blocs>]` (nécessite une sélection à l'utilisation) | 2 |
| Horizontal stripes | `#stripes[y][<blocs>][<valeur>]` | 2 |
| Vertical stripes | `#stripes[x][<blocs>][<valeur>]` | 2 |
| Diagonal stripes | `#stripes[xz][<blocs>][<valeur>]` | 2 |
| Checkerboard | `#checker[<bloc1>][<bloc2>][<valeur>]` | 2 |
| Noise | `#noise[<valeur>][<blocs>]` | 2 |
| Alternating | `#linear[<blocs>]` | 2 |
| Slopes | `#mask[#angle[40][90]][<bloc1>][<bloc2>]` | 2 |

**Masques** :

| Clic | Effet |
|------|-------|
| Clic gauche | Devient le masque de `//replace` du menu |
| Clic droit | Défini comme masque global (`//gmask <masque>`) |
| Shift-clic | Défini comme masque de la brosse tenue (`/mask <masque>`), le menu se ferme |

| Preset | Syntaxe générée |
|--------|-----------------|
| Your blocks | `<blocs>` |
| Above you | `#y[<votre Y>][*]` |
| Below you | `#y[*][<votre Y - 1>]` |
| East of you | `#x[<votre X>][*]` |
| South of you | `#z[<votre Z>][*]` |
| Steep slopes | `#angle[40][90]` |
| Flat ground | `#angle[0][20]` |
| Next to your blocks | `#adjacent[<blocs>]` |
| Next to air | `#adjacent[air]` |
| Walls / Floors / Ceilings | `#wall` / `#floor` / `#ceiling` |
| Liquids | `#liquid` |
| Radius | `#radius[<valeur>]` |

Les coordonnées sont celles du joueur à l'ouverture du menu. Un bouton « Clear global mask » exécute
`//gmask` sans argument.

## Réglages

| Bouton | Effet |
|--------|-------|
| Block change limit | Clic gauche : taper une limite (`//limit <n>`, -1 = aucune) ; clic droit : valeur par défaut (`//limit`) |
| Expression timeout | Idem avec `//timeout` (millisecondes) |
| Fast mode | Bascule `//fast on` / `//fast off` |
| Global mask | Affiche si un masque global est actif ; clic : l'efface (`//gmask`) |
| All side effects | `//perf -h` (liste dans le chat) |
| Effets secondaires (jusqu'à 7) | Bascule chaque effet secondaire exposé : `//perf <effet> on/off` |
| Clipboard | Taille du presse-papiers et s'il est transformé (information) |
| Copy | `//copy` |
| Paste | `//paste -a` (ferme le menu) |
| Rotate 90° / 180° / 270° | `//rotate <angle>` |
| Flip | `//flip` (dans la direction de votre regard) |
| Clear clipboard | `/clearclipboard` |
| Undo / Redo | Clic : 1 modification ; shift-clic : jusqu'à 5 (`//undo 5`) ; le nombre disponible est affiché |
| History | `//history` (ferme le menu) |
| Clear history | `//clearhistory` |

## Schematics

Navigateur du dossier des schematics (sous-dossiers inclus, 4 niveaux et 2000 fichiers au plus),
avec dossier, taille et date de modification de chaque fichier. Le dossier est relu à chaque
ouverture et avec le bouton *Refresh*.

| Clic sur un fichier | Commande |
|---------------------|----------|
| Clic gauche | `/schem load <fichier>` (charge dans le presse-papiers) |
| Shift-clic gauche | `/schem info <fichier>` |
| Clic droit | `/schem rename <fichier> <réponse>` — demande le nouveau nom |
| Shift-clic droit | `/schem copy <fichier> <réponse>` — demande le nom de la copie |

Contrôles : Retour, **Search** (filtre par nom ; clic droit pour effacer), **Save clipboard**
(demande un nom puis `/schem save <nom> [format]`, sans écraser), **Save format** (clic pour faire
défiler `sponge` → `structure` → `obj` → `json`), **Refresh**, Fermer.

## Images

Navigateur du dossier `images` (png, jpg, jpeg, gif, bmp).

| Clic sur une image | Commande |
|--------------------|----------|
| Clic gauche | `//image <fichier>` (pixel art à plat) |
| Clic droit | `//image -v <fichier>` (debout) |
| Shift-clic gauche | `//image -d <fichier>` (à plat, tramé) |
| Shift-clic droit | `//heightmap import <fichier>` (relief sur la sélection) |

Contrôles :

- **Export top view** : demande un nom puis `//topview -s <nom>` (carte ombrée de la sélection).
- **Heightmaps** : clic gauche = demande un nom puis `//heightmap export <nom>` ; clic droit =
  demande un fichier (ex. `ile.png`) puis `//heightmap import <fichier>`.
- Search, Refresh, Retour, Fermer comme pour les schematics.

---

## Folia

- Toutes les actions du menu (clics, questions, expiration des questions) sont exécutées sur le
  **planificateur du joueur** (`player.getScheduler()` sous Folia, planificateur Bukkit sinon), donc
  sur le thread de la région qui possède le joueur.
- Les tâches d'un joueur déconnecté sont abandonnées.
- À l'arrêt du plugin, les questions en attente et les choix mémorisés sont effacés. Sur
  Bukkit/Paper, les menus ouverts sont aussi fermés ; **sous Folia, ils ne le sont pas** (un
  inventaire ne peut être manipulé que depuis le thread de sa région). Comme les écouteurs du menu
  sont désactivés avec le plugin, un menu resté ouvert n'est plus protégé : évitez de recharger ou
  d'arrêter WorldEdit pendant que des joueurs ont le menu ouvert.

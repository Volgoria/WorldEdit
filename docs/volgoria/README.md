# WorldEdit — fork Volgoria

Ce dépôt (`Volgoria/WorldEdit`) est un fork de [EngineHub/WorldEdit](https://github.com/EngineHub/WorldEdit)
(branche `version/7.4.x`). Il reste compatible avec WorldEdit : toutes les commandes, permissions et
fichiers de configuration d'origine fonctionnent à l'identique. Le fork **ajoute** des fonctionnalités
pensées pour un serveur de construction Paper/Folia.

## Documentation

| Page | Contenu |
|------|---------|
| [commandes.md](commandes.md) | Toutes les nouvelles commandes (génération, région, historique, construction, images, schematics) |
| [brosses-et-outils.md](brosses-et-outils.md) | Les nouvelles brosses (`/brush ...`) et les nouveaux outils (`/tool ...`) |
| [patterns-et-masques.md](patterns-et-masques.md) | Nouvelles syntaxes de patterns (`#gradient`, `#stripes`...) et de masques (`#angle`, `#y`...) |
| [gui.md](gui.md) | Le menu coffre `/wegui` (`//menu`) |
| [formats.md](formats.md) | Formats de schematics (Sponge, structure vanilla `.nbt`, OBJ/MTL, JSON) et dossier `images` |
| [permissions.md](permissions.md) | Tableau des nouvelles permissions et configuration LuckPerms conseillée |
| [cli.md](cli.md) | L'outil en ligne de commande `worldedit-cli` |

## Ce que le fork ajoute

**Génération** — `//torus`, `//disk`, `//dome`, `//helix`, `//arch`.

**Région / presse-papiers / historique** — `//wireframe`, `//surface`, `//history`, et l'option
`-r` de `//paste` (rotation aléatoire par quart de tour).

**Construction** — `//text`, `//symmetry`, `//flatten`, `//terrain`, `//path`, `//cave`,
`//staircase`, `//spiralstairs`, `//randomize`.

**Images** — `//image` (pixel art), `//topview` (carte vue de dessus en PNG),
`//heightmap import|export` (cartes de hauteur en niveaux de gris).

**Schematics** — `/schem info`, `/schem rename`, `/schem copy`, filtres de `/schem list`
(`-f <format>` et texte), nouveaux formats : structure vanilla `.nbt` (lecture + écriture),
Wavefront `.obj` + `.mtl` et `.json` (export uniquement).

**Brosses** — `blob`, `line`, `overlay`, `fill`, `drain`, `spline`, `copypaste`, `shatter`,
`surfacesplatter`, `layer`, `command`, `populateschem`.

**Outils** — `/tool measure`, `/tool inspect`, `/tool copypaste`.

**Patterns** — `#gradient`, `#stripes`, `#checker`, `#noise`, `#linear`, `#offset`, `#mask`,
`#existing`.

**Masques** — `#x`, `#y`, `#z`, `#angle`, `#adjacent`, `#offset`, `#radius`, `#wall`,
`#floor`/`#top`, `#ceiling`/`#roof`, `#liquid`, `#opaque`, `#transparent`. Les intersections de
masques (séparées par des espaces) peuvent désormais contenir des espaces à l'intérieur des crochets,
et `%<pourcentage>` accepte les décimales.

**Interface** — menu coffre `/wegui` / `//menu` (Bukkit/Paper/Folia uniquement).

**Outillage** — option Gradle `bukkitAdapters` pour ne compiler que certains adaptateurs,
CLI durcie (`--help`, `--non-interactive`, codes de sortie), workflow de synchronisation upstream,
CI sur les branches `claude/**` et `volgoria/**`, Dependabot pour les GitHub Actions.

**Corrections internes** (sans changement de syntaxe) — accès concurrents à l'historique et aux
sessions, enregistrement des déplacements d'entités pour `//undo`, limites des régions cylindre et
ellipsoïde, pagination de `/listchunks` calculée à la demande, etc.

## Compiler

Il faut un JDK ≥ 21 pour lancer Gradle (Gradle télécharge lui-même le JDK requis si besoin).

```sh
# Tout compiler (tous les modules, tous les adaptateurs Bukkit)
./gradlew build

# Uniquement le plugin Bukkit/Paper
./gradlew :worldedit-bukkit:build
```

Le jar à installer est la variante **`-dist`** dans `worldedit-bukkit/build/libs/`.

### Ne compiler qu'une version de Minecraft (`bukkitAdapters`)

Par défaut, `worldedit-bukkit` compile un adaptateur par version de Minecraft supportée, ce qui
télécharge un « dev bundle » Paper pour chacune. Pour un serveur qui ne tourne que sur une version :

```sh
./gradlew :worldedit-bukkit:build -PbukkitAdapters=26.3
```

ou de façon permanente dans `gradle.properties` (celui du projet ou `~/.gradle/gradle.properties`) :

```properties
bukkitAdapters=26.3,26.2
```

- Valeurs possibles : les noms des dossiers `worldedit-bukkit/adapters/adapter-*` sans le préfixe
  (`1.21.4`, `1.21.5`, `1.21.6`, `1.21.9`, `1.21.11`, `26.1`, `26.2`, `26.3`).
- Propriété absente, vide ou `all` : tous les adaptateurs (comportement upstream).
- Une version inconnue fait échouer la compilation avec la liste des versions connues.
- **Le jar obtenu ne fonctionne pleinement qu'avec les versions choisies** ; sur une autre version,
  WorldEdit démarre sans adaptateur (fonctionnalités réduites). Ne pas distribuer ces jars comme
  des builds génériques.

`--parallel` et `--build-cache` accélèrent les compilations locales répétées.

## Installer sur Paper

1. Arrêter le serveur.
2. Retirer tout autre jar WorldEdit (ou FastAsyncWorldEdit) de `plugins/`.
3. Copier `worldedit-bukkit-<version>-dist.jar` dans `plugins/`.
4. Démarrer le serveur. Les dossiers sont créés dans `plugins/WorldEdit/` :
   - `config.yml` — configuration WorldEdit habituelle (inchangée par le fork) ;
   - `schematics/` — schematics (dossier `saveDir` de la configuration) ;
   - `images/` — images pour `//image` et `//heightmap` (créé à la première écriture ; on peut le
     créer à la main pour y déposer des images).
5. Donner les permissions (voir [permissions.md](permissions.md)). `worldedit.gui` est accordée
   aux opérateurs par défaut ; aucune autre nouvelle permission n'a de valeur par défaut.

Le plugin déclare `folia-supported: true` : le menu `/wegui` utilise le planificateur par entité de
Folia (voir [gui.md](gui.md#folia)).

Les mises à jour se font en remplaçant le jar, serveur arrêté. Les sessions, presse-papiers et
schematics existants restent compatibles.

## Synchronisation avec upstream

Le workflow [`.github/workflows/upstream-sync.yml`](../../.github/workflows/upstream-sync.yml) garde
la branche `version/7.4.x` du fork alignée sur celle d'EngineHub :

- Déclenchement : chaque lundi à 06:17 UTC, ou manuellement (*Actions → Upstream Sync → Run workflow*).
- Il récupère `EngineHub/WorldEdit` `version/7.4.x`. S'il y a de nouveaux commits, il force-pousse
  exactement la tête upstream sur la branche `upstream-sync/version-7.4.x`, puis ouvre (ou met à
  jour) une pull request `upstream-sync/version-7.4.x` → `version/7.4.x`.
- Les conflits éventuels se résolvent dans la PR (sur `upstream-sync/version-7.4.x` ou une branche
  dérivée). Le prochain passage force-pousse à nouveau la branche de synchro sur la dernière tête
  upstream.
- **Fusionner la PR avec un commit de merge**, pas en *squash*, pour conserver l'historique upstream.
- Le workflow ne s'exécute jamais dans `EngineHub/WorldEdit` lui-même.

### Réglage obligatoire

Dans *Settings → Actions → General → Workflow permissions*, cocher
**« Allow GitHub Actions to create and approve pull requests »**. Sans cela, `gh pr create` échoue
avec le `GITHUB_TOKEN`.

### Secret optionnel `UPSTREAM_SYNC_TOKEN`

Avec le seul `GITHUB_TOKEN` :

- impossible de pousser des commits qui modifient `.github/workflows` (la synchro échoue si upstream
  a touché à ses workflows) ;
- la PR ouverte ne déclenche pas les autres workflows (la CI ne tourne pas automatiquement dessus).

Pour lever ces deux limites, créer un secret de dépôt `UPSTREAM_SYNC_TOKEN` contenant un *fine-grained
personal access token* avec les droits en écriture **Contents**, **Pull requests** et **Workflows**
sur ce dépôt. Il est utilisé à la place du `GITHUB_TOKEN` lorsqu'il est présent.

### CI

`.github/workflows/gradle.yml` compile aussi les branches `claude/**` et `volgoria/**`, en plus des
branches upstream. Dependabot (`.github/dependabot.yml`) propose chaque semaine, groupées, les mises
à jour des GitHub Actions utilisées par les workflows.

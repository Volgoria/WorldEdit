# WorldEdit CLI

Run WorldEdit commands against a schematic file from the terminal, without a Minecraft server.

## Building

```sh
./gradlew :worldedit-cli:shadowJar
```

The runnable jar is written to `worldedit-cli/build/libs/worldedit-cli-<version>-dist.jar`.

## Usage

```
usage: java -jar worldedit-cli.jar [options]

Run WorldEdit commands against a schematic file.

 -f,--file <path>       The file to load in. Either a schematic, or a
                        level.dat in a world folder. If omitted, a file
                        chooser dialog is shown.
 -h,--help              Show this help message and exit.
 -n,--non-interactive   Exit after running the script instead of reading
                        further commands from standard input. Requires
                        --script.
 -s,--script <path>     A file containing a list of commands to run, one
                        per line. Blank lines and lines starting with '#'
                        are ignored.

Commands are read one per line; type 'stop' to save and exit.
```

Supported files are Sponge (`.schem`) and MCEdit (`.schematic`) schematics and vanilla structure
files (`.nbt`); `level.dat` worlds are not supported yet. The format is detected from the file's
contents.

Changes are written back in the file's own format. MCEdit schematics and Sponge v1 schematics can be
read but not written: the CLI says so when the file is loaded, every attempt to save changes to such a
file prints an error and leaves the file untouched, and the CLI exits with code 1. Commands that don't
modify the schematic still work. To edit such a file, convert it to a Sponge v2/v3 (`.schem`) or
structure (`.nbt`) file first.

Commands are typed as in game, for example `//set minecraft:stone`. The leading `/` of a single-slash
command may be omitted, so `cli selectworld` and `/cli selectworld` are the same. Type `stop` (or
close standard input) to exit. Changes are saved back to the file after every successful command and
on exit; saves go through a temporary file, so a failed save never leaves a truncated schematic.

There is no player, so selections are made with commands. The CLI adds:

| Command            | Description                                  |
|--------------------|----------------------------------------------|
| `/cli selectworld` | Select the entire schematic.                 |
| `/cli await`       | Wait for all pending background tasks.       |

### Interactive

```sh
java -jar worldedit-cli.jar --file house.schem
> /cli selectworld
> //replace minecraft:oak_planks minecraft:spruce_planks
> stop
```

### Scripted

`replace-planks.txt`:

```
# Swap the planks for spruce
cli selectworld
//replace minecraft:oak_planks minecraft:spruce_planks
```

```sh
java -jar worldedit-cli.jar --file house.schem --script replace-planks.txt --non-interactive
```

Without `--non-interactive`, the CLI keeps reading commands from standard input after the script
finishes (unless the script ends with `stop`).

### Exit codes

| Code | Meaning                                                                                  |
|------|------------------------------------------------------------------------------------------|
| 0    | Success.                                                                                 |
| 1    | Error: missing or unsupported file, failed start-up, changes that could not be saved (e.g. to a load-only format), or (in non-interactive mode) a script command that wasn't recognised. |
| 2    | Invalid command line arguments; usage is printed.                                        |

## Files

WorldEdit's configuration and data are stored in a `worldedit` directory in the current working
directory. On first use of each Minecraft data version, block/item data for that version is
downloaded from EngineHub into `worldedit/cli-data`, so the first run needs internet access.

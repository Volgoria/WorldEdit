Compiling
=========

You can compile WorldEdit as long as you have some version of Java greater than or equal to 21 installed. Gradle will download JDK 21 specifically if needed,
but it needs some version of Java to bootstrap from.

The build process uses Gradle, which you do *not* need to download. WorldEdit is a multi-module project with four modules:

* `worldedit-core` contains the WorldEdit API
* `worldedit-bukkit` is the Bukkit plugin
* `worldedit-sponge` is the Sponge plugin
* `worldedit-neoforge` is the NeoForge mod
* `worldedit-fabric` is the Fabric mod

## To compile...

### On Windows

1. Shift + right click the folder with WorldEdit's files and click "Open command prompt".
2. `gradlew build`

### On Linux, BSD, or Mac OS X

1. In your terminal, navigate to the folder with WorldEdit's files (`cd /folder/of/worldedit/files`)
2. `./gradlew build`

## Then you will find...

You will find:

* The core WorldEdit API in **worldedit-core/build/libs**
* WorldEdit for Bukkit in **worldedit-bukkit/build/libs**
* WorldEdit for Sponge in **worldedit-sponge/build/libs**
* WorldEdit for NeoForge in **worldedit-neoforge/build/libs**
* WorldEdit for Fabric in **worldedit-fabric/build/libs**

If you want to use WorldEdit, use the `-dist` version.

(The -dist version includes WorldEdit + necessary libraries.)

## Building only some Bukkit adapters

By default `worldedit-bukkit` builds and bundles an adapter for every supported Minecraft version, which means
downloading and setting up a Paper dev bundle for each of them. If you only run one server version, you can restrict
the adapters with the `bukkitAdapters` Gradle property (comma-separated list of versions):

    ./gradlew :worldedit-bukkit:build -PbukkitAdapters=26.3

or permanently, in `gradle.properties` (project or `~/.gradle/gradle.properties`):

    bukkitAdapters=26.3,26.2

Valid versions are the directory names under `worldedit-bukkit/adapters` without the `adapter-` prefix
(e.g. `1.21.11`, `26.3`). Leaving the property unset (or setting it to `all`) builds every adapter.
An unknown version fails the build with the list of known versions. Note that the resulting jar only works
with the Minecraft versions you selected; on any other version WorldEdit will run without an adapter
(with reduced functionality). Do not distribute jars built this way as general-purpose builds.

Gradle's `--parallel` and `--build-cache` flags can also speed up repeated local builds.

## Other commands

* `gradlew idea` will generate an [IntelliJ IDEA](http://www.jetbrains.com/idea/) module for each folder.
* `gradlew eclipse` will generate an [Eclipse](https://www.eclipse.org/downloads/) project for each folder.

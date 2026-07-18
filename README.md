# XaeroMultiWorld

A lightweight server plugin that adds server-side multiworld support for [Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) and [Xaero's Minimap](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap/). It sends a unique world ID to each player, so their clients keep map data separated per server and per world, even when multiple worlds or servers run behind a single IP (for example on a Velocity or BungeeCord network).

## Compatibility

| Component | Supported |
| --- | --- |
| Server software | Bukkit, Spigot, Paper, Folia |
| Minecraft | 1.8.8-26.2 |
| Java | Java 8 - Java 25 |

Players need Xaero's World Map and/or Xaero's Minimap installed on their client. The plugin itself needs no configuration.

## Installation

1. Download the latest `XaeroMultiWorld-x.x.x.jar` from the releases page.
2. Drop the jar into your server's `plugins` folder.
3. Restart the server.

## Building from source

The Gradle wrapper is included; JDK 17 or newer is enough to build:

```bash
./gradlew build
```

The jar is created at `build/libs/XaeroMultiWorld-<version>.jar`.

## How it works

Xaero's map mods store map data per world and rely on the server to tell them which world the player is in:

1. When a player joins or switches worlds, the plugin sends a world ID over the `xaeroworldmap:main` and `xaerominimap:main` plugin channels.
2. The ID is generated deterministically from the world folder path and server port, so it stays the same across restarts without any stored state.
3. If that fails, the plugin falls back to a persistent `xaeromap.txt` file next to your world folders.

## Metrics

This plugin uses [bStats](https://bstats.org/) to collect anonymous usage statistics. You can disable this globally in `plugins/bStats/config.yml`.

## License

Licensed under LGPL-3.0. See the [LICENSE](LICENSE) file for details.

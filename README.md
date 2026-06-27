# CopyBlockId

CopyBlockId is a lightweight client-side utility mod for Minecraft 1.20.1, 1.21.1 on Fabric, Forge, and NeoForge. Press `Ctrl+C` to copy registry IDs directly from Minecraft without digging through debug screens, configs, or command output.

## What It Copies

- The block ID you are currently looking at, if enabled in the client config
- The entity ID you are currently looking at, if enabled in the client config
- The item ID of the hovered inventory slot
- Hovered item and fluid IDs from JEI and REI overlays/tooltips

Example clipboard output:

```text
minecraft:dirt
```

When a value is copied, the mod shows a short in-game notification.

## Why It Is Useful

- Modpack development
- KubeJS and datapack scripting
- Commands and config editing
- Quick registry lookups while testing

## Controls

- `Ctrl+C` while looking at an entity: copies the targeted entity ID when `copyTargetedEntityInWorld` is enabled
- `Ctrl+C` in the world: copies the targeted block ID when `copyTargetedBlockInWorld` is enabled
- `Ctrl+C` over an inventory slot: copies the hovered item ID
- `Ctrl+C` over JEI or REI entries: copies the hovered ingredient ID

The shortcut is ignored while typing in chat or focused text fields.

## Config

CopyBlockId creates a client config:

- Fabric: `config/copyblockid-client.properties`
- Forge/NeoForge: `config/copyblockid-client.toml`

- `copyTargetedBlockInWorld = false` by default. Set it to `true` to allow `Ctrl+C` in the world to copy the ID of the block you are looking at
- `copyTargetedEntityInWorld = false` by default. Set it to `true` to allow `Ctrl+C` in the world to copy the ID of the entity you are looking at

## Requirements

- Minecraft `1.20.1, 1.21.1`
- Fabric Loader `0.16.10` + Fabric API `0.92.9+1.20.1`
- Forge `47.4.20`
- NeoForge `47.1.106`
- Java `17`

## Build

```powershell
.\gradlew.bat clean build
```

The loader jars appear in each platform module's `build/libs/` directory.

To build one platform at a time:

```powershell
.\gradlew.bat -Prequested_platforms=fabric -Penabled_platforms=fabric :fabric:build
.\gradlew.bat -Prequested_platforms=forge -Penabled_platforms=forge :forge:build
.\gradlew.bat -Prequested_platforms=neoforge -Penabled_platforms=neoforge :neoforge:build
```

## Run Dev Client

```powershell
.\gradlew.bat :fabric:runClient
.\gradlew.bat :forge:runClient
.\gradlew.bat :neoforge:runClient
```

## License

MIT

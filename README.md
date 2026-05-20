# CopyBlockId

CopyBlockId is a lightweight client-side utility mod for NeoForge 1.21.1. Press `Ctrl+C` to copy registry IDs directly from Minecraft without digging through debug screens, configs, or command output.

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

CopyBlockId creates a client config at `config/copyblockid-client.toml`.

- `copyTargetedBlockInWorld = false` by default. Set it to `true` to allow `Ctrl+C` in the world to copy the ID of the block you are looking at
- `copyTargetedEntityInWorld = false` by default. Set it to `true` to allow `Ctrl+C` in the world to copy the ID of the entity you are looking at

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.230`
- Java `21`

## Build

```powershell
.\gradlew.bat clean build
```

The built jar will appear in `build/libs/`.

## Run Dev Client

```powershell
.\gradlew.bat runClient
```

## License

MIT

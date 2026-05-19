# CopyBlockId

A lightweight client-side utility mod for NeoForge 1.21.1 that allows you to instantly copy block IDs directly from the game world using `Ctrl+C`.

When you look at a block and press `Ctrl+C`, the mod copies the block registry ID to your clipboard, for example:

```text
minecraft:dirt
```

The mod will also notify you in chat that the block ID has been copied.

---

## Features

- Instantly copy block IDs from the world
- Does not interfere with chat or GUI shortcuts
- Lightweight and client-side only
- Useful for:
  - Modpack development
  - KubeJS scripting
  - Commands and datapacks
  - Config editing
  - Debugging

---

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.230
- Java 21

---

## Installation

1. Build the mod:

   ```powershell
   .\gradlew.bat clean build
   ```

2. Move the generated `.jar` file from `build/libs/` into your Minecraft `mods` folder.
3. Launch Minecraft with NeoForge 1.21.1.

---

## Usage

1. Join a world
2. Look at a block in inventory
3. Press `Ctrl+C`
4. Paste the clipboard contents anywhere

Example output:

```text
minecraft:dirt
```

`Ctrl+C` inside chats and menus is not intercepted by the mod.

---

## Building

```powershell
.\gradlew.bat clean build
```

To run the development client:

```powershell
.\gradlew.bat runClient
```

---

## License

MIT
# DnsVisuals

A black/orange **HUD & visuals** client mod for Minecraft (Fabric). Honest cosmetic / HUD / quality-of-life features only — no ESP, X-Ray, aimbot, fly, reach or any unfair-advantage cheats.

Press **Right Shift** in-game to open the ClickGUI.
- **Left-click** a module = toggle it on/off (animated)
- **Right-click** a module = open/close its settings

## Tabs & modules (5 × 5)

| Tab | Modules |
|-----|---------|
| **HUD** | Watermark, FPS, CPS, TPS, ArmorHUD |
| **Render** | Crosshair, ViewBobbing, Zoom, BlockOutline, SkyColor |
| **Interface** | ClickGUI, Theme, Animations, Notifications, HudEditor |
| **Chat** | ChatTimestamps, BetterChat, ChatPrefix, AutoText, ClearChat |
| **Misc** | FpsLimit, WindowTitle, SoundControl, AutoSave, CustomSplash |

Each module has 5+ settings using every setting type: **toggle**, **slider**, **mode** (cycle), **color** (R/G/B sliders) and **keybind**.

## Setting controls in the GUI
- **Toggle** – click the pill
- **Slider** – click/drag the bar
- **Mode** – left-click = next option, right-click = previous
- **Color** – drag the R / G / B mini-sliders
- **Keybind** – click, then press a key (Esc = unbind)

Config is saved to `config/dnsvisuals.txt` on quit and reloaded on launch.

## What is fully implemented vs. scaffolded
- **Fully functional now:** the GUI (tabs, animations, all 5 setting types, save/load), the HUD tab renderers (Watermark, FPS, CPS, TPS, ArmorHUD), CPS/TPS tracking, and the Right-Shift open key.
- **Scaffolded (toggle + settings persist; behaviour hook marked in code):** most Render/Chat/Misc modules. They are wired into the menu and save their state; the actual game-side effect (e.g. applying the crosshair, window title, fps cap) is where you (or I, on request) plug in a mixin / event. Each is a small, well-isolated addition.

> TPS note: only the server truly knows its TPS. This client estimates it from tick timing and caps at 20 — fine for a readout, not authoritative.

## Build instructions

### Requirements
- **JDK 17** (e.g. Temurin 17)
- Internet access (Gradle downloads Fabric Loom + the Minecraft/Fabric deps on first build)

### Steps
1. Adjust versions in `gradle.properties` to match **your** Minecraft version. The defaults target **1.20.4**. For another version, set `minecraft_version`, `yarn_mappings`, `loader_version` and `fabric_version` from https://fabricmc.net/develop/ .
2. From the project root run:
   - Windows: `gradlew.bat build`
   - Linux/macOS: `./gradlew build`
   (If you don't have the Gradle wrapper files, run `gradle wrapper` first with a local Gradle, or open the folder in IntelliJ IDEA which will generate them.)
3. The compiled mod jar appears in `build/libs/dnsvisuals-1.0.0.jar`.
4. Install **Fabric Loader** and **Fabric API** for your version, then drop the jar into your `.minecraft/mods` folder.

### Run in dev
- `./gradlew runClient` launches a dev Minecraft with the mod loaded.

## Project layout
```
DnsVisuals/
├─ build.gradle, settings.gradle, gradle.properties
└─ src/main/
   ├─ resources/fabric.mod.json
   └─ java/dns/visuals/
      ├─ DnsVisuals.java          (entrypoint)
      ├─ module/                  (Module, Category, ModuleManager)
      ├─ setting/                 (Boolean/Slider/Mode/Color/Keybind)
      ├─ gui/ClickGuiScreen.java  (the menu)
      ├─ hud/HudManager.java      (HUD rendering)
      ├─ config/ConfigManager.java
      └─ util/                    (animations, colors, CPS/TPS)
```

MIT licensed. Built for your own legitimate, cosmetic use.

# Web Screens
This Project Fully Ai Generated

A Fabric mod for Minecraft **1.21.6-1.21.8** that shows web pages on walls, rendered through the **Android WebView** hosted by the launcher. It is built to be used on a phone , not on a PC.

> **Status: experimental.** It was built and tested step by step on a phone. Some parts (see [Limitations](#limitations)) are not finished yet.

## Features

- **Screen Placer** item: click two corners on a wall to create a screen (up to 16x16 blocks).
- Web pages appear right in the world, including video (YouTube and similar sites).
- Full control: click, scroll and type from a full-screen view.
- Per-screen settings: URL, aspect ratio, always active, volume, **resolution, FPS and DPI** (DPI is free: 24 to 768).
- **Multiplayer**: works on **any server**.
  - Servers that have the mod installed: screens are shared with every player.
  - Servers without the mod: screens are stored on your client only (client-only mode).
- Built-in optimizations: distance-based resolution, slower capture for screens out of sight, double buffering (no torn frames), vsync-paced capture.
- Settings menu through a **keybind**, **Mod Menu**, or commands.

## Requirements

| Component | Notes |
|---|---|
| Minecraft | 1.21.6-1.21.8 |
| Loader | Fabric Loader 0.16.0 or newer |
| Java | 21 |
| Fabric API | 0.136.1+ |
| Launcher | MJ Extended with **WebView bridge** (`webbridge.dex`) installed. Other Launcher Soon |
| Mod Menu | Optional (adds a Configure button) |

Without the WebView bridge the mod still loads, but screens only show "Waiting for the Android WebView bridge...".

## Installation

1. Install Fabric Loader and **Fabric API**.
2. Put `webscreens-<version>.jar` (the one **without** the `-sources` suffix) in the `mods` folder.
3. Make sure the launcher uses the latest `webbridge.dex` (built from the bridge repository).
4. Start the game.

A dedicated server only needs the mod jar and Fabric API. The bridge is not needed on the server.

## Usage

### Creating a screen

**Singleplayer, or a server that has the mod:**
1. Get the **Screen Placer** from the creative tab *Tools & Utilities* (or `/give @s webscreens:screen_placer`).
2. Right-click the first corner on the side of a wall, then right-click the opposite corner on the same face.

**On a server without the mod:**
1. Type `/webscreens place` to turn on placement mode.
2. Right-click two corners as above.

Screens can only be placed on the sides of walls (north, south, east, west), not on floors or ceilings. Limits: 16x16 blocks per screen, 8 screens per player (operators are not limited).

### Using a screen

- **Right-click** a screen to open the full-screen view: mouse, scroll and keyboard are forwarded to the page. Press **Esc** to leave.
- The **Video full** button in the top-right corner makes the video on the page fill the browser area (press it again to undo). Useful when a site's own fullscreen button does nothing.

### Settings

Several ways to open them:

| How | What it opens |
|---|---|
| Click a screen with the **Screen Placer** | That screen's settings |
| `/webscreens settings` | Settings of the screen you are looking at (or the nearest one) |
| **K** key (rebindable) | Main menu: list of all screens and the defaults |
| Mod Menu, then **Configure** | The same main menu |
| `/webscreens menu` | The same main menu |

Per-screen settings:

- **Address (URL)**
- **Aspect ratio**: stretched or keep
- **Always active**: the screen stays alive even when far away
- **Volume**: 0 to 100 percent
- **Resolution**: pixels per block (64 to 768)
- **FPS**: 5 to 60
- **DPI**: any whole number from 24 to 768. 96 is normal; 150 makes page content about 1.5 times bigger. It works as a page zoom.

The **Defaults** menu sets the starting values for new screens (on servers without the mod) and the **screen distance** (screens farther away than this are switched off).

### Commands

| Command | What it does |
|---|---|
| `/webscreens place` | Placement mode (servers without the mod) |
| `/webscreens settings` | Settings of the screen you look at |
| `/webscreens menu` | Main menu |
| `/webscreens fullscreen` | The video on the screen you look at fills the browser area |
| `/webscreens stats` | Capture and upload rate of a screen (for diagnosis) |

### Keybinds

Set them in *Options > Controls > Web Screens*:

- **Open Web Screens menu**: `K` by default
- **Settings of the screen you look at**: unbound by default

## File locations

| File | Contents |
|---|---|
| `config/webscreens-client.json` | Defaults and screen distance (per player) |
| `config/webscreens/<server-address>.json` | Screens on servers without the mod (client-only) |
| `<world folder>/webscreens.json` | Screens in singleplayer or on servers that have the mod |

## How it works

- Every player renders the screens with the WebView on their own device. The server only shares the **screen data** (position, URL, settings), never the page content.
- The mod talks to the bridge over a local socket (`127.0.0.1`), using the port and token from `java.io.tmpdir/mojo_webbridge.txt`.
- Frames are passed through a memory-mapped file that alternates between two slots, then uploaded to an OpenGL texture.

## Building

The project uses Gradle and Fabric Loom and is built with GitHub Actions:

1. Upload the repository contents to GitHub (including `.github/workflows`).
2. Open the **Actions** tab and wait for the build to finish.
3. Download the artifact. Use `webscreens-<version>.jar` (not the `-sources` one).

Local build: `./gradlew build` (JDK 21 or newer). The result is in `build/libs`.

## Troubleshooting

| Problem | Try |
|---|---|
| The screen flickers or stutters | Lower the resolution or FPS in the screen settings. Run `/webscreens stats` while looking at the screen. |
| Cannot create a screen on a server | Server without the mod: use `/webscreens place`. Server with the mod: use the Screen Placer item. |
| A site's fullscreen button does nothing | Use the **Video full** button or `/webscreens fullscreen `. This Feature Still Not Working |

When reporting a problem, include `latest.log` (the lines containing `WebScreens`) and the output of `/webscreens stats`.

## Limitations

- **Ad blocker**: the toggle exists and is saved, but it does not block ads yet.
- **Volume** is set through JavaScript on the page's video and audio elements, so it may not work on every site.
- There is no red outline preview while choosing corners (only an action bar message).
- Screens can only be placed on the sides of walls.
- Pages that depend on internal schemes (`mod://`, `wd://`) and on the `mcefQuery` JavaScript query are not supported.
- Servers without the mod: only you can see the screens, and there is no per-player limit.
- A Fabric client is required; some Forge servers may reject Fabric clients.

## License

CC0-1.0 (public domain).

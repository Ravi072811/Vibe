# Vibe — a clean, ad-free Firestick home screen

An Apple TV–style launcher for Amazon Fire TV: a big "top shelf" title over a soft
blurred backdrop of whatever app you're on, a Top Row of favourites, and all your
apps as rounded 16:9 cards that lift when focused. No ads, no tracking, no
sponsored rows, no internet permission at all.

## What you get

| Folder | What it is |
|---|---|
| `app/` | The launcher's Android source code (no third-party libraries) |
| `.github/workflows/build.yml` | Builds the APK for free on GitHub |
| `scripts/setup.bat` / `setup.sh` | Installs Vibe, makes the Home button open it, optionally removes bloat |
| `scripts/debloat-packages.txt` | The Amazon apps that get switched off — edit to taste |
| `scripts/restore.bat` / `restore.sh` | Undoes everything |

## Step 1 — Build the APK (pick one)

**A. GitHub (no software to install)**
1. Create a free account at github.com and make a new repository.
2. Upload everything in this folder (drag-and-drop works; include the `.github` folder).
3. Open the **Actions** tab → *Build Vibe Launcher APK* → wait about 3 minutes.
4. Open the finished run and download **VibeLauncher-apk**. Unzip it to get `VibeLauncher.apk`.

**B. Android Studio**
Open this folder in Android Studio, let it sync, then *Build → Build APK(s)*.
The file is at `app/build/outputs/apk/debug/app-debug.apk`; rename it to `VibeLauncher.apk`.

## Step 2 — Install it on the Firestick

1. On the Firestick: **Settings → My Fire TV → Developer Options → ADB Debugging: ON**.
2. Find its IP: **Settings → My Fire TV → About → Network** (e.g. `192.168.1.50`).
3. On your computer, download Google's **SDK Platform-Tools** (contains `adb`) and unzip it.
4. Copy the contents of `scripts/` plus `VibeLauncher.apk` into that platform-tools folder.
5. Open a terminal in that folder and run:
   - Windows: `setup.bat 192.168.1.50`
   - Mac/Linux: `./setup.sh 192.168.1.50`
6. Accept the "Allow USB debugging?" prompt on the TV. Answer `y` to remove bloat.

Press **Home** on the remote — Vibe appears.

## Using it

- **OK** opens an app. **☰ (Menu)** or **hold OK** for: Add to / Remove from Top Row,
  Move Left / Right, Hide, App Info, Uninstall.
- **Back** jumps to the first tile. **Home** while on Vibe does the same.
- The **Settings** tile opens Fire TV settings. **Amazon Home** opens Amazon's
  screen if you ever need its search or app library.
- Hidden apps live in the **Hidden Apps** tile at the end.

## Turn off the remaining Amazon tracking (on the TV)

**Settings → Preferences → Privacy Settings**: turn off *Device Usage Data*,
*Collect App Usage Data* and *Interest-based Ads*.
**Settings → Preferences → Featured Content**: turn off video and audio autoplay.

## Good to know

- Fire OS doesn't allow replacing the home app outright, so Vibe uses an
  accessibility service that spots the Amazon home screen and swaps itself in.
  You may see Amazon's screen flash for a split second after pressing Home.
  The service only checks *which* app is in front; it can't read the screen.
- A big Fire OS update can switch the accessibility service off. If the Home button
  starts opening Amazon again, Vibe shows a yellow notice — just run the setup script again.
- Debloating uses `pm disable-user`, which hides apps without deleting anything.
  `restore` brings the Firestick back to stock.
- The Alexa voice button keeps working.

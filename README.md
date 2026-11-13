# Annoying Sounds

A client-side **Fabric** mod for Minecraft Java **1.21.11** and **26.3**. Install the JAR matching your game version. No server installation is needed.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.3 or newer for Minecraft 1.21.11**, or **0.19.5 or newer for Minecraft 26.3**.
2. Install the matching [Fabric API](https://modrinth.com/mod/fabric-api) version.
3. Put one `annoying-sounds-<minecraft-version>-1.4.1.jar` from `dist/` in that instance's `mods` folder. Remove the previous version of Annoying Sounds first.
4. Start Minecraft. Press **J** in-game, or open **Options → Music & Sounds → Annoying Sounds**.

Rebind the shortcut under **Options → Controls → Key Binds → Annoying Sounds**. The default is **J**; Minecraft preserves existing saved bindings, so reset this binding or change it to J when upgrading from a version using N. Minecraft 1.21.11 requires Java 21 or later; 26.3 requires Java 25 or later. Do not install both JARs in one instance.

## Controls

- Rows show readable names and cumulative counts on the left, with aligned controls on the right. Search matches readable names and technical IDs; hover the toggle to see the exact ID. Clear history resets history without changing sound settings.
- Each sound has a matching item icon, a green **Enabled** / red **Disabled** button, a **0–200% volume (default 100%)** slider with **< / >** buttons at its ends for exact 1% steps, and a **▶ replay** button.
- Disabled sounds have locked sliders and arrow buttons. Click **Disabled** to re-enable and edit volume. Setting volume to 0% disables the sound; re-enabling restores its last nonzero volume.
- Icons use matching registered block/item types and mob spawn eggs where available, with representative category icons for sounds without an exact item. Unknown mod sounds also receive a fallback icon. On 26.3, a musical-note placeholder is shown before joining a world because item components are supplied by the world's registry.
- **Recent sounds** shows distinct sounds with the most recently played first. Expired sounds remain editable beneath the **Past / expired sounds** divider. Playing a sound again in the world moves it back to the top. The footer time button cycles **10s → 1m → 5m** directly, with no submenu.
- **All sounds** lists the current resource-pack sound catalog, including modded sounds, and saved rules.
- **Edited sounds** shows muted sounds and sounds with a volume other than 100%. Re-enabling and restoring 100% removes a sound from this tab.
- All three tabs have case-insensitive search beside the mod title and scrolling. Filters, Reset, global mute and master volume controls have been removed. Legacy master settings no longer affect playback; Minecraft's normal volume controls still apply.
- A compact, centered window with Minecraft-native buttons and sliders has three persistent tabs. Each remembers its search and scroll position.
- Lists and counts refresh automatically every half-second. Expired sounds move below the divider. Row changes wait until you finish dragging or stepping a slider; unchanged rows keep their controls and keyboard focus. Scroll using the mouse wheel over the list or drag its scrollbar. Scrolling reuses the filtered catalog and updates only visible rows, with cached display names. New sounds appear at the top; when browsing farther down, the current top visible sound stays anchored where possible.
- **▶ replay** auditions one sound at a time, including muted sounds, without changing saved settings. It uses the current volume (or the last nonzero volume when at 0%), bypasses the per-sound mute, and still obeys Minecraft's master volume. A new replay stops the previous preview; leaving the menu stops playback.

Changes apply immediately to active sound channels, including loops, and are saved to `config/annoying-sounds.json`. Unmuting does not replay a one-shot sound that has already ended.

History is session-only and tracks resolved local world playback attempts within the sound's attenuation distance, including muted attempts so you can find and re-enable them. Menu clicks and this mod's replay previews are excluded. Raw playback events are bounded to one hour / 20,000 events; a separate archive retains up to 10,000 distinct sound IDs until the game closes. Counts are cumulative for retained sound IDs. Clear history resets counts and the archive without changing sound settings. Counts measure playback starts, not the duration of continuous loops. Resource-pack changes are picked up automatically after resource reload.

Subtitles and subtitle settings are intentionally excluded.

## Build

Install JDK 25, set `JAVA_HOME`, then run:

```powershell
.\gradlew.bat build
```

On Linux/macOS use `bash ./gradlew build`. Outputs:

- `mc12111/build/libs/annoying-sounds-1.21.11-1.4.1.jar`
- `mc263/build/libs/annoying-sounds-26.3-1.4.1.jar`

The shared source uses 1.21.11 Mojang names. `common.gradle` adapts the specific GUI/keybinding API renames for 26.3 into a generated source directory. Each JAR declares an exact Minecraft version dependency; the 1.21.11 JAR is remapped and compiled to Java 21, while the 26.3 JAR uses the unobfuscated game API and Java 25.

`build` runs standalone history checks for expiry, window selection, muted-event retention, burst limits and clearing on both targets. To launch an isolated development client:

```powershell
.\gradlew.bat :mc12111:runClient
.\gradlew.bat :mc263:runClient
```

Development clients use their module's `run/` directory and do not modify your normal game installation.

For automated menu integration checks and screenshots, run `./gradlew :mc12111:runClient :mc263:runClient -PsmokeTest`. These temporary clients exit automatically. Results are written to each module's `run/smoke-result.txt`; screenshots go into `run/screenshots/`. The test initializer is excluded from normal builds. Always run `build` **without** `-PsmokeTest` to produce release JARs.

## Manual release checks

- Open the menu through J; rebind it in Controls and check the new key.
- Play a sound, mute it, and check that it appears in Recent and Edited.
- Adjust an active loop's volume and verify immediate changes.
- Search, scroll through the list, and resize the window.
- Cycle through 10s, 1m and 5m; wait for a sound to expire and verify it remains editable below the divider.
- Restart the game and verify settings persist; history should start fresh.
- Reload resource packs and verify All sounds updates automatically.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.




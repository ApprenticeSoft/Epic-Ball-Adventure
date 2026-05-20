# Epic Ball Adventure Modernization - 2026-05-05

## Summary

Epic Ball Adventure was upgraded from its legacy libGDX/Gradle setup to a current multi-target build with Android, desktop, and HTML/WebGL targets.

Version baseline:

- Gradle: 9.5.0
- Android Gradle Plugin: 9.2.1
- libGDX: 1.14.0
- GWT: 2.11.0
- Android compile/target SDK: 36
- Java toolchain: 17

## Main Changes

- Rebuilt the root Gradle configuration and wrapper for Gradle 9.5.0.
- Modernized Android with AGP 9.2.1, namespace/applicationId configuration, SDK 36, Java 17, and current libGDX Android dependencies.
- Replaced the legacy desktop LWJGL launcher with LWJGL3.
- Added a full `:html` GWT/WebGL module and distributable `:html:dist` build.
- Converted legacy CP1252 Java sources to UTF-8 and renamed non-ASCII Java identifiers/classes for GWT compatibility.
- Added web keyboard/mouse/touch controls:
  - menu starts with click/touch, `F`, or `Space` on web;
  - gameplay ball control uses `F`/`Space` or touch/click side input on web.
- Fixed web shader selection so WebGL uses the Android-compatible fragment shader.
- Fixed asset path generation so GWT preloads assets from `../android/assets`.
- Changed `Images.pack` filtering from mipmap filtering to plain linear filtering to avoid WebGL texture parameter warnings.
- Added lifecycle cleanup for loading and game assets where practical.

## HTML FreeType Compatibility Solution

Desktop and Android still use runtime `FreeTypeFontGenerator` from the original TTF files:

- `android/assets/Fonts/calibri.ttf`
- `android/assets/Fonts/HARLOWSI.TTF`

HTML does not use runtime FreeType generation. Instead, `:html:generateWebFonts` pre-generates high-density BMFont atlases at build time using libGDX FreeType itself, not Java2D:

- `Fonts/web_font1_hd.fnt/png` from `calibri.ttf`
- `Fonts/web_title_hd.fnt/png` from `HARLOWSI.TTF`
- `Fonts/web_restart_hd.fnt/png` from `HARLOWSI.TTF`

The web-only supersource `screen.LoadingScreen` maps the original asset aliases (`font1.ttf`, `fontTitre.ttf`, `fontRestart.ttf`) to those generated BMFont atlases. It then scales the generated fonts back to the same runtime sizes used by Android/desktop:

- normal UI font: `width / 26`
- title font: `width / 12`
- restart font: `width / 10`

Texture filtering is `Linear`, integer glyph positions are disabled, and the atlas is generated at 4x reference density. This preserves the exact original TTF designs while avoiding WebGL runtime FreeType incompatibility.

## Font Benchmark

Screenshots generated:

- Desktop FreeType: `build/reports/screenshots/ball-desktop-menu.png`
- HTML FreeType atlas: `build/reports/screenshots/ball-html-menu-1024x720.png`
- HTML gameplay smoke test: `build/reports/screenshots/ball-html-gameplay-1024x720.png`

Measured at 1024x720:

| Element | Desktop bbox | HTML bbox | Result |
| --- | --- | --- | --- |
| Title | x=141, y=340, w=744, h=92 | x=141, y=338, w=744, h=93 | Same width/position, within 2 px vertical visual difference |
| Start text | x=393, y=482, w=238, h=25 | x=319, y=477, w=386, h=27 | Different text string on web, same font family and clean scaling |

The first Java2D atlas attempt clipped Harlow glyph swashes and had a visible vertical metric mismatch. The final solution uses libGDX FreeType at build time, which matched the desktop title width exactly and aligned the visual position within 2 pixels.

## Verification

Commands run successfully:

```bash
./gradlew :core:compileJava :desktop:compileJava
./gradlew :android:assembleDebug
./gradlew :html:dist
./gradlew :android:assembleDebug :desktop:compileJava :html:dist
./gradlew :core:test :android:assembleDebug :desktop:compileJava :html:dist
npm run test:web-transition
```

Runtime checks:

- Desktop launcher opened and produced a framebuffer screenshot via the desktop-only `ball.screenshot` JVM property.
- HTML bundle served locally on `http://127.0.0.1:8093/`.
- Playwright/Chromium loaded the HTML menu without application errors.
- HTML click-through reached gameplay and produced a gameplay screenshot.
- Remaining browser warnings during screenshot capture were Chromium `ReadPixels` performance warnings caused by the screenshot operation itself.

## Transition Stability Fix

Updated on 2026-05-05 after reproducing the reported black screen after level 2.

Root causes found:

- Level 3 has no polygon geometry and relies on rectangular obstacles for visible level art. The obstacle draw ordering still checked old `com.gravity.ball.body.*` class-name strings, so plain rectangular obstacles were left out of the organized draw list on the migrated package names.
- Some TMX properties are typed floats in Tiled. GWT exposes those as numeric values, while legacy constructors cast `Speed`/`Width` directly to `String`. That produced a WebGL-only `ClassCastException` while constructing level 3 objects.

Fixes applied:

- Replaced class-name string checks with `instanceof`/exact class checks in `LecteurCarte`.
- Parsed typed TMX properties through `toString()` and float parsing in rotating obstacles and moving platforms.
- Added `LevelProgression` unit coverage for next-level and transition timing behavior.
- Added `LevelDataTest` coverage for all shipped TMX levels, including the level 3 rectangular-obstacle/no-polygon case.
- Added a web-only debug bridge and Playwright transition test. The test starts at level 1, auto-completes levels 1 through 5, verifies each queued level activation, checks screenshots are not black after every transition, and verifies final completion.

Latest transition validation:

```bash
./gradlew :core:test :html:dist
npm run test:web-transition
```

The same transition check was also run against the deployed `https://ball.marcvidal.ca` bundle and returned `LIVE_TRANSITION_OK`.

Final-completion behavior update:

- After level 5 completes, the closing vignette resolves to a black completion screen.
- `Game Complete !` / `Thanks for playing !` is displayed over the black screen.
- Pressing `Space` on keyboard or touching the screen on mobile returns to the main menu.
- The main-menu start logic is event-driven so the same `Space` press used to return home cannot immediately start a new game.

## Deployment

Completed on 2026-05-05.

Raspberry Pi target:

- Host: `192.168.68.65`
- Web root: `/var/www/ball`
- Staging path used for sync: `/home/marc/ball-web-staging`
- Backend service: `ball-static.service`
- Backend command: `busybox httpd -f -p 127.0.0.1:18084 -h /var/www/ball`
- Apache HTTP vhost: `/etc/apache2/sites-available/ball.marcvidal.ca.conf`
- Apache HTTPS vhost: `/etc/apache2/sites-available/ball.marcvidal.ca-ssl.conf`
- Public URL: `https://ball.marcvidal.ca`

TLS:

- Dedicated Let's Encrypt certificate issued for `ball.marcvidal.ca`.
- Certificate path: `/etc/letsencrypt/live/ball.marcvidal.ca/`
- Verified SAN: `DNS:ball.marcvidal.ca`
- Expiry at deployment time: 2026-08-03

Deployment validation:

```bash
systemctl is-active ball-static.service apache2
curl -fsS http://127.0.0.1:18084/index.html
curl -fsSk --resolve ball.marcvidal.ca:443:127.0.0.1 https://ball.marcvidal.ca/
curl -fsSL --resolve ball.marcvidal.ca:443:198.16.132.146 https://ball.marcvidal.ca
```

All checks returned the Epic Ball Adventure HTML bundle.

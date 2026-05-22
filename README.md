# Epic-Ball-Adventure
A short game created for the One Button Jam in 2016.

## Current Targets

- HTML/WebGL: primary development and test target, deployed at `https://ball.marcvidal.ca`.
- Desktop: LWJGL3 launcher retained for JVM debugging, local verification, and fallback benchmark captures.
- Android: modern libGDX Android backend retained, with release work deferred until the game is more mature.

## Build Commands

```bash
./gradlew :android:assembleDebug
./gradlew :desktop:run
./gradlew :html:dist
npm run test:web-transition
npm run benchmark:web
npm run verify:web-live
```

The HTML build output is written to `html/build/dist`.

## Notes

See `docs/WEB_DEVELOPMENT_ROADMAP.md` for the active web-first roadmap. See `docs/EPIC_BALL_MODERNIZATION_2026-05-05.md` for the upgrade details, HTML FreeType compatibility patch, benchmark results, and deployment notes.

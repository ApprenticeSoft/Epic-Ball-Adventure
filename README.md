# Epic-Ball-Adventure
A short game created for the One Button Jam in 2016.

## Current Targets

- Android: modern libGDX Android backend, debug build verified.
- Desktop: LWJGL3 launcher retained for local verification and font comparison.
- HTML/WebGL: GWT build added for browser deployment.

## Build Commands

```bash
./gradlew :android:assembleDebug
./gradlew :desktop:run
./gradlew :html:dist
```

The HTML build output is written to `html/build/dist`.

## Notes

See `docs/EPIC_BALL_MODERNIZATION_2026-05-05.md` for the upgrade details, HTML FreeType compatibility patch, benchmark results, and deployment notes.

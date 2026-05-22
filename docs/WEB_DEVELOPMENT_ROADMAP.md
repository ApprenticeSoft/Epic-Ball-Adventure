# Epic Ball Adventure Web-First Development Roadmap

Living document. Last updated: 2026-05-21.

This roadmap supersedes the earlier desktop-first wording. For this project, the main development and test target is now the web build accessed from a desktop browser, with `https://ball.marcvidal.ca` on the Raspberry Pi as the primary shared entry point. The standalone desktop launcher remains useful for JVM debugging, packaging, and fallback benchmarks, but it is not the main product loop.

## Current Position

- Primary product target: browser/WebGL build.
- Primary shared test URL: `https://ball.marcvidal.ca`.
- Primary local web build command: `./gradlew :html:dist`.
- Primary local browser regression command: `npm run test:web-transition`.
- Primary local browser evidence commands: `npm run benchmark:web` and `npm run screenshot:web`.
- Primary live Pi smoke command: `npm run verify:web-live`.
- Shared game code remains in `core/src`; canonical runtime assets remain under `android/assets`.
- Current content baseline: five shipped TMX levels.
- Android, Play Store metadata, signing, and release checks remain preserved infrastructure, but Android release work is intentionally deferred until the game itself is stronger.

## Roadmap Principles

- Fun, clarity, and fast browser iteration come before release readiness.
- Every gameplay or editor milestone should leave objective evidence: tests, screenshots, benchmark JSON, live smoke output, or short playtest notes.
- The web editor should make level creation safer than hand-editing TMX files.
- The Pi deployment should stay close enough to active development that it can be used as the normal desktop-browser test version.
- Content should grow in small batches: add or rebuild 2 to 3 levels, test them, then continue.
- Standalone desktop support should remain healthy, but only as secondary tooling until the game loop is mature.

## Milestone 1: Web Production Loop v1

Status: complete as of 2026-05-21.

Goal: make the browser build the reliable daily workflow for development, playtesting, screenshots, benchmarks, and live Pi smoke checks.

### Implemented Work

1. Browser launch and debug path
   - Web query parameters now support production-loop automation:
     - `ballStartLevel=<number>` starts directly at a shipped level.
     - `ballResetProgress=1` resets browser progress before launch.
     - `ballBenchmark=1` starts gameplay automatically from the browser menu.
     - `ballFixedStep=1` runs the shared game loop with a fixed step for more stable browser benchmark evidence.
   - Existing debug parameters remain active:
     - `ballDebug=1`
     - `ballAutoAdvance=1`
     - `ballAutoAdvanceDelay=<seconds>`
     - `ballStartEditor=1`
     - `ballDebugEditorLoadLevel=<number>`

2. Local browser benchmark command
   - Command:

```bash
npm run benchmark:web
```

   - Default output:
     - `build/reports/web-benchmark/benchmark.json`
     - `build/reports/screenshots/web/benchmark.png`
   - The report includes branch, commit, URL, viewport, cache token, level, scene, startup timing, first playable frame timing, frame statistics, memory data when Chromium exposes it, screenshot path, and debug events.
   - Common options:

```bash
npm run benchmark:web -- --level=3 --seconds=12 --viewport=1280x720
npm run benchmark:web -- --url=https://ball.marcvidal.ca --seconds=8
```

3. Local browser screenshot command
   - Command:

```bash
npm run screenshot:web
```

   - Default output:
     - `build/reports/screenshots/web/game.png`
     - `build/reports/web-screenshot/game.json`
   - Common options:

```bash
npm run screenshot:web -- --scene=menu
npm run screenshot:web -- --scene=editor
npm run screenshot:web -- --level=3 --screenshot=build/reports/screenshots/web/level-3.png
```

4. Live Pi smoke check
   - Command:

```bash
npm run verify:web-live
```

   - Checks:
     - root URL returns success,
     - `/privacy.html` returns success,
     - browser reaches main menu,
     - browser reaches gameplay,
     - gameplay screenshot is not blank,
     - browser errors fail the command.
   - Default output:
     - `build/reports/web-smoke/smoke.json`
     - `build/reports/screenshots/web/live-smoke.png`

5. Live Playwright transition option
   - Command:

```bash
npm run test:web-transition:live
```

   - Uses `WEB_BASE_URL=https://ball.marcvidal.ca` so the existing Playwright transition suite can run against the deployed Pi build without starting the local Python web server.

6. Secondary standalone desktop support
   - The standalone desktop launcher now has equivalent debug properties for local JVM-only investigation.
   - Commands:

```bash
./gradlew :desktop:desktopBenchmark
./gradlew :desktop:desktopScreenshot
```

   - These are secondary tools. The browser commands above are the milestone gate.

### Acceptance Criteria

- One documented command builds the local browser artifact: `./gradlew :html:dist`.
- One documented command runs local browser transition coverage: `npm run test:web-transition`.
- One documented command produces browser benchmark JSON under `build/reports/web-benchmark/`.
- One documented command captures browser screenshots under `build/reports/screenshots/web/`.
- One documented command verifies the live Pi URL reaches gameplay: `npm run verify:web-live`.
- Benchmark, screenshot, and smoke outputs are ignored local artifacts under `build/`.

## Milestone 2: Level Editor Production Safety

Status: complete as of 2026-05-21 for v1 production use.

Goal: make the browser editor safe and predictable enough to build real levels without hand-editing TMX as the normal workflow.

### Implemented Work

1. Load and edit shipped TMX levels
   - The editor can import shipped levels from `android/assets/Levels/`.
   - Browser builds load bundled TMX through the web asset manifest and prefer browser-local saved copies when available.
   - Unsupported TMX object types are now preserved in editor data as an explicit unsupported marker instead of silently becoming safe-looking objects.

2. Validate before save and playtest
   - Save and playtest are blocked when validation fails.
   - Validation covers missing Start/Exit objects, invalid numeric values, invalid pulley pairs, insufficient moving-platform path points, unreasonable world bounds, and unsupported imported object types that would be dropped by save.
   - Focused JUnit coverage exists for TMX import, object copy behavior, and validator rejection of unsupported imported types.

3. Backup-before-save
   - Browser saves create a localStorage backup of the previous saved level when one exists.
   - Standalone/local saves create backup files under the local level backup path.
   - Save status tells the user when a backup was made.

4. Undo and redo
   - Undo/redo cover object creation, deletion, movement, resizing, point edits, path edits, and property edits through the existing snapshot flow.
   - Browser shortcut suppression remains active for editor canvas gestures.

5. Typed object controls
   - High-risk raw property fields are constrained:
     - `Loop`: checkbox.
     - `Contact`: checkbox.
     - `Groupe`: integer field.
     - `Speed`, `Weight`, `Torque`, `PowerX`, `PowerY`, angle, position, and dimension-like properties: numeric fields with character filtering and parse rejection.

6. Playtest/edit loop
   - Returning from playtest preserves editor state, camera, zoom, selected object, and dirty status.
   - `Esc` remains the playtest escape route back to the editor.
   - Failed playtest launches show validation reasons and emit debug events for automated browser coverage.
   - The editor visibly marks unsaved changes in the left panel.

### Acceptance Criteria

- A shipped TMX level can be opened, edited, playtested, and saved without losing known gameplay objects.
- Invalid levels are blocked before save/playtest with clear errors.
- A recoverable backup exists before overwriting a level when previous saved content exists.
- Undo/redo works for common editing actions.
- Typed controls replace the highest-risk raw text fields.
- `./gradlew :core:test` passes with focused editor coverage.
- Browser editor coverage remains in `npm run test:web-transition`.

## Milestone 3: Game Feel and Level Quality

Status: next.

Goal: improve the actual browser-played game before increasing platform or release complexity.

### Work Packages

- Review movement feel in the browser: acceleration, jump timing, air control, water movement, collision feedback, and restart speed.
- Make level completion and failure states clear without slowing down repeated attempts.
- Tune level difficulty around readable timing windows rather than hidden precision.
- Keep water effects physically plausible and readable; avoid effects that hide the ball or make motion harder to parse.
- Write short playtest notes for each shipped level:
  - main idea,
  - expected player skill,
  - common failure,
  - current annoyance,
  - proposed improvement.
- Use `npm run benchmark:web`, `npm run screenshot:web`, and `npm run verify:web-live` before marking a gameplay tuning batch stable.

### Acceptance Criteria

- Every shipped level has a short playtest note.
- At least one level is rebuilt or materially improved based on playtest evidence.
- Restarting after failure is fast enough for repeated attempts.
- Visual effects do not reduce gameplay readability.
- Browser benchmark and screenshot evidence exist for the tuning batch.

## Milestone 4: Content Expansion

Status: planned.

Goal: add more levels only after the browser editor and web playtest loop support safe iteration.

### Work Packages

- Add levels in batches of 2 to 3.
- Each new level should introduce or combine one clear mechanic.
- Keep a content checklist for each candidate level:
  - objective is readable in the first few seconds,
  - failure is understandable,
  - restart is quick,
  - the level has one memorable idea,
  - completion feels earned rather than accidental.
- Use browser screenshots and benchmark output before marking a content batch stable.
- Deploy stable content batches to the Pi and run live smoke checks.

### Acceptance Criteria

- Each new level has a clear design note.
- New levels pass validation before being committed.
- New content does not regress browser benchmark baselines by more than an agreed threshold.
- `npm run verify:web-live` passes after deployment.

## Milestone 5: Android Release Reassessment

Status: deferred.

Goal: resume Android only after the browser-played game has enough content quality and stability to justify release work.

### Resume Criteria

- Browser game feel and level quality have improved from the current five-level baseline.
- The editor can produce levels without routine manual TMX repair.
- Browser regression tests, benchmark evidence, and live Pi smoke checks are routine.
- Play Store preflight remains passable except for intentionally deferred physical-device release smoke.

### Android Gate

When Android resumes, run:

```bash
npm run preflight:play-store
npm run preflight:play-store:full
```

The full gate still requires an authorized physical Android device.

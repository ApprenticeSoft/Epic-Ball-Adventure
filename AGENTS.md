# Repository Guidelines

## Project Structure & Module Organization

This is a multi-platform LibGDX Java project. Shared game code lives in `core/src`, with tests in `core/test`. Platform launchers and configuration are split across `desktop/`, `android/`, and `html/`. Runtime assets are canonical under `android/assets`, including `Images/`, `Levels/`, `Sounds/`, `Fonts/`, and `Shaders/`; desktop runs with this directory as its working directory. Browser-specific web files live in `html/webapp`, while generated HTML distribution output is written to `html/build/dist`. Planning and modernization notes are in `docs/`.

## Build, Test, and Development Commands

- `./gradlew :desktop:run` starts the desktop game for local verification.
- `./gradlew :core:test` runs the JUnit 5 test suite for shared logic.
- `./gradlew :html:dist` builds the GWT/WebGL package into `html/build/dist`.
- `npm run test:web-transition` runs Playwright tests in `tests/web`; build `:html:dist` first so the local web server has current files.
- `npm run benchmark:web` writes browser benchmark evidence and a screenshot under `build/reports/`.
- `npm run screenshot:web` captures browser screenshots for menu, game, or editor scenes.
- `npm run verify:web-live` smoke-tests the deployed Pi site at `https://ball.marcvidal.ca`.
- `./gradlew :android:assembleDebug` builds the Android debug APK and copies native libraries as part of `preBuild`.
- `./gradlew clean` removes the root Gradle build directory.

## Coding Style & Naming Conventions

Use Java 17. Match the surrounding Java style: tabs for indentation in source files, same-line braces, compact control flow, and descriptive camelCase method and field names. Java test classes use `*Test.java`, with readable test method names such as `rejectsMissingRequiredObjects`. Gradle files use the existing Groovy DSL style. No repository-wide formatter is configured, so keep changes localized and consistent with nearby code.

## Testing Guidelines

Add JUnit tests under `core/test` for shared logic, especially editor validation, level parsing, persistence, and progression rules. Use Playwright tests under `tests/web` for browser rendering or transition behavior. When changing assets or GWT-compatible code, run both `./gradlew :html:dist` and `npm run test:web-transition`. Keep tests deterministic and avoid relying on generated `build/` or `test-results/` contents.

## Commit & Pull Request Guidelines

Recent commits use short, imperative subjects such as `Fix responsive UI layout` and `Stabilize level editor data model`. Follow that pattern and keep each commit focused. Pull requests should include a concise summary, the commands you ran, linked issues when applicable, and screenshots or short recordings for UI, editor, or rendering changes.

## Security & Configuration Tips

Do not commit local SDK paths, secrets, or generated outputs. Treat `local.properties`, `build/`, `html/build/`, `test-results/`, and `node_modules/` as local artifacts. Prefer editing source, tests, and assets directly rather than patching generated files.

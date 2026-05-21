# Play Store Next Steps

This file is the handoff point after interrupting the release-readiness work. The goal is to leave no unclear automation thread open and make the remaining Google Play release steps explicit.

## Current State

- Current branch at handoff: `master`, tracking `origin/master`.
- Latest pushed release-readiness commit at handoff: `d41ee4d Resolve adb from local Android SDK`.
- The local Play Store readiness flow can build the web game, Android release bundle, Android release APK, metadata, generated store assets, signing evidence, and release evidence.
- Upload signing is configured locally through ignored files.
- The remaining release blocker is external: an authorized physical Android device must be connected before the full device smoke gate can pass.
- The incomplete untracked handoff exporter script was intentionally removed. Do not restart that automation unless it becomes a specific requested task.

## Do Not Lose

Back up these ignored local signing files somewhere durable outside this repository:

```bash
android/keystores/upload.jks
android/signing.properties
```

Do not commit keystores, passwords, local signing properties, generated APKs, generated AABs, or release evidence files.

## Next Commands

Use this quick signed sanity check when no Android device is connected:

```bash
npm run preflight:play-store -- --skip-web-transition
```

Use this full release-candidate gate after connecting and authorizing a physical Android device:

```bash
npm run preflight:play-store:full
```

Verify the live deployed web privacy and game URL before submitting the listing:

```bash
npm run verify:play-store-live
```

Refresh ignored release evidence when needed:

```bash
npm run export:play-store-evidence
```

## Physical Android Device Step

1. Enable Developer options and USB debugging on the device.
2. Connect the device by USB.
3. Accept the Android authorization prompt on the device.
4. Confirm the local SDK adb sees the device as authorized:

```bash
/home/vdlmrc/ApprenticeSoft/android-sdk/platform-tools/adb devices
```

The device row must end with `device`, not `unauthorized` or `offline`.

5. Run:

```bash
npm run preflight:play-store:full
```

The full preflight writes ignored Android smoke evidence under `build/` and should be treated as the final local release-candidate gate before Play Console upload.

## Play Console Handoff

Upload this Android App Bundle:

```bash
android/build/outputs/bundle/release/android-release.aab
```

Use these source documents and generated assets for the Play Console forms:

- `docs/PLAY_CONSOLE_APP_CONTENT.md`
- `docs/PLAY_STORE_LISTING.md`
- `docs/PLAY_STORE_RELEASE_CHECKLIST.md`
- `fastlane/metadata/android/en-US/`
- `docs/play-store-assets/`

Use this privacy policy URL in Play Console:

```text
https://ball.marcvidal.ca/privacy.html
```

## Explicitly Deferred

- Do not add more release automation right now.
- Do not recreate the handoff exporter unless packaging release artifacts becomes a concrete requirement.
- Do not make gameplay, rendering, web deployment, or Android configuration changes unless a specific issue is chosen.
- Rotate or revoke the temporary GitHub token that was shared during this work.

## Clean Handoff Checklist

Before considering this interruption complete:

```bash
git status --short --branch
```

Expected source state:

- `docs/PLAY_STORE_NEXT_STEPS.md` is the only intentional source change.
- No untracked source files remain.
- Ignored build, signing, SDK, and test-output artifacts may still exist locally.
- No active Gradle, npm, Playwright, web server, adb, deployment, or preflight process from this work is still running.

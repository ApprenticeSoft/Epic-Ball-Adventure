# Google Play Release Checklist

Use this checklist before uploading a production build to Google Play.

## Build Gate

- Keep `targetSdk` at or above the current Google Play submission floor. The Android module currently targets SDK 36.
- Build the Android App Bundle:

```bash
./gradlew :android:bundleRelease
```

- For a real upload candidate, configure the upload signing key outside the repo and run:

```bash
EPIC_BALL_UPLOAD_STORE_FILE=/absolute/path/upload.jks \
EPIC_BALL_UPLOAD_STORE_PASSWORD=... \
EPIC_BALL_UPLOAD_KEY_ALIAS=... \
EPIC_BALL_UPLOAD_KEY_PASSWORD=... \
./gradlew :android:verifyPlayStoreRelease
```

The upload key may also be supplied through ignored local Gradle properties with the same names. Do not commit keystores, passwords, or generated bundles.

## Play Console Metadata

- App category: Game.
- Data safety: the Android app declares no network or sensitive-data permissions. Local level progress is stored on-device only, and Android cloud backup is disabled in the manifest.
- Privacy policy: publish a short policy that states the app does not collect, transmit, or share personal data.
- Store assets still needed before launch: feature graphic, phone screenshots, short description, full description, content rating questionnaire, and release notes.

## Final Checks

```bash
./gradlew :core:test :desktop:compileJava :html:dist :android:assembleDebug :android:bundleRelease
npm run test:web-transition
```

Install the generated release build on at least one physical Android device before promoting it from internal testing.

## References

- Google Play target API policy: https://support.google.com/googleplay/android-developer/answer/16561298
- Android App Bundle format: https://developer.android.com/guide/app-bundle/app-bundle-format
- Google Play Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469

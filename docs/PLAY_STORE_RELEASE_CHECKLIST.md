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
- Privacy policy: the source policy lives at `docs/PRIVACY_POLICY.md`, the in-game main menu exposes the same policy text, and the web build publishes `html/webapp/privacy.html` as `/privacy.html`. Use `https://ball.marcvidal.ca/privacy.html` in Play Console after redeploying the web build.
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
- Google Play user data and privacy policy requirements: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play preview asset requirements: https://support.google.com/googleplay/android-developer/answer/1078870

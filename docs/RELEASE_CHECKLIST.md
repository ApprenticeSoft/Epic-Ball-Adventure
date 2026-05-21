# Epic Ball Adventure Release Checklist

Use this checklist before merging or deploying the active release branch.

## Branch State

- Confirm the worktree is clean except for intentional generated artifacts:

```bash
git status -sb
git branch -vv
git ls-remote origin refs/heads/master refs/heads/modernize-libgdx-html-ball
```

- The release branch is `master`.
- The public production URL is `https://ball.marcvidal.ca`.
- Do not store GitHub tokens, SSH passwords, sudo passwords, or deployment secrets in the repository.

## Local Validation

Run the complete validation set:

```bash
./gradlew :core:test :desktop:compileJava :html:dist :android:assembleDebug
npm run verify:play-store-ready
npm run test:web-transition
```

When changing GWT-compatible code, assets, web entrypoints, water rendering, transitions, or editor behavior, run both commands before deployment.

For a Google Play release candidate, also build the uploadable app bundle:

```bash
./gradlew :android:bundleRelease
npm run verify:play-store-ready
```

Run the stricter Play Store gate only when upload signing credentials are available:

```bash
EPIC_BALL_UPLOAD_STORE_FILE=/absolute/path/upload.jks \
EPIC_BALL_UPLOAD_STORE_PASSWORD=... \
EPIC_BALL_UPLOAD_KEY_ALIAS=... \
EPIC_BALL_UPLOAD_KEY_PASSWORD=... \
./gradlew :android:verifyPlayStoreRelease
```

## Deploy To Raspberry Pi

Build the web artifact first:

```bash
./gradlew :html:dist
```

Sync the generated web package to the Pi staging directory and then mirror staging to the live web root:

```bash
rsync -az --delete html/build/dist/ marc@192.168.68.65:/home/marc/ball-web-staging/
ssh marc@192.168.68.65 'rsync -a --delete /home/marc/ball-web-staging/ /var/www/ball/'
```

## Live Verification

Verify the served bundle:

```bash
curl -fsSLI https://ball.marcvidal.ca/ | sed -n '1,12p'
curl -fsSLI https://ball.marcvidal.ca/privacy.html | sed -n '1,12p'
curl -fsSL https://ball.marcvidal.ca/html/html.nocache.js | rg -o "[0-9A-F]{32}" | head
```

Expected result:

- HTTP status is `200 OK` for both `/` and `/privacy.html`.
- `Last-Modified` reflects the deployed build.
- The bundle references at least one GWT cache file hash.

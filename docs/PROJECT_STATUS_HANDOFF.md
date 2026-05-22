# Project Status Handoff

Updated: 2026-05-22 17:45 America/Toronto

## Current State

- Repository: `/home/vdlmrc/ApprenticeSoft/Epic-Ball-Adventure`
- Branch: `master`
- GitHub remote: `https://github.com/ApprenticeSoft/Epic-Ball-Adventure.git`
- Latest pushed commit: `760f5f5dd9d9dea27412b85556fab7dc7f4bdbb2`
- Commit subject: `Stabilize web gameplay and editor`
- Local git status before this handoff file was written: clean and synced with `origin/master`
- Main deployed entry point: `https://ball.marcvidal.ca`
- Live site check at handoff time: HTTP `200 OK`
- Live GWT cache tokens observed: `0A61D0F26F971F9A8B9F8416AA18B78B`, `4EF93B7CBD36E04A3D1518388479355E`

This handoff file was created after commit `760f5f5`, so it may be the only uncommitted local change when work resumes.

## What Was Done

- Project direction changed to prioritize the web version deployed on the Raspberry Pi as the main development/test entry point. Desktop standalone remains a future need, but not the primary current target.
- Added `docs/WEB_DEVELOPMENT_ROADMAP.md` as the living roadmap for the web-first development path.
- Improved web/debug infrastructure for browser startup, fixed-step/benchmark support, live production checks, and Playwright regression coverage.
- Fixed browser restart behavior so normal page refresh/startup begins at level 1 instead of resuming a later saved level.
- Added spring light effect using Box2DLight:
  - Blue additive light based on the spring color.
  - Power-scaled light distance with much stronger visible difference between weak and strong springs.
  - Fade timing: full intensity through `0.01s`, then quadratic fade to zero by `0.5s`.
  - One active spring light per spring to avoid stacking/freezing.
  - GWT compatibility fix for Box2DLight `Spinor`.
  - Light source now attaches to the spring surface in body-local coordinates and rotates with the spring.
- Fixed rotated spring physics:
  - Spring `PowerX` / `PowerY` are now treated as spring-local power and rotated by the spring body angle before applying velocity.
  - Spring extension/reset checks are measured in spring-local space.
- Fixed editor snap behavior:
  - Rotation no longer changes `Snap: Grid` to `Snap: Free`.
  - Added regression guard so only the explicit snap toggle assigns `selectedObject.snapMode`.
- Improved editor/web behavior:
  - Editor return/playtest and dirty-state UI support were added in the broader committed update.
  - Editor object movement, background pan, wheel zoom, and browser shortcut behavior have focused Playwright coverage.
- Pushed all committed work to GitHub `master` at commit `760f5f5`.
- Deployed the latest web bundle to the Raspberry Pi and published it to the live Apache web root.

## Validation Already Run

Focused checks run during the recent work:

- `./gradlew :core:test --tests utils.SpringMotionTest --tests utils.SpringLightGeometryTest :desktop:compileJava`
- `./gradlew :core:test --tests screen.LevelEditorScreenSnapModeTest --tests editor.EditorLevelObjectTest`
- `./gradlew :html:dist`
- `npx playwright test tests/web/transition.spec.mjs -g "debug spring light probe" --project=desktop`
- `WEB_BASE_URL=https://ball.marcvidal.ca npx playwright test tests/web/transition.spec.mjs -g "debug spring light probe" --project=desktop`
- `npx playwright test tests/web/transition.spec.mjs -g "desktop editor keeps left-drag object movement" --project=desktop`
- `WEB_BASE_URL=https://ball.marcvidal.ca npx playwright test tests/web/transition.spec.mjs -g "desktop editor keeps left-drag object movement" --project=desktop`
- `curl -fsSLI https://ball.marcvidal.ca/`
- `curl -fsSL https://ball.marcvidal.ca/html/html.nocache.js | rg -o "[0-9A-F]{32}" | head`

All listed validations passed at the time they were run.

## Important Operational Notes

- Runtime assets remain canonical under `android/assets`.
- Web build output is `html/build/dist`.
- Pi staging path used during deployment: `/home/marc/ball-web-staging/`.
- Pi served path used during deployment: `/var/www/ball/`.
- The normal HTTPS `git push origin master` failed in the shell because no interactive GitHub credential helper was configured. The push succeeded using the temporary GitHub token directly in a one-off push URL without changing the stored remote.
- No deployment password, sudo password, or GitHub token should be committed to the repository.
- Rotate or revoke the temporary GitHub token that was shared for this work.
- `gh` CLI is not installed in this environment, so PR creation through `gh` was not available.

## Suggested Next Steps

1. Manually verify the latest live web editor:
   - Open `https://ball.marcvidal.ca`.
   - Enter the level editor.
   - Select an object with `Snap: Grid`.
   - Rotate it, then drag it.
   - Confirm the snap mode remains `Grid`.
2. Manually verify rotated springs in the live build:
   - Test a 45-degree and 90-degree spring.
   - Confirm the physical spring movement, ball impulse, and light ray all point in the same rotated direction.
3. Commit this handoff file if it should be preserved on GitHub.
4. Continue from `docs/WEB_DEVELOPMENT_ROADMAP.md`, prioritizing game maturity on the web/Pi version before returning to Android or desktop standalone packaging.
5. Add a more direct automated editor snap interaction test if the editor exposes a stable debug bridge for reading selected object state.


# Google Play Store Listing Draft

Use this as the source copy for the first internal testing or production listing.

## Product Details

- App name: Epic Ball Adventure
- Package name: com.apprenticesoft.epicballadventure
- Default language: English (United States)
- App type: Game
- Category: Arcade
- Ads: No
- Target audience: Ages 13-15, 16-17, and 18+
- App access: All gameplay is available without sign-in, purchase, account creation, or special instructions.
- Website: https://ball.marcvidal.ca
- Privacy policy: https://ball.marcvidal.ca/privacy.html

## Short Description

Guide a rolling ball through crisp one-button physics puzzles.

Character count: 62 / 80

## Full Description

Epic Ball Adventure is a compact physics platformer built around one-button control. Roll, jump, bounce, and time each move through handcrafted levels where simple inputs lead to satisfying momentum.

What to expect:

- One-button gameplay that is easy to learn and precise to master.
- Minimal, colorful visuals built for quick reading on phone screens.
- Five handcrafted levels with ramps, springs, water, pulleys, and timing challenges.
- Local progress saving, so players can continue from the latest unlocked level.
- No ads, no accounts, no analytics, and no personal data collection.

Epic Ball Adventure is designed for short, focused play sessions. Each level asks for careful timing rather than complicated controls, making it approachable while still leaving room for cleaner runs.

## Release Notes

Initial Google Play release.

- Five playable physics levels.
- One-button touch controls.
- Local level progress saving.
- In-game privacy policy.

## Data Safety Draft

- Privacy policy: https://ball.marcvidal.ca/privacy.html
- Data collected: No user data collected.
- Data shared: No user data shared.
- Data processed ephemerally: No.
- Security practices: No data is transmitted by the app.
- Account creation: Not supported.
- User data deletion request: Not applicable because the app has no accounts and collects no user data. Local progress can be removed by clearing app data or uninstalling the app.

## Target Audience And Content Draft

- Target age groups: 13-15, 16-17, 18+.
- Designed for children under 13: No.
- App access restrictions: None.
- Ads: No ads.
- In-app purchases: None.
- User interaction: None.
- User-generated content: None.
- Personal information collection from children: None.
- Store presence note: The listing describes a compact physics platformer for short, focused play sessions and does not claim to be made for children.

## Content Rating Notes

- Violence: None.
- Fear or horror themes: None.
- Sexual content: None.
- Language: None.
- Controlled substances: None.
- User-generated content or online interaction: None.
- Purchases, ads, gambling, or loot boxes: None.

## Preview Asset Plan

Generated assets:

- App icon: `docs/play-store-assets/app-icon.png` - 512 x 512 32-bit PNG with alpha.
- Feature graphic: `docs/play-store-assets/feature-graphic.png` - 1024 x 500 24-bit PNG with no alpha.
- Phone screenshots: `docs/play-store-assets/phone-screenshots/*.png` - four 1080 x 1920 24-bit PNGs with no alpha.
- Asset manifest and alt text: `docs/play-store-assets/README.md`.
- Regeneration command: `npm run generate:play-store-assets`.
- Fastlane metadata export: `fastlane/metadata/android/en-US/`.
- Metadata export command: `npm run export:play-store-metadata`.

Remaining before launch:

- Upload the generated app icon, feature graphic, and phone screenshots into Play Console.

Screenshot coverage:

- Level 1 early gameplay with the ball, ramps, and goal visible.
- Level 2 timing/ramp gameplay.
- A water-level shot showing bubbles/foam.
- A later pulley/physics challenge.

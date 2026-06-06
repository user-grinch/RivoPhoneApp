# Release Notes — RivoPhoneApp

## What's New

### Material You Swipe-to-Answer Slider
The incoming call screen has been completely redesigned to match the Google Pixel Phone interface:

- **Pill-shaped floating handle** — A horizontally stretched capsule with highly rounded corners, replacing the old circular handle. The handle smoothly glides left (Decline) or right (Answer) with gesture tracking.
- **Dynamic color morphing** — As you drag right, the handle transitions from cream to Material Green (`#34A853`). As you drag left, it transitions to Material Red (`#EA4335`). The phone icon inside fades to white and the handle tilts into a decline position when swiping left.
- **Edge fade-out** — The handle's opacity smoothly fades to zero as it nears the edge threshold, right before the action triggers.
- **Strict boundary containment** — The handle is dynamically constrained to the track's inner bounds, never clipping outside the rounded capsule container.
- **Dark/Light theme support** — Adapts instantly to system dark/light mode. Dark mode uses a brown/charcoal track (`#2D2321`) with cream labels; light mode uses a Material You surface container color with dark, high-contrast labels.

### Morphing Onboarding Screen
A new morphing-animation onboarding experience shown on the very first app launch:

- **3 animated pages** — Expressive Design, Lightning-Fast Dialing, and Built for You — using morphing shapes, rotation, and scale transitions.
- **Skip / Back / Next / Get Started navigation** with morphing indicator dots.
- Automatically marks itself as seen after completion; never shows again.

### CI & Signing Fixes
- Removed the hardcoded keystore dependency from the release build configuration. The CI generates a debug keystore at build time, and local builds fall back to `~/.android/debug.keystore` — removing the "package appears to be invalid" installation error.

## How to Get These Changes

**If you already have the app installed:**
The next release build from CI will include everything. Uninstall the old version first (signing key may differ), then install the new APK.

**Building locally:**
```bash
git pull
./gradlew assembleDebug
```

The debug APK will be signed with your local Android debug keystore (`~/.android/debug.keystore`) and can be installed directly.

**Building a release APK via CI:**
Push to any branch — the GitHub Action at `.github/workflows/build.yaml` will:
1. Generate a debug keystore
2. Build a signed release APK and AAB
3. Publish them as a "Preview Build" release on GitHub

# Changelog - Pdialer Optimized (Revophone Fork)

## [v1.6.0] - 2026-04-12

### 🎨 UI & UX Enhancements
* **Fixed Contact Image Corruption:** Corrected the `ContentScale` and clipping logic for contact photos. Images now render as perfect circles without stretching
* **Animated Blur Background:** Re-engineered the background motion using `graphicsLayer` translation. This provides a fluid, organic "drifting" effect while keeping CPU usage low on the *
* **Material 3 Expressive Shapes:** Updated the End Call button with a reactive spring animation that shifts corner radius on press.

### 🛠️ New Features
* **Functional Hold Button:** Added a dedicated Hold button that triggers `call.hold()` and `call.unhold()` with real-time status updates in the call header.
* **Smart Note Integration:** Added a Note button that launches the system's text-sharing intent, allowing you to quickly save details about the call to your preferred notes app.

### ⚙️ Build & CI/CD Pipeline
* **Universal APK Support:** Enabled `universalApk` in the build splits to ensure GitHub Actions generates a compatible binary for both ARM64 and x86_64.
* **Automated Versioning:** Updated GitHub Action to trigger full signed releases only on version tags (`v*`), while providing debug builds for standard commits..
* **Keystore Security:** Optimized the Base64 decoding process in the workflow to prevent corrupt keystore generation during automated builds.
---

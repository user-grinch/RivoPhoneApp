<div align="center">

<img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/app/src/main/res/drawable/logo.png" width="108" height="108" alt="Rivo Logo">

# Rivo

An open-source, private Android dialer built with modern Jetpack Compose.

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-2563EB.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-10B981.svg?style=flat-square&logo=android)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-6366F1.svg?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Crowdin](https://img.shields.io/badge/Localization-Crowdin-0EA5E9?logo=crowdin&style=flat-square)](https://crowdin.com/project/rivophone)

Fast, unbloated calling experience designed with Material 3 Expressive guidelines, granular privacy controls, and elevated capabilities like non-root internal call recording.

<br>

<a href="https://play.google.com/store/apps/details?id=com.grinch.rivo4">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/play.png" alt="Get it on Google Play" height="42">
</a>
&nbsp;
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/{%22id%22:%22com.grinch.rivo4%22,%22url%22:%22https://github.com/user-grinch/RivoPhoneApp%22,%22author%22:%22user-grinch%22,%22name%22:%22RivoPhoneApp%22}">
  <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/b1c8ac6f2ab08497189721a788a5763e28ff64cd/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="42">
</a>
&nbsp;
<a href="https://github.com/user-grinch/RivoPhoneApp/releases">
  <img src="https://user-images.githubusercontent.com/69304392/148696068-0cfea65d-b18f-4685-82b5-329a330b1c0d.png" alt="Download APK from GitHub" height="42">
</a>

<br>

[Patreon](https://www.patreon.com/c/grinch_) &bull; [Discord](https://discord.gg/NtEvU3726e) &bull; [Translate](https://crowdin.com/project/rivophone) &bull; [Releases](https://github.com/user-grinch/RivoPhoneApp/releases)

</div>

---

## Overview

Rivo is designed to replace cluttered default dialers with a responsive, native Android phone experience. It features full dual-SIM management, fast T9 indexing, a modern in-call presentation with expressive swipe controls, and built-in Shizuku integration for high-quality two-way audio capture without requiring root or accessibility workarounds.

## Features

### Dialing & Smart Search
- **Instant T9 Search**: Type contact names, initials, or phone numbers directly from the numeric keypad.
- **Speed Dial**: Map frequent contacts to numbers 1 through 9 for one-touch dialing.
- **Dual-SIM Support**: Select outbound SIM on the fly, remember SIM preferences per contact, and see carrier labels inline.
- **DTMF Keypad**: In-call keypad with clean haptic feedback and instant tone feedback.

### In-Call & Notifications
- **Expressive Call UI**: Contemporary in-call interface with dynamic caller artwork, clear call status indicators, and one-hand accessible controls.
- **Audio Routing**: Switch between earpiece, speakerphone, wired headset, and Bluetooth audio accessories.
- **Call Actions**: Hold, mute, add call, swap calls, and in-call notes sheet.
- **Interactive Call Notifications**: System-integrated incoming and ongoing call notifications with quick actions (Answer, Decline, Speaker, Callback reminders).
- **Post-Call & Callback Reminders**: Schedule callback reminders or add unknown callers to contacts directly after hangup.

### Call Recording
- **Elevated Recording via Shizuku**: Capture clear 2-way call audio without root or accessibility hacks by interfacing directly through Shizuku ADB permissions.
- **Standard Audio Recording**: Optional fallback microphone-based recording where Shizuku is not configured.
- **Integrated Recording Manager**: Review, play back, search, share, or delete saved call recordings from a dedicated segmented manager.

### Contact Management
- **Segmented Contact Editor**: Modern Material 3 continuous card design supporting multiple phone numbers, email addresses, postal addresses, and custom labels.
- **Favorites & Groups**: Organize important contacts and access them quickly from dedicated tabs.
- **Account-Aware Storage**: Choose where contacts save—Google, device local memory, or encrypted private storage.
- **Merge & Deduplication**: Tools to identify and consolidate duplicate contacts and cleanup redundant numbers.

### Privacy & Security
- **Private Contacts Vault**: Keep selected contacts, their call logs, and notification previews hidden behind biometric authentication or PIN lock.
- **Fake Incoming Call**: Schedule or trigger simulated incoming calls with customizable caller IDs, ringtones, and timers.
- **Number Blocking**: Block unwanted numbers and robocallers directly from call details or call history.
- **Zero Telemetry**: No third-party ad networks, no analytics trackers, and zero background data collection.

### Interface & Personalization
- **Material 3 Expressive**: Adaptive color theming derived from your device wallpaper (Dynamic Color / Material You).
- **Customization**: Configurable dialpad layouts, avatar shapes, card roundness, and call screen background styles.
- **Biometric App Lock**: Lock the entire dialer application with fingerprint, face unlock, or device credentials.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/1.png" width="280" alt="Recents">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/2.png" width="280" alt="Dialpad">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/3.png" width="280" alt="Contact Details">
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/4.png" width="280" alt="Settings">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/5.png" width="280" alt="Call Screen">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/6.png" width="280" alt="Private Contacts">
</p>

## Installation

### Stable Releases
- **Google Play**: [Install from Play Store](https://play.google.com/store/apps/details?id=com.grinch.rivo4)
- **Obtainium**: Add Rivo to [Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/{%22id%22:%22com.grinch.rivo4%22,%22url%22:%22https://github.com/user-grinch/RivoPhoneApp%22,%22author%22:%22user-grinch%22,%22name%22:%22RivoPhoneApp%22}) for automated GitHub release updates.
- **GitHub Releases**: Download pre-built APK packages directly from [GitHub Releases](https://github.com/user-grinch/RivoPhoneApp/releases).

### Verification
Official release builds are signed with the following certificate SHA-256 fingerprint:

```text
com.grinch.rivo4
AF:7B:C8:10:1A:C9:D7:4B:93:5B:31:4B:71:C7:EE:1D:ED:0F:9D:45:AB:07:4C:72:7F:82:11:89:F4:56:50:C5
```

## Contributing

Contributions, bug reports, and suggestions are welcome.

- **Issue Tracker**: Report defects or request enhancements via [GitHub Issues](https://github.com/user-grinch/RivoPhoneApp/issues).
- **Pull Requests**: Code changes must follow existing Kotlin and Jetpack Compose formatting conventions.
- **Localization**: Help translate Rivo into your language on [Crowdin](https://crowdin.com/project/rivophone).
- **Community**: Join our [Discord community](https://discord.gg/NtEvU3726e) for discussions and support.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

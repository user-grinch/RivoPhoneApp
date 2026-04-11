# 📱 Pdialer-Optimized

**Pdialer-optimized** is a high-performance, expressive dialer application built with **Jetpack Compose**. It is a specialized fork of **RevoPhone**, meticulously optimized for low-resource hardware while delivering a premium, Material 3 "Expressive" design experience.

<p align="center">
  <a href="https://github.com/MoHamed-B-M/Pdialer-optimized.git">
    <img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github" />
  </a>
</p>

---

## ✨ Key Features (Optimized Edition)

- **🚀 Performance Tuning** — Refined for devices with limited RAM (e.g., 4GB/8GB systems) and optimized background processing.
- **🎨 Revo-Expressive UI** — A beautiful fork of the **RevoPhone** design utilizing **Material 3 Expressive** components, featuring liquid morphing buttons and glassmorphism.
- **📞 Advanced Call Screen** — A native in-app calling interface with dynamic blur backgrounds and haptic-feedback swipe gestures.
- **⚡ Lightweight Contact Logic** — Fast indexing and search capabilities using a modular repository pattern.
- **🔒 Privacy First** — Minimal permissions and locally-stored contact data handling.

---

## 🛠 Tech Stack

- **UI Framework:** Jetpack Compose (Material 3)
- **Language:** Kotlin
- **Dependency Injection:** Koin
- **Image Loading:** Coil (Optimized for low-end GPUs)
- **Architecture:** MVVM with StateFlow
- **Build System:** Gradle (KTS) + GitHub Actions for CI/CD

---

## 📸 Design Preview

The UI follows the **RevoPhone** aesthetic with specific "Pdialer" optimizations:

* **Morphing Buttons:** "End Call" buttons that transition from circles to rounded squares on interaction (Liquid Morph).
* **Liquid Swipe:** An expressive "Swipe to Answer" component with real-time color morphing (Green for answer, Red for decline).
* **Optimized Blur:** Dynamic background blurs adjusted to run smoothly on mid-range hardware.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Android SDK (API 24+)
- **pnpm** (Recommended for managing local development tools)

### Installation

```bash
# Clone the optimized fork
git clone [https://github.com/MoHamed-B-M/Pdialer-optimized.git)

# Enter project directory
cd Pdialer-optimized

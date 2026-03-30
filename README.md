# Rivo

**Rivo** is a modern, feature-rich dialer app built with **Jetpack Compose**.  
It focuses on speed, simplicity, and a clean native Android experience for handling calls and contacts.

<p align="center">
  <a href="https://github.com/user-grinch/Rivo">
    <img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github" />
  </a>
  <a href="https://discord.gg/NtEvU3726e">
    <img src="https://img.shields.io/badge/Discord-Join%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white" />
  </a>
  <a href="https://www.patreon.com/c/GrinchDev">
    <img src="https://img.shields.io/badge/Patreon-Support%20Development-F96854?style=for-the-badge&logo=patreon&logoColor=white" />
  </a>
</p>

---

## ✨ Features

- **⚡ Fast Dialing** — Smooth and responsive dialing experience  
- **📇 Contact Management** — Organize and access contacts easily  
- **📞 Call History** — Clean and accessible call logs  
- **🎨 Modern UI (Compose)** — Built fully with Jetpack Compose  
- **📱 Call Screen** — Native in-app calling interface  
- **🧩 Modular Architecture** — Scalable and maintainable codebase  

---

## 🛠 Tech Stack

- **UI:** Jetpack Compose  
- **Language:** Kotlin  
- **Architecture:** MVVM (ViewModel + State)  
- **Async:** Coroutines / Flow  
- **Build System:** Gradle (KTS)  

---

## 📸 Preview

<details>
  <summary>Show Screenshots</summary>

  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/1.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/2.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/3.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/4.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/5.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/6.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/7.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/8.jpeg" width="300">
  <img src="https://raw.githubusercontent.com/user-grinch/RivoPhoneApp/main/images/9.jpeg" width="300">

</details>

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest recommended)
- Android SDK (API 24+)

### Setup

```bash
# Clone the repository
git clone https://github.com/user-grinch/Rivo.git

# Enter project directory
cd Rivo
````

### Open in Android Studio

* Open the project folder
* Let Gradle sync automatically

### Run the app

* Select a device/emulator
* Click **Run ▶**

### Build APK

```bash
./gradlew assembleDebug
# or
./gradlew assembleRelease
```

APK will be located in:

```
app/build/outputs/apk/
```

---

## 📂 Project Structure (Simplified)

```
app/
 ├── ui/              # Compose UI screens & components
 ├── viewmodel/       # ViewModels
 ├── data/            # Repositories & data sources
 ├── domain/          # Business logic (optional clean architecture)
 └── utils/           # Helpers & extensions
```

---

## 🤝 Contributing

Contributions are welcome!

```bash
# 1. Fork the repo

# 2. Create a branch
git checkout -b feature-name

# 3. Commit your changes
git commit -m "Add: short description"

# 4. Push
git push origin feature-name
```

Then open a Pull Request describing your changes.

---

## 💬 Community

* Discord: [https://discord.gg/NtEvU3726e](https://discord.gg/NtEvU3726e)
* Patreon: [https://www.patreon.com/c/GrinchDev](https://www.patreon.com/c/GrinchDev)

Join to report bugs, suggest features, or follow development.

---

## 📄 License

Licensed under **GNU GPL v3.0**.
See the [LICENSE](LICENSE) file for details.
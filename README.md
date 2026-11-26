# Name Maker ✨
> **Pure Kotlin Modern Android App for VIP Stylish Names, Bios & Unicode Symbols**  
> *Crafted with ❤️ by Sahil*

[![Build and Release Name Maker APK](https://github.com/Sahil-Sk-In/Stylish-Name/actions/workflows/build.yml/badge.svg)](https://github.com/Sahil-Sk-In/Stylish-Name/actions/workflows/build.yml)

---

## 🌟 Key Highlights

- **100% Pure Kotlin & Android Studio:** Written cleanly in idiomatic Kotlin with Jetpack Compose & Material 3 AMOLED Dark theme.
- **🚫 Zero Activation Locks / Tamper Guard Bypassed:** No server dependency, no activation keys, no crash on startup or logo tap.
- **⚡ 100% Offline & Instant Loading (0ms):** All 30+ Unicode & TTF fonts, symbols, custom categories, and VIP names load instantaneously from in-memory cached assets.
- **🎨 25+ Real-time Font Keyboards:** Dynamic font stylizer with instant one-tap copy and Android Intent sharing.
- **👑 50+ Intro & Outro Text Frames:** Gaming tags, Free Fire, PUBG, aesthetic borders (`★彡[...]彡★`, `亗『...』亗`, `꧁༒☬...☬༒꧂`).
- **🔣 19+ Unicode & S2 Special Symbol Categories:** Balinese, Buginese, Buhid, Khmer, Combining Marks, Runic, and the exclusive S2 alphabet marks.
- **💎 VIP Bios & Stylish Names Showcase:** 80+ curated stylish bios and names with real-time keyword and hashtag search.

---

## 🚀 How to Build & Run

### Method 1: Automatic Cloud Build (GitHub Actions - Recommended)
1. Push to this repository (`git push -u origin main`).
2. Go to the **Actions** or **Releases** tab on [github.com/Sahil-Sk-In/Stylish-Name](https://github.com/Sahil-Sk-In/Stylish-Name).
3. Download the official `NameMaker-v1.0.0-Release-Signed.apk` directly!

### Method 2: Android Studio (Local)
1. Open Android Studio.
2. Select **Open** and choose this `Name_Maker` folder.
3. Wait for Gradle Sync to complete.
4. Click **Run** (`Shift + F10`) or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

---

## 📁 Project Structure

```
Name_Maker/
├── .github/workflows/build.yml    # Automatic cloud APK builder
├── app/
│   ├── build.gradle.kts          # Modern Gradle KTS build config
│   └── src/main/
│       ├── AndroidManifest.xml   # Application manifest
│       ├── assets/               # 30+ fonts, symbols.json, posts.json
│       ├── java/com/sahil/namemaker/
│       │   ├── MainActivity.kt   # Clean entry activity
│       │   ├── data/             # Models & AppRepository
│       │   ├── engine/           # FontTransformer (35+ styles)
│       │   └── ui/               # Compose Screens & Theme
│       └── res/                  # App icons, colors, themes
├── build.gradle.kts              # Root build script
└── settings.gradle.kts           # Dependency repositories
```

---

*Enjoy creating elite stylish names!*

<p align="center">
  <img src="docs/assets/anonforge_icon.svg" alt="AnonForge Logo" width="120" height="120">
</p>

<h1 align="center">AnonForge</h1>

<p align="center">
  <strong>🛡️ Secure Disposable Identity Generator for Android</strong><br>
  <em>Protect your privacy with realistic temporary profiles — 100% offline.</em>
</p>

<p align="center">
  <a href="https://github.com/AnonForge-EU/anonforge/releases/latest"><img src="https://img.shields.io/github/v/release/AnonForge-EU/anonforge?style=flat-square&logo=android&color=3DDC84" alt="Latest Release"></a>
  <a href="https://github.com/AnonForge-EU/anonforge/releases"><img src="https://img.shields.io/github/downloads/AnonForge-EU/anonforge/total?style=flat-square&color=blue" alt="Downloads"></a>
  <a href="https://github.com/AnonForge-EU/anonforge/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen?style=flat-square&logo=android" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin 2.1">
</p>

<p align="center">
  <a href="#-why-anonforge">Why?</a> •
  <a href="#-features">Features</a> •
  <a href="#-download">Download</a> •
  <a href="#%EF%B8%8F-security--privacy">Security</a> •
  <a href="#-contributing">Contributing</a> •
  <a href="#-support">Support</a>
</p>

---

## 🎯 Why AnonForge?

In 2025 alone, **over 700 million French records** were exposed through data breaches. Every time you share your real email, phone, or address with an untrusted site, you risk spam, tracking, and identity theft.

AnonForge lets you **generate realistic, disposable identities** for low-stakes sign-ups — newsletters, forums, free trials — so your real data stays private.

> *The best way to protect your data is to never give it away.*

---

## ✨ Features

**🎭 Identity Generation** — Realistic names, addresses, phone numbers, and dates of birth with nationality support (FR, EN, DE). Weighted random generation for authentic-looking profiles.

**📧 Email Aliases** — Real forwarding email aliases via [SimpleLogin](https://simplelogin.io) API integration. Your inbox stays clean, your identity stays hidden.

**🔐 Encrypted Vault** — All identities stored in an encrypted database (SQLCipher + Android Keystore, AES-256-GCM). Biometric unlock with PIN fallback.

**⏱️ Auto-Expiry** — Temporary identities self-destruct after a configurable delay via WorkManager. Set it and forget it.

**📦 Secure Export/Import** — Encrypted backups with passphrase protection. Migrate between devices safely.

**🌙 Dark Theme** — Full Material 3 dark mode with dynamic theming support.

**🌍 Bilingual** — French and English interface.

**📴 100% Offline** — No network required. No tracking. No analytics. No ads. Ever.

---

## 📱 Screenshots

<!-- Add screenshots here when available -->
<!-- <p align="center">
  <img src="docs/assets/screenshot_generate.png" width="200" alt="Generate">
  <img src="docs/assets/screenshot_vault.png" width="200" alt="Vault">
  <img src="docs/assets/screenshot_settings.png" width="200" alt="Settings">
</p> -->

*Screenshots coming soon.*

---

## 📥 Download

### Direct APK

Download the latest release:

👉 **[Download APK (latest)](https://github.com/AnonForge-EU/anonforge/releases/latest)**

1. Download the `.apk` file from the release page
2. Enable "Install from unknown sources" in Android settings
3. Install and launch

### F-Droid

*Planned — coming soon.*

### Build from Source

```bash
git clone https://github.com/AnonForge-EU/anonforge.git
cd anonforge
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🛡️ Security & Privacy

AnonForge is built with zero-knowledge principles:

| Layer | Protection |
|-------|-----------|
| **Storage** | AES-256-GCM encryption via SQLCipher + Android Keystore |
| **Memory** | Sensitive data wiped after use (`CharArray.fill`) |
| **Screen** | `FLAG_SECURE` blocks screenshots on all screens |
| **Network** | Fully offline by default. Optional APIs use user-provided keys only |
| **Logging** | No sensitive data logged, even in debug builds |
| **Auth** | Biometric (fingerprint/face) with PIN fallback and auto-lock timeout |
| **Compliance** | OWASP Mobile Top 10 audited |

**We collect nothing. Your identities never leave your device.**

See [SECURITY.md](SECURITY.md) for vulnerability reporting.

---

## 🏗️ Architecture

```
Clean Architecture — Strict layer separation

┌─────────────────────────────────────┐
│  UI (Jetpack Compose + Material 3)  │
│  feature/generator • feature/vault  │
├─────────────────────────────────────┤
│  Domain (Pure Kotlin)               │
│  UseCases • Models • Repository IF  │
├─────────────────────────────────────┤
│  Data (Room + SQLCipher)            │
│  DAOs • Entities • Repo Impl       │
├─────────────────────────────────────┤
│  Core (Security • DI • Theme)       │
│  Keystore • Hilt Modules • Utils    │
└─────────────────────────────────────┘
```

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room + SQLCipher |
| Async | Coroutines + Flow |
| Security | Android Keystore, BiometricPrompt |
| Background | WorkManager (auto-expiry) |
| Min SDK | 26 (Android 8.0) |

---

## 🤝 Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

Ways to help: bug reports, feature suggestions, translations (EN/FR), documentation, code improvements.

```bash
# Clone & build
git clone https://github.com/AnonForge-EU/anonforge.git
cd anonforge
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest
```

---

## ⚖️ Legal & Ethical Use

AnonForge is designed for **legitimate privacy protection only** — avoiding spam, protecting personal data during sign-ups, testing applications.

**Not intended for**: fraud, impersonation, bypassing legal verification (government, banking), or any illegal activity. You are solely responsible for your usage.

See [DISCLAIMER.md](DISCLAIMER.md) for full terms.

---

## 💖 Support

AnonForge is free, open-source, and ad-free. If you find it useful:

- ⭐ **[Star this repo](https://github.com/AnonForge-EU/anonforge)** — visibility helps!
- 💰 **[Donate](https://anonforge-eu.github.io/anonforge-pages/)** — voluntary contributions on our website
- 🐛 **Report bugs** — open an issue
- 📢 **Share** — tell privacy-conscious friends

---

## 📄 License

```
Copyright 2025 AnonForge-EU

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for full text.

---

<p align="center">
  <strong>Built with ❤️ for privacy in the EU 🇪🇺</strong><br>
  <sub>AnonForge does not encourage illegal activities. Use responsibly.</sub>
</p>

<p align="center">
  <a href="https://anonforge-eu.github.io/anonforge-pages/">Website</a> •
  <a href="PRIVACY.md">Privacy Policy</a> •
  <a href="DISCLAIMER.md">Disclaimer</a> •
  <a href="SECURITY.md">Security</a>
</p>

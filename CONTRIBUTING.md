# Contributing to AnonForge

Thanks for your interest in improving AnonForge! Every contribution helps protect privacy.

## How to Contribute

### Bug Reports

Open an [issue](https://github.com/AnonForge-EU/anonforge/issues) with:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if relevant

### Feature Suggestions

Open an issue tagged `enhancement`. Describe the use case and how it fits AnonForge's privacy-first philosophy.

### Code Contributions

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/your-feature`
3. **Commit** with clear messages: `git commit -m "feat: add German address formats"`
4. **Push** to your fork: `git push origin feature/your-feature`
5. **Open** a Pull Request with a clear description

### Translations

We currently support **French** and **English**. To add a language:
1. Copy `app/src/main/res/values/strings.xml`
2. Create `app/src/main/res/values-XX/strings.xml` (where XX is the language code)
3. Translate all strings
4. Submit a PR

## Code Guidelines

- **Language**: Kotlin (no Java)
- **Architecture**: Clean Architecture with strict layer separation (domain → data → UI)
- **UI**: Jetpack Compose + Material 3 — no XML layouts
- **DI**: Hilt for dependency injection
- **Async**: Coroutines + Flow (no RxJava)
- **Testing**: JUnit 5 + MockK for unit tests

### Security Rules

- Never log sensitive data (identities, API keys, PINs)
- Wipe sensitive `CharArray` / `ByteArray` after use
- Use `@VisibleForTesting` sparingly — prefer testing through public interfaces
- All new data storage must go through SQLCipher encrypted database

## Development Setup

```bash
git clone https://github.com/AnonForge-EU/anonforge.git
cd anonforge
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requirements: Android Studio Ladybug+, JDK 17+, Android SDK 26+.

## Priority Areas

We especially welcome help with:
- 🌍 **Translations** — reach more users
- 🐛 **Bug fixes** — stability and edge cases
- ♿ **Accessibility** — screen readers, contrast, content descriptions
- 📖 **Documentation** — guides, comments, examples

## Code of Conduct

Be respectful, constructive, and inclusive. We're all here to protect privacy.

Application Android gratuite et open-source pour générer des identités jetables sécurisées (privacy tool).
# AnonForge

**Secure Disposable Identity Generator for Android**

AnonForge is a **100% offline, open-source Android app** that generates realistic temporary identities (names, addresses, emails, phones) to protect your privacy during low-stakes online sign-ups.

**No tracking. No accounts. No data collection. Everything stays on your device.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Downloads](https://img.shields.io/github/downloads/YourUsername/AnonForge/total.svg)](https://github.com/YourUsername/AnonForge/releases)
[![Stars](https://img.shields.io/github/stars/YourUsername/AnonForge.svg)](https://github.com/YourUsername/AnonForge/stargazers)

## Why AnonForge?

In 2026, billions of personal records have been leaked worldwide — including over 700 million French records from various breaches. Sharing your real email, phone, or address with untrusted sites exposes you to spam, tracking, identity theft, and worse.

AnonForge helps you:
- Generate **realistic, disposable identities** for casual sign-ups (newsletters, forums, trials).
- Keep your real data private.
- Stay ethical — this tool is for **privacy protection only**, not fraud.

## Features

- **Offline Generation**: Weighted random names, addresses, phones, and DOB (18-85 years by default, configurable ranges).
- **Nationality Preference**: Support for French, English, German datasets (names, streets, cities) — realistic per country.
- **Email Aliases**: Integration with SimpleLogin API for real forwarding aliases (optional, user-configured).
- **Phone Aliases**: Manual entry with guides for EU services (OnOff, Sonetel, Zadarma, JMP.chat).
- **Secure Storage**: Encrypted database (SQLCipher + Android Keystore).
- **Biometric Unlock**: Fingerprint/face ID with PIN fallback and auto-lock timeout.
- **Export/Import**: Encrypted backups with passphrase.
- **Auto-Cleanup**: Temporary identities expire automatically (WorkManager).
- **No Ads, No Tracking**: Completely free and privacy-respecting.

## Security & Privacy

AnonForge is built with **military-grade security** and zero-knowledge principles:

- **Encryption**: All data encrypted at rest with AES-256-GCM via SQLCipher and Android Keystore.
- **Memory Safety**: Sensitive data (API keys, PINs) wiped from memory after use (CharArray.fill).
- **No Network by Default**: Fully offline. Optional alias APIs use user-provided keys only.
- **No Logging**: No sensitive data logged, even in debug builds.
- **FLAG_SECURE**: Screenshots blocked on all screens.
- **OWASP Mobile Top 10 Compliant**: Regular audits for insecure storage, weak crypto, etc.

**We collect nothing. Your identities never leave your device.**

## Alias Providers (Why These Choices?)

We recommend **user-managed third-party services** for real forwarding aliases (no built-in backend to avoid central risks):

### Email Aliases
- **SimpleLogin** (recommended): Privacy-focused, EU-friendly, real forwarding.
  - Pros: Open-source client, unlimited with premium, strong anti-spam.
  - Cons: Free tier limited to 10 aliases.
  - Setup: Create account at [simplelogin.io](https://simplelogin.io), copy API key into app Settings.

### Phone Aliases (Manual Entry)
No perfect "one-click" EU solution exists in 2026 due to regulations. Users enter numbers manually:

- **OnOff** (FR/EU numbers): Reliable forwarding.
- **Sonetel** (web-based, cheap EU numbers).
- **Zadarma** (low-cost, EU focus).
- **JMP.chat** (privacy-first, crypto payments).

**Important**: These services may log usage on their side. Use for low-sensitivity only.

## Legal & Ethical Use

AnonForge is **legal** when used ethically:
- Designed for **privacy protection** in legitimate scenarios (e.g., avoiding spam from newsletters).
- **Not intended** for fraud, impersonation, illegal activities, or bypassing official verification (government sites, banking, etc.).

**You are responsible for your usage**. Comply with all applicable laws.

## Disclaimers

**STRONG WARNING**:

- This app is for **ethical privacy testing and protection only**.
- Do **NOT** use for:
  - Fraud, scams, or illegal activities.
  - Impersonating others.
  - Bypassing legal verification (e.g., government, financial services).
- Generated identities are synthetic — using them fraudulently is illegal and unethical.
- Third-party alias services (SimpleLogin, Twilio, etc.) have their own terms — read them.

By using AnonForge, you agree to use it responsibly.

## Installation

1. Download the latest APK from [Releases](https://github.com/YourUsername/AnonForge/releases).
2. Enable "Install from unknown sources" in Android settings.
3. Install and launch.

**Future**: Available on F-Droid (planned).

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md).

- Report bugs
- Suggest features
- Improve translations (EN/FR currently)

## Support the Project

AnonForge is free and open-source. Voluntary donations help maintain and improve it:

[Donate via Stripe](your-stripe-link-here)

Thank you ❤️

## License

[MIT License](LICENSE) — free to use, modify, and distribute.

---

Made with ❤️ for privacy.

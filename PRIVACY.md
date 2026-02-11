# Privacy Policy

**Last updated**: February 2025

## Summary

AnonForge collects **no personal data**. Period.

## Data Storage

- All generated identities and settings are stored **locally on your device only**.
- Data is encrypted at rest with **AES-256-GCM** via SQLCipher and Android Keystore.
- No data is transmitted to any server by default.

## Network Access

- AnonForge is **fully offline by default** — no internet permission required for core features.
- **Optional**: If you configure SimpleLogin email aliases, the app connects to SimpleLogin's API using **your own API key**. We never see, store, or transmit your key beyond your device's encrypted storage.

## What We Don't Do

- ❌ No analytics or telemetry
- ❌ No crash reporting
- ❌ No advertising or ad SDKs
- ❌ No third-party tracking
- ❌ No data collection of any kind
- ❌ No cloud sync (unless you manually export)

## Third-Party Services

If you choose to use optional integrations (SimpleLogin for email aliases), those services have their own privacy policies. We recommend reviewing them:

- [SimpleLogin Privacy Policy](https://simplelogin.io/privacy/)

## Permissions

| Permission | Purpose | Required? |
|-----------|---------|-----------|
| `USE_BIOMETRIC` | Fingerprint/face unlock | Optional |
| `INTERNET` | SimpleLogin API only | Optional |
| `RECEIVE_BOOT_COMPLETED` | Auto-expiry scheduling | Optional |

## Open Source

AnonForge is fully open-source. You can audit every line of code at [github.com/AnonForge-EU/anonforge](https://github.com/AnonForge-EU/anonforge).

## Contact

Questions about privacy? Open an issue on [GitHub](https://github.com/AnonForge-EU/anonforge/issues).

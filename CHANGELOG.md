# Changelog

All notable changes to AnonForge are documented in this file.
The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] — 2026-04-28

Major security & SimpleLogin pass. Existing data (vault, alias history, settings,
PIN, biometric flag) is preserved across the upgrade — see the migration notes
at the bottom of this section.

### Added
- **Pull-to-refresh** on the vault screen.
- **Slide + fade transitions** between navigation destinations.
- **Inline validation** on the custom-expiry-days field (1–365 days),
  with the Save button disabled when the value is out of range.
- **SimpleLogin: alias deletion on the server** (`DELETE /api/aliases/{id}`).
  Each alias row in the history offers a choice between local-only delete
  and full delete on SimpleLogin.
- **SimpleLogin: enable/disable toggle** (`POST /api/aliases/{id}/toggle`)
  with a switch on each remote-managed row.
- **SimpleLogin: live alias details** (`GET /api/aliases/{id}`) and
  **alias update** (`PATCH /api/aliases/{id}`) endpoints exposed.
- **SimpleLogin: full pagination** when fetching the alias list
  (previously only the first page was loaded).
- **Self-hosted SimpleLogin instance** support: a new setting in
  *Email Aliases* lets you point the app at your own instance over HTTPS.
- **Random alias mode** (UUID vs Word) selectable in *Email Aliases*.
  Word mode requires a SimpleLogin premium subscription.
- **`mailbox_ids` field** on the custom-alias creation request, for
  accounts with multiple mailboxes.

### Changed
- **Biometric authentication is now bound to a Keystore key**
  (`setUserAuthenticationRequired(true)` + `AUTH_BIOMETRIC_STRONG`) and
  performs a real cryptographic operation through
  `BiometricPrompt.CryptoObject`. Bypassing the prompt no longer grants
  access — the cipher operation actually has to complete.
- **PIN is stretched with PBKDF2-HMAC-SHA256** (200 000 iterations,
  16-byte salt) and verified in constant time. The previous
  AES-encrypted PIN format is still recognised once and silently
  re-stored in the new format on the next successful unlock.
- **TLS 1.2+ is enforced** on every SimpleLogin call
  (`ConnectionSpec.MODERN_TLS`); legacy SSLv3/TLSv1/TLSv1.1 are refused.
- **Re-authentication on app resume**: when the auto-lock timeout has
  elapsed while the app was in the background, the activity is recreated
  so the navigation graph routes through the unlock screen again.
- **`collectAsState()` migrated to `collectAsStateWithLifecycle()`**
  across every screen — flows now pause when the UI leaves the screen.

### Removed
- Unused `POST_NOTIFICATIONS` runtime permission (never wired to a
  notification channel).

### Fixed
- README inaccuracies: `Min SDK` corrected from 26 to 29 and the
  build instructions no longer recommend `assembleDebug` for end users.
- LazyColumn missing-`key` recompositions in the phone-alias dialog.

### Migration notes
- **Existing PIN keeps working.** It is silently re-hashed in the new
  PBKDF2 format the first time you unlock with it.
- **Existing biometric setup keeps working.** The first biometric
  prompt after the upgrade transparently provisions the new
  cryptographic gate (single prompt, no extra step).
- **Vault data, alias history and settings are untouched** — the
  database schema and SQLCipher passphrase are unchanged.
- If you add or remove a fingerprint after upgrading, the biometric
  gate will detect the change and ask you to re-enable biometric in
  *Settings* (a single prompt). PIN stays available as fallback.

## [1.0.0] — 2026-02-11

Initial public release.

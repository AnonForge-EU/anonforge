# Changelog

All notable changes to AnonForge are documented in this file.
The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] — 2026-07-19

Security polish and honest documentation. **Signed with the same keystore as
1.1.0/1.2.0 — updates in place, no uninstall, vault/PIN/aliases preserved.**
No database schema change (Room stays at version 5), no storage format change.

### Fixed
- **PIN-only lock is now enforced at launch** — the splash routing only sent
  users to the unlock screen when *biometric* unlock was enabled, so a vault
  protected by a PIN alone opened without ever asking for it (and the
  auto-lock re-check took the same path). The app now routes through the
  unlock screen whenever a PIN **or** biometric is configured.
  ⚠️ If you set up a PIN and never saw it requested since, you will be asked
  for it after this update — make sure you remember it (biometric users are
  unaffected; without any PIN/biometric nothing changes).
- **Secure clipboard everywhere** — copying from the generator screen and the
  phone-alias list now goes through the same secure clipboard as the vault:
  marked sensitive (hidden from clipboard previews on Android 13+) and
  auto-cleared after 30 seconds. Previously those two paths used the raw
  system clipboard with no auto-clear.
- **Export password wipe** — the backup password buffer is now overwritten
  with null characters (`\u0000`) instead of ASCII `'0'` after use. The
  export file format itself is unchanged; existing backups import fine.
- **PIN unlock no longer blocks the UI thread** — PIN verification became a
  proper suspend call, and the auto-lock timeout is now cached in memory
  (fail-closed while loading) instead of a blocking DataStore read on every
  foreground check.
- **Settings/About now shows the real app version** — it displayed a
  hard-coded "1.0.0" regardless of the installed release; it now reads
  `BuildConfig.VERSION_NAME`.
- **Saved virtual numbers now actually show up** — the saved-numbers list
  and the "Virtual Numbers" status row stayed empty even with numbers in the
  database: with Room 2.8 over the SQLCipher factory, a second concurrent
  subscription on the same table stalled silently, and the old
  `catch { emptyList() }` hid every error. The repository now shares one
  upstream subscription (seeded by a direct snapshot, bounded retries,
  errors logged), the phone-number selection dialog in the generator is
  finally wired up, and picking a saved national-format number no longer
  crashes (the `Phone` model accepts everything the input screen accepts —
  strictly widening, all previously valid values stay valid).

### Changed
- **Honest security wording** — README and in-code docs now describe exactly
  what the app does: full-database encryption at rest via SQLCipher (AES-256)
  with the passphrase in the Android Keystore. Removed the misleading
  "field-level / never stored in plaintext" and "100% offline" phrasing
  (the app is offline **by default**; SimpleLogin is an optional connection).

### Removed
- **Dead code**: the unused `EncryptionBridge` field-encryption layer and its
  Hilt wiring. It was never called — identity fields were, and still are,
  protected by the full-database SQLCipher encryption. The legacy Keystore
  alias is still cleaned up by `clearAllKeys()` on devices where older builds
  created it.

### Internal
- `DateOfBirthGenerator` moved from `feature.generator` to `generator`,
  removing the domain → feature dependency.
- Unit tests now exercise the real `ExportCryptoManager` (production
  constants: PBKDF2-HMAC-SHA256 100k iterations, AES-256-GCM) instead of a
  drifted mirror implementation that claimed 600k iterations, plus new
  `PhoneGenerator` format tests (FR/UK/DE).

### Migration notes
- Updates normally over 1.2.0 / 1.1.x (same signing key). Vault, alias
  history, settings, PIN and biometric setup all carry over untouched.

## [1.2.0] — 2026-06-07

Toolchain modernization + vault usability. **Signed with the same keystore as
1.1.0 — this updates in place over 1.1.0, no uninstall, all data preserved.**

### Added
- **Manual identity entry** — a dedicated form (reachable from the vault)
  lets you type every field yourself instead of generating randomly, e.g.
  to preserve an existing identity. Only first + last name are required;
  phone, date of birth, address, email, label and expiry are optional.
- **Vault search** — a search field filters identities in real time by name,
  custom name, email alias, or phone number.
- **Vault sorting** — sort by newest first (default), soonest to expire, or
  name A–Z.

### Changed — dependency & toolchain upgrades
- **Android Gradle Plugin 8.2.2 → 9.2.0** (Gradle 9.0-milestone-1 → 9.4.1).
  Adopts AGP's built-in Kotlin support (the standalone `kotlin-android`
  plugin is no longer applied).
- **Kotlin 2.0.0 → 2.3.21** (KSP → 2.3.9), compiler options migrated to the
  modern `compilerOptions` DSL. (Held at 2.3.x rather than 2.4.0 because the
  latest Hilt, 2.59.2, only reads Kotlin metadata up to 2.3.0.)
- **Compose BOM 2024.12.01 → 2026.06.00**, **Hilt 2.52 → 2.59.2**,
  **Room 2.6.1 → 2.8.4**, **OkHttp 4.12.0 → 5.3.2**,
  **coroutines 1.8.1 → 1.11.0**, **serialization 1.6.3 → 1.11.0**.
- **AndroidX**: core-ktx 1.19.0, lifecycle 2.10.0, activity-compose 1.13.0,
  navigation-compose 2.9.8, work 2.11.2, datastore 1.2.1,
  hilt-navigation/work 1.3.0, sqlite-ktx 2.6.2.
- **targetSdk / compileSdk 34 → 35** (Android 15).

### Removed
- Deprecated `android.enableJetifier` flag (no legacy support-library deps).

### Kept intentionally
- **SQLCipher stays at `android-database-sqlcipher` 4.5.4.** The maintained
  successor (`sqlcipher-android`) is a package migration that would risk the
  existing encrypted vault's openability. Held back to guarantee in-place
  upgrades preserve data; revisit as a dedicated, well-tested change.

### Migration notes
- Updates normally over 1.1.0 (same signing key). Vault, alias history,
  settings, PIN and biometric setup all carry over untouched.

## [1.1.0] — 2026-04-28

Major security & SimpleLogin pass.

> ### ⚠️ Breaking — signing key rotated
>
> The 1.0.0 signing keystore was lost. 1.1.0 is signed with a new keystore,
> so Android refuses an in-place update from 1.0.0 (`signature mismatch`).
> **You must uninstall 1.0.0 before installing 1.1.0.** Uninstalling
> wipes the encrypted vault — back it up first via the in-app *Export*
> feature, then re-import after the fresh install.
>
> Future releases (1.1.x onward) will be signed with this new keystore
> and will update normally.

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

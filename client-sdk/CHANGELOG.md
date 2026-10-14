# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.2.0] - 2026-10-14

### Added
- `authenticate(scope, nonce, refreshNonce)`: Paspo asks the SDK for a new nonce when the current one expires (while the user installs Paspo and signs up, or when the consent screen stays open too long), and the flow goes on instead of ending with `SESSION_EXPIRED`. The ready-made buttons pass their `nonceProvider` as `refreshNonce`.
- When Paspo is not installed, the store link carries the client package and the requested scope in `referrer`, so Paspo runs a short sign-up for this client.
- The SDK manifest declares visibility of Google Play (`<queries>`), which the inline install sheet needs on Android 11+.

### Changed
- Google Play opens as an inline install sheet on top of the client app. Without Google Play the SDK opens `market://`, and Android picks the store installed on the device (RuStore, AppGallery, GetApps) with the same `referrer`. Without any store the Paspo ID website opens (`https://paspo.id/mob/s/<referrer>`).
- When Paspo is not installed, `authenticate` no longer returns right after opening the store. It waits for the installation and then opens the Paspo consent screen by itself: a new user signs up in Paspo and comes back to the consent screen, without tapping the sign-in button again. `NotInstalled` is returned only when the user comes back to your app without installing Paspo.
- After Paspo is installed, the SDK closes the Google Play screens left on top of your activity: it brings your activity back with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`, so it receives `onNewIntent` with the `PaspoID.EXTRA_RETURNED_FROM_STORE` extra and nothing else. Skip your usual intent handling for such intents.
- Supporting `PaspoID.ACTION_SIGN_IN` is now required. Paspo uses it to bring a user who has just signed up back to sign-in in your app when the flow started by `authenticate` is gone, for example after the system killed your app during the sign-up.

## [1.0.0] - 2026-06-12

### Added
- `PaspoID(activity)` - the SDK entry point.
- `authenticate(scope, nonce)` - starts the SSO flow via the Paspo app; a single result channel `PaspoAuthResult` (`Success` / `Cancelled` / `NotInstalled` / `Failure`).
- `checkInstallation()` - checks whether the Paspo app is installed.
- `cleanup()` - manual destruction of the ephemeral encryption keys.
- `PaspoScope` - requested profile data: `PHONES`, `EMAILS`, `NATIONAL_ID`, `PASPO_ID`.
- `PaspoID.ACTION_SIGN_IN` - intent action for sign-in initiated from the Paspo app (optional intent-filter on the client side).
- `PaspoClientError` - flow error codes.
- End-to-end encryption of the exchange with Paspo: ECDH P-256 + HKDF (HMAC-SHA256) + AES-256-GCM, ephemeral in-memory keys.
- Play Store redirect when Paspo is not installed, with referral nonce hand-off after installation.
- R8 consumer rules shipped inside the AAR - clients need no keep rules of their own.

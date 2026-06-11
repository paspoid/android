# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - unreleased

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

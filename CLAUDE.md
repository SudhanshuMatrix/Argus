# Argus

Scam protector for parents' Android phones, built with Flutter. It reads incoming SMS and
WhatsApp messages on the phone, checks them with scam rules and an on-device Gemma model, and
warns the parent loudly before they act. Three flavors (`lib/app/edition.dart`): offline (no
internet), online (model download + live alerts), demo (warnings and live alerts only).
`docs/PLAN.md` is the upstream plan of the original Kotlin app: history and design notes.

## Stack
- Flutter (stable 3.47), Dart, Material 3. Android only.
- `lib/core/`: the scam engine. Pure Dart: no Flutter, plugin or Android imports.
- `lib/app/`: settings, alerting, listeners (SMS via another_telephony, WhatsApp via
  flutter_notification_listener), voice (audioplayers + flutter_tts for Hindi), notifications
  (flutter_local_notifications), Gemma (flutter_edge_ai + LiteRT-LM), live alerts (Firebase Messaging).
- `backend/`: the Go live-alert server (standard library only). `go test ./...` must pass.
- `lib/ui/`: screens. No business logic in widgets.
- Strings: `lib/l10n/app_en.arb` and `app_hi.arb` (English and Hindi only; English is the
  template and fallback). Every key needs both.
- APKs are built by `.github/workflows/build-apk.yml`.

## Hard rules
- The offline flavor NEVER gets the INTERNET permission (its manifest removes it; CI fails if
  it appears). Online and demo use the internet only for the model download and live alerts.
- Message text is never logged (no `print`), saved to disk, put in a notification payload or
  Intent, or sent anywhere, in any flavor. Notifications carry fixed words only
  (`AlertCodec.toPayload`). Secrets (passwords, keys, Firebase settings) never go in the repo.
- Background handlers are top-level `@pragma('vm:entry-point')` functions and never throw.
- Pin dependency versions in pubspec.yaml and read a package's real API before using it.
- Ask before adding a dependency.

## UI for older users
- Body text at least 18sp, headlines 28sp+, buttons at least 56dp tall and full width.
- High contrast, one main action per screen, plain words, no icon-only buttons.

## Definition of done
1. `flutter analyze` is clean and `flutter test` passes.
2. The Build APK workflow is green.
3. Commit with a clear Conventional Commit message.
4. Give a short checklist of exactly what to test on the phone.

# Argus

Offline scam protector for parents' Android phones, built with Flutter. It reads incoming SMS
and WhatsApp messages on the phone, checks them with fixed scam rules, and warns the parent
loudly before they act. An on-device Gemma checker is planned next (`docs/PLAN.md` is the
upstream plan and build log of the original Kotlin app; treat it as history and design notes).

## Stack
- Flutter (stable 3.47), Dart, Material 3. Android only.
- `lib/core/`: the scam engine. Pure Dart: no Flutter, plugin or Android imports.
- `lib/app/`: settings, alerting, listeners (SMS via another_telephony, WhatsApp via
  flutter_notification_listener), voice (audioplayers), notifications (flutter_local_notifications).
- `lib/ui/`: screens. No business logic in widgets.
- Strings: `lib/l10n/app_en.arb` and `app_hi.arb` (English and Hindi only; English is the
  template and fallback). Every key needs both.
- APKs are built by `.github/workflows/build-apk.yml`.

## Hard rules
- NEVER add the INTERNET permission or any networking package. The manifest removes INTERNET
  and CI fails if the release APK has it.
- Message text is never logged (no `print`), saved to disk, put in a notification payload or
  Intent, or sent anywhere. Notifications carry fixed words only (`AlertCodec.toPayload`).
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

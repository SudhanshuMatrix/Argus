# Argus

**An offline scam protector for your parents' Android phone.**

Argus reads every incoming text message (SMS) and WhatsApp message **on the phone itself** and warns loudly, with a red screen, strong vibration and a spoken warning, before your mum or dad clicks a link, sends money or shares a code.

It is built for parents and grandparents who use Android, WhatsApp and SMS every day, aren't technical, and will never open an app to "check" a message. Their son or daughter installs it once, in about ten minutes, and it then works on its own. It speaks **English and Hindi**.

This is the Flutter version. It checks messages with fixed scam rules; an AI checker (Gemma, also running on the phone) comes in a later version.

## The privacy promise

- **No internet permission.** The release app cannot connect to the internet at all. Android enforces this, and the build fails if the permission ever sneaks in (see `.github/workflows/build-apk.yml`).
- **Messages never leave the phone.** Every check runs on the phone.
- **Messages are never stored or logged.** They are checked in memory and forgotten. Notifications only carry fixed words ("Stop! This looks like a scam."), never the message.
- **The only thing Argus can send** is an optional text message to one family member when a scam arrives: "Argus: Mummy just received a likely scam message. Please give Mummy a call." It never includes the message, who sent it, or why it was flagged.

| Permission | Why |
| --- | --- |
| Receive SMS | To check text messages as they arrive |
| Send SMS | Only for the optional family alert |
| Notifications, full-screen notifications | To show warnings, and the red warning over the lock screen |
| Notification access (a setting) | To read WhatsApp messages from their notifications |
| Ignore battery optimisation | So the phone doesn't put Argus to sleep |
| Vibrate, wake lock, foreground service, start at boot | Scam warnings vibrate; WhatsApp checking keeps running after a restart |

## Get the APK

Every push to `main` builds a release APK with GitHub Actions:

1. Open the repo's **Actions** tab, then the latest **Build APK** run.
2. Download the **Argus-apk** artifact (a zip with `Argus-<version>.apk` and its SHA-256).
3. Push a tag like `v0.1.0` to get the APK attached to a GitHub **Release** instead.

Without signing secrets the APK is signed with a throwaway debug key, so a newer build won't install over an older one: uninstall first. To sign with your own key, add the repository secrets `ARGUS_KEYSTORE_BASE64` (`base64 -w0 your.jks`), `ARGUS_KEYSTORE_PASSWORD`, `ARGUS_KEY_ALIAS` and `ARGUS_KEY_PASSWORD`.

## Install it on your parent's phone

Android 8 or newer.

1. Copy the APK to the phone and open it. Allow "Install unknown apps" for your file manager if asked.
2. Open Argus and follow the setup:
   - Who it is for (Mummy, Daddy, Grandma or their name), the language (English or हिन्दी), and your name and number for the **Call** button and the optional family SMS.
   - Allow text messages and warnings (and the full-screen warning on Android 14+).
   - Turn on WhatsApp checking. On Android 13+ a sideloaded app first shows "Restricted setting": open **App info**, tap ⋮, **Allow restricted settings**, and try again.
   - Let Argus run in the background, plus the extra step for Xiaomi/Redmi/POCO or Tecno/Infinix/itel phones.
   - **Send a test scam** to see and hear the warning.

## How it works

1. **Skip filter:** "ok", "Amen" and emoji-only messages are skipped.
2. **Real OTP rule:** a genuine bank code (with no link and nobody asking for it) gets a calm "never share this code" note instead of an alarm.
3. **Hard rules** raise the alarm at once: a link with "blocked / verify / KYC-style" account words, anyone asking for an OTP, PIN or password, money requested from a "new number", a prize that needs a fee, a promised investment return with a money request, or "call this number" about account trouble from someone claiming to be a bank.
4. **Safety floor:** messages with two or more warning signs, a promised investment return, or "call this number" about account trouble get a quieter "be careful" note.
5. **Fixed reasons:** every warning explains itself in a fixed sentence built from what the rules found ("It has a link and says your account is blocked."), and the warning screen marks the dangerous parts of the message. Links are shown but can't be tapped.

The engine lives in `lib/core/` (pure Dart, no Flutter or Android imports) and is covered by the tests in `test/core/`. It is a port of the original Kotlin engine, with the same test cases.

## Limitations

- The rules read English (and Nigerian Pidgin) wording, with Nigerian phone numbers and naira amounts. Messages written in Hindi script, Indian phone numbers and rupee amounts are mostly not recognised yet; the AI checker and India-specific rules are the next steps.
- Messages only, not phone calls. Photos and voice notes aren't checked.
- A WhatsApp chat that is open on screen is missed (WhatsApp shows no notification for it).
- Hindi voice clips aren't recorded yet, so Hindi warnings play the English voice.
- It can be wrong, and it never says a message is safe.

## Build from source

```sh
flutter pub get
flutter analyze
flutter test
flutter build apk --release
```

Flutter 3.47 (stable) and JDK 17 or newer. The APK is in `build/app/outputs/flutter-apk/`.

Demo messages for trying it out are in [docs/DEMO.md](docs/DEMO.md).

## Credits and licences

- **Based on [ScamGuard](https://github.com/abgolor/ScamGuard)** by Abraham Golor, used and modified with the author's permission. See [NOTICE](NOTICE).
- **Voice by ElevenLabs.** The warning clips were generated once with ElevenLabs and ship inside the app; no voice service is called at run time.
- **Atkinson Hyperlegible Next** font by the Braille Institute, under the SIL Open Font License 1.1 (see NOTICE).
- **Gemma** (for the coming AI checker) is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms; see NOTICE.

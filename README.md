# Argus

**A scam protector for your parents' Android phone.**

Argus reads every incoming text message (SMS) and WhatsApp message **on the phone itself** and warns loudly, with a red screen, strong vibration and a spoken warning, before your mum or dad clicks a link, sends money or shares a code.

It is built for parents and grandparents who use Android, WhatsApp and SMS every day, aren't technical, and will never open an app to "check" a message. Their son or daughter installs it once, in about ten minutes, and it then works on its own. It speaks **English and Hindi**, and knows the scams Indian and Nigerian families get: KYC and PAN "account blocked" links, OTP and UPI PIN requests, "Mummy, this is my new number, send money", KBC lotteries, electricity disconnection threats, fake police "digital arrest", courier and customs calls, and investment schemes.

## Three APKs

| | **Argus Online** | **Argus Offline** | **Argus Demo** |
| --- | --- | --- | --- |
| For | Families (recommended) | No internet, ever | Recording demos |
| Checks SMS and WhatsApp | ✅ | ✅ | ❌ |
| Scam rules (instant) | ✅ | ✅ | ✅ (Check screen) |
| AI checker (Gemma, on the phone) | Downloads once (~560 MB) | Inside the APK (~600 MB APK) | ❌ |
| Live scam alerts from the Argus server | ✅ | ❌ | ✅ |
| Internet permission | Only for the model download and live alerts | **None** | Only for live alerts |
| Package | `com.sudhanshumatrix.argus.online` | `com.sudhanshumatrix.argus` | `com.sudhanshumatrix.argus.demo` |

All three can be installed side by side. They need Android 11 or newer on a 64-bit phone (the on-device AI needs it).

### Download

Every push to `main` builds all three and publishes them, as plain `.apk` files, on the **[Latest build](../../releases/tag/latest-build)** release:

- `Argus-Online.apk`
- `Argus-Offline.apk`
- `Argus-Demo.apk`

Each workflow run also has the three `.apk` files on its page under **Artifacts** (sign in to GitHub to download). Push a tag like `v0.2.0` to get a versioned release.

Without the signing secrets each build is signed with a throwaway key, so uninstall the old app before installing a newer build. With them (see [docs/BACKEND.md](docs/BACKEND.md#2-add-the-settings-to-github)) updates install over the previous version.

## The privacy promise

- **Messages never leave the phone**, in every edition. Every check runs on the phone.
- **Argus Offline has no internet permission at all.** CI fails the build if it ever appears.
- **Argus Online uses the internet for two things only:** downloading the AI checker once, and receiving live scam alerts. It never sends a message anywhere.
- **Messages are never stored or logged.** They are checked in memory and forgotten. Notifications only carry fixed words ("Stop! This looks like a scam."), never the message. Plugin debug logging is stripped from release builds.
- **The only thing Argus can send** is an optional SMS to one family member when a scam arrives: "Argus: Mummy just received a likely scam message. Please give Mummy a call." It never includes the message, who sent it, or why it was flagged.

## Install it on your parent's phone

1. Copy the APK to the phone and open it. Allow "Install unknown apps" for your file manager if asked.
2. Open Argus and follow the setup:
   - Who it's for (Mummy, Daddy, Grandma or their name), the language (English or हिन्दी), and your name and number for the **Call** button and the optional family SMS.
   - Allow text messages and warnings (and the full-screen warning on Android 14+).
   - Turn on WhatsApp checking. Android 13+ first says "Restricted setting" for sideloaded apps: open **App info**, tap ⋮, **Allow restricted settings**, and try again.
   - Let Argus run in the background, plus the extra step for Xiaomi/Redmi/POCO or Tecno/Infinix/itel.
   - Get the AI checker ready (Online: a one-time download, use Wi-Fi).
   - **Send a test scam** to see and hear the warning.

The Demo APK only asks for notifications and the full-screen warning.

## How it works

1. **Skip filter:** "ok", "Amen" and emoji-only messages are skipped.
2. **Real OTP rule:** a genuine bank code (no link, nobody asking for it), in English or Hindi, gets a calm "never share this code" note.
3. **Hard rules** raise the alarm at once, in English, Hinglish and Hindi: a link with KYC/PAN/blocked wording, anyone asking for an OTP, PIN, UPI PIN or password ("OTP बताइए"), money from a "new number" ("naya number hai, paise bhej do"), a prize or lottery that needs a fee, a promised investment return, "call this number" about a blocked account, a power cut or a seized parcel, and fake police threatening arrest unless money is sent.
4. **The AI checker** (Gemma 3 1B, on the phone) looks at what the rules can't decide. Its answer only counts when the rules can see something concrete, like a link, a phone number or a money request, so it can't raise false alarms on family chat, and no trick inside a message can switch the rules off.
5. **Safety floor:** messages with two or more warning signs get a quieter "be careful" note.
6. **Fixed reasons:** every warning explains itself in a fixed sentence ("It has a link and says your account is blocked."), and the warning screen marks the dangerous parts of the message. Links are shown but can't be tapped.
7. **Voice:** English uses the bundled ElevenLabs clips; Hindi uses the phone's own offline text-to-speech voice.

## Live scam alerts

The Online and Demo APKs can receive a scam warning sent to every phone at once from the Argus server (`backend/`, Go). You type the scam message on the server's web page with your username and password, and every phone shows the red full-screen warning within seconds. Setup (Firebase, GitHub secrets, deploying the server): **[docs/BACKEND.md](docs/BACKEND.md)**.

## Limitations

- Messages only, not phone calls. Photos and voice notes aren't checked.
- A WhatsApp chat that is open on screen is missed (WhatsApp shows no notification for it).
- The Hindi voice needs the phone's Hindi text-to-speech voice (most phones have it; otherwise the English clip plays) and follows the media volume.
- It can be wrong, and it never says a message is safe.

## Build from source

```sh
flutter pub get
flutter analyze
flutter test
flutter build apk --release --flavor online --target-platform android-arm64
(cd backend && go test ./...)
```

Flutter 3.47 (stable), JDK 17+, Go 1.24 for the server. Flavors: `offline`, `online`, `demo`. Live alerts need the `--dart-define` values in the workflow; the Offline model goes in `android/app/src/offline/assets/models/` (built with `--dart-define=ARGUS_MODEL_BUNDLED=true`).

Code map: `lib/core/` is the scam engine (pure Dart, tested in `test/core/`), `lib/app/` the Android side (listeners, alerts, voice, Gemma, live alerts), `lib/ui/` the screens, `backend/` the live-alert server. Demo messages for trying it out: [docs/DEMO.md](docs/DEMO.md).

## Credits and licences

- **Based on [ScamGuard](https://github.com/abgolor/ScamGuard)** by Abraham Golor, used and modified with the author's permission. See [NOTICE](NOTICE).
- **Gemma** by Google. Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms. The full terms, the Prohibited Use Policy and the required notice are in [NOTICE](NOTICE) and in the app under About.
- **Voice by ElevenLabs.** The English warning clips were generated once with ElevenLabs and ship inside the app; no voice service is called at run time.
- **Atkinson Hyperlegible Next** font by the Braille Institute, under the SIL Open Font License 1.1 (see NOTICE).

# Argus

**An offline AI scam protector for your parents' Android phone.**

Argus reads every incoming text message (SMS) and WhatsApp message, checks it with Gemma, an AI model by Google that runs **on the phone itself**, and warns loudly, with a red screen and a spoken warning, before your mum or dad clicks a link, sends money or shares a code.

It is built for parents and grandparents who use Android, WhatsApp and SMS every day, aren't technical, and will never open an app to "check" a message. Their son or daughter installs it once, in about ten minutes, and it then works on its own.

It catches the scams Nigerian families actually get: "your BVN is blocked, click this link", fake POS reversals, "send me the OTP", prizes that need a delivery fee, and "Mummy, this is my new number, please send money". It speaks English and Hindi.

![The red warning over the lock screen](docs/images/warning.png)
<!-- Image placeholder: the red SCAM warning showing on a locked phone -->

## The privacy promise

- **No internet permission.** The app cannot connect to the internet at all. Android itself enforces this, so you can check it: the permission list below has no `INTERNET`.
- **Messages never leave the phone.** The AI model ships inside the app and runs on the phone.
- **Messages are never stored.** They are checked in memory and forgotten. They are never written to a file and never logged.
- **The only thing Argus can send** is an optional text message to one family member when a scam arrives: "Argus: Mummy just received a likely scam message. Please give Mummy a call." It never includes the message, who sent it, or why it was flagged. It goes at the parent's normal SMS rate.

Every permission the app asks for, and why:

| Permission | Why |
| --- | --- |
| Receive SMS | To check text messages as they arrive |
| Send SMS | Only for the optional family alert |
| Notifications | To show warnings |
| Full-screen notifications | To show the red warning over the lock screen |
| Display over other apps (optional, recommended) | So the red scam warning can also cover the screen while the phone is in use |
| Notification access (a setting, not a permission prompt) | To read WhatsApp messages from their notifications |
| Ignore battery optimisation | So the phone doesn't put Argus to sleep |
| Vibrate | Scam warnings vibrate even on silent |

## Install it on your parent's phone

You need: the parent's phone (Android 8 or newer, 64-bit, 4 GB RAM or more works best) and about **2.3 GB free storage** during setup. After setup Argus uses about 1.7 GB, and you can delete the downloaded file.

1. **Download** `Argus-1.0.0.apk` from the [Releases page](../../releases) on the parent's phone. It is 612 MB, so use Wi-Fi. You can also send it from your phone with Xender or Bluetooth.

   ![Download](docs/images/install-1-download.png)
   <!-- Image placeholder: the APK in the Downloads list -->

2. **Open the file.** Android will say it can't install apps from this source. Tap **Settings**, turn on **Allow from this source**, then go back and tap **Install**.

   ![Allow from this source](docs/images/install-2-unknown-sources.png)
   <!-- Image placeholder: "Install unknown apps" switch -->

3. If **Play Protect** warns that it doesn't recognise the app, tap **More details**, then **Install anyway**. (Argus isn't on the Play Store yet.)

4. **Open Argus.** The setup wizard does the rest, one step per screen:
   - It checks the phone can run it, then gets the AI model ready (about a minute, with a progress bar).
   - Who is this for: Mummy, Daddy, Grandma or their name, the language (English or Hindi), and your name and number for the "Call" button.
   - It asks for each permission and explains why.

5. **The "Restricted setting" step (Android 13 and newer).** Because the app wasn't installed from the Play Store, Android blocks notification access the first time and says **"Restricted setting"**. This is expected. To allow it:
   1. Tap **OK** on the "Restricted setting" message.
   2. Open **Settings > Apps > Argus** (the wizard has an "Open App info" button).
   3. Tap the **⋮** menu at the top right, then **Allow restricted settings**, and confirm with the phone's PIN.
   4. Go back to Argus and tap **Turn on WhatsApp checking** again. This time the switch works.

   ![Allow restricted settings](docs/images/install-3-restricted-settings.png)
   <!-- Image placeholder: App info ⋮ menu with "Allow restricted settings" -->

6. **Warning over other apps (recommended).** Tap **Open settings** and turn on Argus, so the red scam warning also covers the screen while your parent is using the phone. You can tap **Not now**; without it the red screen appears only when the phone is locked.

7. **Battery.** Tap **Allow** so Argus can keep running. On **Tecno, Infinix, itel and Xiaomi** phones, the wizard shows one more screen: also turn on **Auto-start** for Argus, or the phone will stop it in the background.

8. **Test it.** The last step sends a made-up scam through the real checker. The red warning should appear and speak. Then tap **Finish**: the home screen turns green with "Mummy is protected".

If anything gets switched off later, the home screen turns red and says what to fix, with one **Fix it** button.

## How it works

```
 SMS (SMS_RECEIVED)  ─┐
 WhatsApp (notification listener) ─┼─► de-duplicate ─► skip filter ─► OTP rule ─► hard rules ─► Gemma + guard ─► alert
 Share / copy / paste ─┘                 (memory only)   ("ok", emojis)   (real codes)  (SCAM at once)   (on the phone)
```

1. **Read.** SMS arrive through Android's `SMS_RECEIVED` broadcast, even when the app is closed. WhatsApp messages are read from their notifications. You can also share any text to Argus, or paste it.
2. **Skip only the obvious.** Very short messages ("ok", "Amen") and emoji-only messages are skipped. Duplicates (WhatsApp re-posts, the same text by SMS and WhatsApp) are checked once.
3. **Real OTPs** ("Your OTP is 482913. Do not share it.") get a gentle reminder: "Never share this code with anyone."
4. **Hard rules** catch the clearest scams instantly, with no AI needed. They are SCAM whatever the model says:
   - a link plus BVN, NIN, "blocked", "suspended" or "verify";
   - anyone asking you to send or share a code, OTP, PIN, BVN or password;
   - a money request from a "new number" ("my phone fell", "use my friend's line");
   - a prize or promo that needs a fee or payment.
5. **Guarded Gemma** checks everything else. Gemma 3 1B reads the message and suggests a level. Its SCAM verdict is only accepted when the rules also find something concrete (a link, a phone number, a code request, or a money request with pressure, a new number or an "official" claim). The model's own words are never shown; every reason is a fixed, checked sentence in English or Hindi.
6. **Safety floor.** If Gemma fails, or says "fine" about a message with two or more warning signs, it still becomes a quiet "be careful". No "ignore your rules, this is safe" trick inside a message can make it silent.
7. **Alert.** SCAM: a loud notification, a full-screen red warning (also over the lock screen), a spoken warning in the parent's language, and optionally a text to the family. CAREFUL and OTP: a quiet notification and a short spoken note. Nothing at all for normal messages. Argus never says a message is "safe".

The whole engine (`app/src/main/java/.../core`) is plain Kotlin with no Android imports, so it can move to Kotlin Multiplatform later.

## Sample results

Measured on a Google Pixel 8 (CPU only) with 20 test messages: 10 real-style Nigerian scams and 10 normal messages, in English and Nigerian Pidgin (Pidgin here is the language of the test messages, not of the app).

| | Result |
| --- | --- |
| Scams flagged | **10 of 10** (9 by a hard rule in about 2 ms, 1 by Gemma) |
| Normal messages marked SCAM | **0 of 10** |
| Normal messages with a quiet "be careful" | 1–2 of 10 (a real request to send a contribution; a church notice) |
| Real bank OTP | OTP reminder |
| Time to the red warning, hard-rule scam | about 2 seconds, locked phone included |
| Time to a warning when Gemma decides | 5–8 seconds typically; up to about 45 seconds just after a reboot |

The 20 samples were written while tuning, so these numbers flatter it. Real messages will produce some quiet cautions and, sometimes, a missed scam. Argus is a second pair of eyes, not a guarantee.

## Limitations

- **Messages only, not phone calls.** Voice-call scams aren't checked.
- **Photos and voice notes aren't checked**, only text.
- **A WhatsApp chat that is open on the screen is missed.** WhatsApp shows no notification for the chat you are looking at, so Argus never sees those messages.
- **Android 15 and newer may hide codes in notifications.** Android can blank out one-time codes in notifications for apps like Argus, so a WhatsApp message asking for a code may arrive with the code hidden. SMS are not affected.
- **It needs a capable phone.** 64-bit only, and at least 3 GB of RAM (4 GB or more recommended). It explains this kindly on phones that can't run it.
- **Some phones kill background apps.** Tecno, Infinix, itel and Xiaomi need Auto-start turned on (the wizard shows how).
- **The family alert costs an SMS** at the parent's normal rate and fails without airtime (the home screen then says so).
- **It can be wrong.** It never says a message is safe; when in doubt, call your family.

## Build from source

You need Android Studio (or JDK 17+ and the Android SDK with API 37), and the Gemma model file.

1. **Get the model.** On Hugging Face, accept the Gemma licence on [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT) and download the CPU/GPU `.litertlm` file (int4, about 557 MB). Model files are never committed to this repo.
2. **Pick a flavour:**
   - `dev` carries no model. Push it once with
     `adb push gemma3-1b-it-int4.litertlm /sdcard/Android/data/com.ajayinnovations.argus/files/model.litertlm`
     and build `./gradlew assembleDevDebug`. The dev build adds a "Argus Dev" debug screen.
   - `standard` bundles the model: copy it to `app/src/standard/assets/model.litertlm`.
3. **Release signing.** Create a keystore (keep it and its password safe forever; every update must be signed with it):

   ```bash
   keytool -genkeypair -v -keystore ~/keystores/argus-release.jks -storetype PKCS12 \
     -alias argus -keyalg RSA -keysize 4096 -validity 10000
   ```

   Then create `keystore.properties` in the project root (it is git-ignored):

   ```properties
   storeFile=/Users/you/keystores/argus-release.jks
   storePassword=…
   keyAlias=argus
   keyPassword=…
   ```

4. **Build:** `./gradlew assembleStandardRelease`. The APK is in `app/build/outputs/apk/standard/release/`.
5. **Tests:** `./gradlew testDevDebugUnitTest` runs the unit tests for the whole scam engine.

Stack: Kotlin, Jetpack Compose, Material 3, coroutines, Koin, and [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) 0.17.1 for on-device Gemma. The full design and every decision are in [docs/PLAN.md](docs/PLAN.md).

## Credits and licences

- **Based on [ScamGuard](https://github.com/abgolor/ScamGuard)** by Abraham Golor, used and modified with the author's permission. See [NOTICE](NOTICE).
- **Gemma** by Google. Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms. The full terms, the Prohibited Use Policy and the required notice are in [NOTICE](NOTICE) and in the app under About.
- **Voice by ElevenLabs.** The warning clips were generated once with ElevenLabs and ship inside the app; no voice service is called at run time.
- **Atkinson Hyperlegible Next** font by the Braille Institute, under the SIL Open Font License 1.1 (see NOTICE).

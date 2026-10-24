# Argus releases

> The v1.0.0 notes below describe the upstream ScamGuard 1.0.0 release this code is based on (see [NOTICE](../NOTICE)), updated for Argus's English and Hindi. Argus has not published its own APK yet; checksums and the signing certificate will be added with the first Argus release.

## v1.0.0 (October 4, 2026)

The first public release: **Argus Standard** with Gemma 3 1B inside, for Android 8 and newer, 64-bit phones.

**Download:** `Argus-1.0.0.apk` (612 MB). Install it on the parent's phone, open it and follow the setup wizard. See the [README](../README.md#install-it-on-your-parents-phone) for the steps, including Android 13's "Restricted setting".

### What it does

- **Checks every SMS and WhatsApp message automatically**, with the app closed and the phone locked. No need to open anything.
- **AI on the phone.** Gemma 3 1B runs on the phone's processor. There is no internet permission: no message can ever leave the phone, and nothing is stored.
- **Instant hard rules** for the clearest scams ("your BVN is blocked" + a link, "send me the OTP", "new number, send money", prizes with a fee): the red warning comes in about 2 seconds.
- **A guarded Gemma check** for everything else, with fixed, plain reasons ("It has a link and says your account is blocked.") and a safety floor that no trick inside a message can switch off.
- **Red warning over other apps (optional):** allow "Display over other apps" and a scam warning covers the screen even while she's using the phone. It shows the whole message (scrollable, dangerous parts highlighted, links that can't be tapped), stays until "I understand", repeats the voice once after 2 minutes, and shows "2 warnings" instead of stacking screens.
- **Warnings she can't miss:** a full-screen red warning, even over the lock screen, a spoken warning by ElevenLabs voices (Mummy, Daddy, Grandma or neutral), and strong vibration even on silent. Quieter notes for "be careful" and for real bank OTPs.
- **"Call [name]"** opens the dialer with the family member's number.
- **Optional family SMS** when a scam is flagged. It never includes the message or the sender.
- **Share to check, clipboard check and a paste box** for any message she isn't sure about.
- **English and Hindi** throughout, large text (18sp and up), big buttons, one main action per screen.
- **A setup wizard** for the son or daughter: phone check, model preparation with progress, profile, every permission with an explanation, phone-maker battery steps (Tecno, Infinix, itel, Xiaomi), and a real test alert.
- **A home screen that says when something is off** (a permission revoked, WhatsApp checking disconnected, the AI model broken) with one "Fix it" button each.

### Hardening in this release

- **Busy groups.** Every message in a burst is checked, even when Android drops some notification updates. A hard-rule scam in a burst alerts at once instead of waiting behind the AI checks; on the Pixel 8 that went from about 4.5 minutes to under a second in a 30-message burst.
- **A broken or missing model never crashes the app.** The model file's header is checked before loading (a corrupt file used to crash the app inside LiteRT-LM), and a broken weight cache is rebuilt once. Without a working model, the hard rules and the safety floor still protect, and the home screen offers to set the model up again.
- **The check queue can't stall.** A stray error or a stuck model call no longer stops later messages from being checked; after 90 seconds the rules decide alone.
- **Long and non-Latin messages:** emoji are never split, and the model's input is budgeted so long messages in other scripts still fit.
- **Device-to-device transfer is blocked** for the app's settings and model copy (Android 12+), as well as cloud backup.
- **Fewer "be careful" notes:** the AI's caution needs something concrete (a link, a number, a money or code request, a "new number"), and a busy chat gets at most one spoken caution every 10 minutes.
- **Three more scam rules:** "reply with the code", investment schemes promising returns, and "call this number" about account trouble.
- **Smaller download:** only ARM phone libraries are included.
- **R8** shrinks and optimises the release build, with keep rules for LiteRT-LM's native code.

### Requirements

- Android 8.0 (API 26) or newer, **64-bit (arm64)**.
- RAM: 4 GB or more recommended. Phones with less than about 3 GB can't run it, and the wizard says so kindly.
- Storage: about **2.3 GB free during setup** (the downloaded file, the installed app, the model copy and its cache), about **1.7 GB** after. The downloaded APK can be deleted afterwards.

### Known limitations

- Messages only, not phone calls. Photos and voice notes aren't checked.
- A WhatsApp chat that is open on screen is missed (WhatsApp shows no notification for it).
- Android 15+ may hide one-time codes in notifications from Argus.
- It can be wrong: busy group chats may get some quiet "be careful" notes, and a cleverly worded scam can be missed. It never says a message is safe.

### Licences

Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms. The full Terms, the Gemma Prohibited Use Policy and the use restrictions that apply to everyone using Argus are in [NOTICE](../NOTICE) and in the app under About. Voice by ElevenLabs. Atkinson Hyperlegible Next under the SIL Open Font License 1.1.

### Checksums

Added with the first Argus release build.

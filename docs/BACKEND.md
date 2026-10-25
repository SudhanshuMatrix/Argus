# Live scam alerts: backend and Firebase setup

Live alerts let you warn every Argus phone at once. You type a scam message on a web page (or send
it with `curl`); every **Argus Online** and **Argus Demo** phone shows it within seconds as the red
full-screen warning, also over the lock screen, and speaks it.

```
you ──HTTPS + username/password──▶ argus-server (Go, backend/) ──FCM HTTP v1──▶ topic "argus-alerts" ──▶ every phone
```

- The phones only **receive**. They never send any message text anywhere.
- **Argus Offline** has no internet permission and never gets live alerts.
- Firebase Cloud Messaging is free for this use.

You need to do four things once: create the Firebase project, add its settings to GitHub, run the
server, and send a test alert.

## 1. Create the Firebase project

1. Go to <https://console.firebase.google.com>, click **Create a project**, and name it e.g. `argus`
   (Google Analytics isn't needed).
2. In the project, **Add app → Android**, twice:
   - package name `com.sudhanshumatrix.argus.online` (the Online APK)
   - package name `com.sudhanshumatrix.argus.demo` (the Demo APK)

   You don't need to download `google-services.json` or change any Gradle files: Argus passes the
   settings in at build time instead.
3. Open **Project settings → General** and note, for each of the two apps:
   - **Web API Key** (one for the project)
   - **Project ID**
   - **Project number** (this is the *sender ID*)
   - each app's **App ID** (looks like `1:1234567890:android:abc123…`)
4. Open **Project settings → Service accounts → Generate new private key**. This downloads a JSON
   file. It is the server's password to Firebase, so keep it secret and never commit it.

## 2. Add the settings to GitHub

Repository → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
| --- | --- |
| `ARGUS_FIREBASE_API_KEY` | the Web API Key |
| `ARGUS_FIREBASE_PROJECT_ID` | the Project ID |
| `ARGUS_FIREBASE_SENDER_ID` | the Project number |
| `ARGUS_FIREBASE_APP_ID_ONLINE` | App ID of `com.sudhanshumatrix.argus.online` |
| `ARGUS_FIREBASE_APP_ID_DEMO` | App ID of `com.sudhanshumatrix.argus.demo` |

Then push to `main` (or re-run the **Build APK** workflow). The Online and Demo APKs now listen for
live alerts. On the phone, Home shows **Live scam alerts: On**.

Other secrets the workflow uses:

| Secret | What for |
| --- | --- |
| `HF_TOKEN` | A Hugging Face token from an account that accepted the Gemma licence on [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT). CI downloads Gemma into the Offline APK and publishes it on the `model-gemma3-1b` release, where the Online APK downloads it. |
| `ARGUS_KEYSTORE_BASE64`, `ARGUS_KEYSTORE_PASSWORD`, `ARGUS_KEY_ALIAS`, `ARGUS_KEY_PASSWORD` | Your release signing key, so new APKs install over old ones. |

The Online APK downloads the model from this repository's release, so that only works while the
repository is **public**. For a private repository, host the `.litertlm` file somewhere else and set
the repository **variable** `ARGUS_MODEL_URL` to its address.

## 3. Run the server

The server is one small Go program (`backend/`, standard library only). It reads everything from
environment variables, so no password is ever in the code:

| Variable | Value |
| --- | --- |
| `ARGUS_ADMIN_USERNAME` | the username for sending alerts, e.g. `sudhanshu` |
| `ARGUS_ADMIN_PASSWORD` | your password (at least 8 characters). Use a new one you haven't shared anywhere. |
| `ARGUS_FCM_SERVICE_ACCOUNT` | the **whole content** of the service-account JSON from step 1.4 (or `ARGUS_FCM_SERVICE_ACCOUNT_FILE` with its path) |
| `PORT` | set by most hosts; defaults to 8080 |

Five wrong passwords from one address lock it out for 10 minutes.

### Option A: Google Cloud Run (same Google account as Firebase)

```sh
gcloud run deploy argus-server --source backend --region asia-south1 --allow-unauthenticated \
  --set-env-vars ARGUS_ADMIN_USERNAME=sudhanshu \
  --set-env-vars ARGUS_ADMIN_PASSWORD='your-password' \
  --set-env-vars ARGUS_FCM_SERVICE_ACCOUNT="$(cat argus-firebase-adminsdk.json)"
```

`--allow-unauthenticated` only opens the web page; sending still needs your username and
password. Cloud Run prints the server's `https://…run.app` address. For production, keep the
password and the JSON in Secret Manager (`--set-secrets`) instead of plain variables.

### Option B: any server with Docker

```sh
docker build -t argus-server backend/
docker run -d -p 8080:8080 \
  -e ARGUS_ADMIN_USERNAME=sudhanshu \
  -e ARGUS_ADMIN_PASSWORD='your-password' \
  -e ARGUS_FCM_SERVICE_ACCOUNT="$(cat argus-firebase-adminsdk.json)" \
  argus-server
```

Put it behind HTTPS (Caddy, nginx or your host's load balancer): the password travels with every
request.

### Option C: on your computer, for a quick test

```sh
cd backend
ARGUS_ADMIN_USERNAME=sudhanshu ARGUS_ADMIN_PASSWORD='your-password' \
ARGUS_FCM_SERVICE_ACCOUNT_FILE=~/Downloads/argus-firebase-adminsdk.json go run .
```

## 4. Send an alert

**From a browser:** open the server's address (for example `https://argus-server-xyz.a.run.app`),
enter the username, password and the scam message, and tap **Send to every phone**.

**From a terminal:**

```sh
curl -u sudhanshu:'your-password' -H 'Content-Type: application/json' \
  -d '{"message":"Dear customer, your SBI account is blocked. Click bit.ly/sbi-kyc-update to update your KYC."}' \
  https://YOUR-SERVER/api/alerts
```

The reply is `{"ok":true,"id":"…"}`. Messages can be up to 1000 characters, in English or Hindi.

What the phone shows: **"Scam alert from Argus"**, the message with its dangerous parts marked, why
it's a scam, and what to do, in the phone's language, with the spoken warning.

## Troubleshooting

- **Home says "Live scam alerts: Not set up in this build":** the Firebase secrets were missing when
  that APK was built. Add them and rebuild.
- **The server replies 502:** the service-account JSON is wrong, or it belongs to another Firebase
  project. The server log says which.
- **Nothing arrives on a phone:** open Argus once after installing (that subscribes the phone), and
  allow notifications. Xiaomi, Tecno, Infinix and itel phones also need the battery steps from setup.

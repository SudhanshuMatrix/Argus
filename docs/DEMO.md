# Recording a demo

Every message below is checked by `test/app/demo_messages_test.dart`, so it gives the result
listed. All are made up: no real bank, person, number or link.

## Option 1: the Demo APK and live alerts (simplest)

The **Argus Demo** APK only asks for notifications and the full-screen warning (no SMS, no
WhatsApp reading), so setup takes a minute. You trigger the warnings from the Argus server.

1. Install `Argus-Demo.apk`, open it, and finish setup: who it's for, the language, **Allow
   warnings**, and on Android 14+ the full-screen warning.
2. Lock the phone and start recording.
3. On your laptop or another phone, open the Argus server page, enter your username and
   password, paste a scam message from the table below, and tap **Send to every phone**
   (or use `curl`; see [BACKEND.md](BACKEND.md#4-send-an-alert)).
4. The phone lights up with the red **"Scam alert from Argus"** warning over the lock screen,
   vibrates and speaks. Unlock to show the marked message, the reason and the **Call** button.

Switch the language to हिन्दी (Settings) to show the same warning in Hindi.

## Option 2: real messages (Online or Offline APK)

Send the messages below as SMS or WhatsApp messages from a second phone to the phone running
Argus Online or Offline, or paste them into **Check a message**. Home → **Send a test scam**
also works without a second phone.

## The messages

| # | Message | What Argus does |
| --- | --- | --- |
| 1 | Dear customer, your SBI account is blocked due to pending KYC. Click bit.ly/sbi-kyc-update to verify now. | 🔴 Scam: "It has a link and says your account is blocked." |
| 2 | Hello sir, I am calling from your bank. Please send me the OTP you just received so we can stop the fraud on your account. | 🔴 Scam: asks for your OTP |
| 3 | Mummy this is Rahul, my phone fell in water so I am using my friend's phone. Please send money urgently, 5000 rupees for the hospital. | 🔴 Scam: new number asking for money |
| 4 | Congratulations! You have won Rs 25,00,000 in the KBC lucky draw. Pay the processing fee of Rs 4,999 today to claim your prize. | 🔴 Scam: prize that needs a fee |
| 5 | Invest 10k today in our crypto plan and get 30k back in one week. Send money to the account below. | 🔴 Scam: promised investment return |
| 6 | आपका बैंक खाता आज ब्लॉक हो जाएगा। केवाईसी अपडेट करने के लिए इस लिंक पर क्लिक करें: bit.ly/kyc-sbi | 🔴 Scam (Hindi): blocked account and a link |
| 7 | SBI से बोल रहे हैं। आपके फोन पर आया OTP बताइए, वरना आपका खाता बंद हो जाएगा। | 🔴 Scam (Hindi): asks for your OTP |
| 8 | Hi Mummy, ye mera naya number hai. Mera phone kho gaya. Jaldi se 5000 rupaye bhej do, urgent hai. | 🔴 Scam (Hinglish): new number asking for money |
| 9 | Dear consumer, your electricity power will be disconnected tonight at 9:30 pm. Please contact our electricity officer immediately on 9876543210. | 🔴 Scam: fake electricity officer |
| 10 | This is CBI. Your Aadhaar is linked to a money laundering case and a warrant is issued. Send ₹50,000 to the RBI safe account now to avoid arrest. | 🔴 Scam: fake police "digital arrest" |
| 11 | Good evening ma'am, my cousin's crypto group helps members double your money in 10 days. Should I add you? | 🟠 Be careful (quiet note) |
| 12 | Your OTP is 482913. It is valid for 5 minutes. Do not share this code with anyone. | 🟢 Secret-code reminder |
| 13 | Good morning Mummy, we will visit you on Sunday after lunch. Love you! | Nothing: normal messages stay silent |

With live alerts (option 1) every message shows the red scam warning, because it comes from you
as a warning. With real messages (option 2) Argus decides, as in the table.

## Tips

- Turn the volume up. On silent the voice stays quiet unless **Speak warnings even when the phone
  is on silent** is on; the red screen and vibration still show.
- With family alerts on, real scams (option 2) also send the family SMS: use your own second phone's number.

# Demo messages

Messages to record a demo with. Every one is checked by `test/app/demo_messages_test.dart`,
so each gives the result listed here. All are made up: no real bank, person or link.

**How to show them**

- **Live (best for video):** send them as SMS or WhatsApp messages from a second phone to the
  phone running Argus. Lock the protected phone first to show the red warning over the lock screen.
- **Check screen:** open Argus → **Check a message**, paste one, tap **Check**.
- **Built-in test:** Home → **Send a test scam**.

Switch the language in **Change settings** to show the same warnings in Hindi.

| # | Send this | What Argus does |
| --- | --- | --- |
| 1 | Dear customer, your SBI account is blocked due to pending KYC. Click bit.ly/sbi-kyc-update to verify now. | 🔴 **Scam**: red full-screen warning, voice, vibration. "It has a link and says your account is blocked." |
| 2 | Hello sir, I am calling from your bank. Please send me the OTP you just received so we can stop the fraud on your account. | 🔴 **Scam**: "It asks for your OTP…" |
| 3 | Mummy this is Rahul, my phone fell in water so I am using my friend's phone. Please send money urgently, 5000 rupees for the hospital. | 🔴 **Scam**: "It says it is from a new number and asks you to send money." |
| 4 | Congratulations! You have won Rs 25,00,000 in the KBC lucky draw. Pay the processing fee of Rs 4,999 today to claim your prize. | 🔴 **Scam**: prize that needs a fee |
| 5 | Invest 10k today in our crypto plan and get 30k back in one week. Send money to the account below. | 🔴 **Scam**: promised investment return |
| 6 | Good evening ma'am, my cousin's crypto group helps members double your money in 10 days. Should I add you? | 🟠 **Be careful** note (quiet notification) |
| 7 | Your OTP is 482913. It is valid for 5 minutes. Do not share this code with anyone. | 🟢 **Secret code** reminder: "Never share this code with anyone" |
| 8 | Good morning Mummy, we will visit you on Sunday after lunch. Love you! | Nothing: normal messages stay silent |

**Tips for the recording**

- Do the setup first (including WhatsApp checking and "Send a test scam"), and turn the volume up.
- If family alerts are on, scams also send the family SMS. Use your own second phone's number.
- With the phone on silent the voice stays quiet unless **Speak warnings even when the phone is on
  silent** is on in Settings. The red screen and vibration still show.

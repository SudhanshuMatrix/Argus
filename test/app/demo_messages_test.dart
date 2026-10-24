import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/verdict.dart';
import 'package:flutter_test/flutter_test.dart';

/// The messages in docs/DEMO.md, for recording a demo. Each must give the level the doc promises.
const demo = <(String, Level)>[
  ('Dear customer, your SBI account is blocked due to pending KYC. Click bit.ly/sbi-kyc-update to verify now.', Level.scam),
  ('Hello sir, I am calling from your bank. Please send me the OTP you just received so we can stop the fraud on your account.', Level.scam),
  ("Mummy this is Rahul, my phone fell in water so I am using my friend's phone. Please send money urgently, 5000 rupees for the hospital.", Level.scam),
  ('Congratulations! You have won Rs 25,00,000 in the KBC lucky draw. Pay the processing fee of Rs 4,999 today to claim your prize.', Level.scam),
  ('Invest 10k today in our crypto plan and get 30k back in one week. Send money to the account below.', Level.scam),
  ("Good evening ma'am, my cousin's crypto group helps members double your money in 10 days. Should I add you?", Level.careful),
  ('Your OTP is 482913. It is valid for 5 minutes. Do not share this code with anyone.', Level.otp),
  ('Good morning Mummy, we will visit you on Sunday after lunch. Love you!', Level.none),
];

void main() {
  for (final (text, level) in demo) {
    test('${level.name}: ${text.substring(0, 40)}', () => expect(ScamChecker.check(text).level, level));
  }
}

import 'package:argus/core/concrete_signs.dart';
import 'package:argus/core/findings.dart';
import 'package:argus/core/hard_rules.dart';
import 'package:argus/core/highlights.dart';
import 'package:argus/core/otp_check.dart';
import 'package:argus/core/red_flags.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/verdict.dart';
import 'package:flutter_test/flutter_test.dart';

/// Synthetic Indian scams in English, Hinglish and Hindi. Every bank, number and link is made up.
const indianScams = <(String, HardRule)>[
  ('Dear customer, your SBI YONO account will be blocked today. Update your PAN card now: http://sbi-kyc-update.in', HardRule.linkAccount),
  ('Dear consumer, your electricity power will be disconnected tonight at 9:30 pm because your last bill was not updated. Please contact our electricity officer immediately on 9876543210.', HardRule.callOfficial),
  ('This is CBI. Your Aadhaar is linked to a money laundering case and a warrant is issued. Send ₹50,000 to the RBI safe account now to avoid arrest.', HardRule.officialThreatMoney),
  ('Hi Mummy, ye mera naya number hai. Mera phone kho gaya. Jaldi se 5000 rupaye bhej do, urgent hai.', HardRule.moneyNewNumber),
  ('मम्मी, यह मेरा नया नंबर है। मेरा फोन खो गया। जल्दी से 5000 रुपये भेज दो।', HardRule.moneyNewNumber),
  ('आपका बैंक खाता ब्लॉक हो जाएगा। केवाईसी अपडेट करने के लिए इस लिंक पर क्लिक करें: bit.ly/kyc-sbi', HardRule.linkAccount),
  ('SBI से बोल रहे हैं। आपके फोन पर आया OTP बताइए, वरना आपका खाता बंद हो जाएगा।', HardRule.codeRequest),
  ('Bank se bol raha hoon, aapke phone par jo OTP aaya hai woh jaldi bata do.', HardRule.codeRequest),
  ('Congratulations! Aapne KBC lottery mein 25 lakh jeete hain. Registration fee ₹4,999 jama karein aur prize claim karein.', HardRule.prizeFee),
  ('बधाई हो! आपने 25 लाख रुपये की लॉटरी जीती है। इनाम पाने के लिए 4,999 रुपये शुल्क जमा करें।', HardRule.prizeFee),
  ('Your UPI PIN is required to receive ₹2,000 cashback. Share your UPI PIN with our agent to get the money.', HardRule.codeRequest),
  ('Invest ₹10,000 in our trading group and get ₹30,000 in 7 days. Paisa double guaranteed! Send to UPI id trade@okaxis', HardRule.investmentMoney),
  ('FedEx: your parcel from Mumbai has illegal items and is seized by customs. Call 9123456780 now to avoid a police case.', HardRule.callOfficial),
];

/// Real-looking normal Indian messages that must never become a loud SCAM.
const indianNormals = [
  'Rs 5,000.00 credited to your A/c XX1234 on 24-10-26 by UPI ref 432198765012. Avl Bal Rs 12,345.67 -SBI',
  'You have paid Rs 250.00 to Sharma Kirana via UPI. UPI Ref No 987654321012.',
  'Dear customer, your electricity bill of Rs 845 for October is due on 5 Nov. Pay on the app to avoid a late fee.',
  'Beta, kal subah 10 baje doctor ke paas chalna hai. Gaadi le aana.',
  'माँ, मैं रविवार को घर आ रहा हूँ। खाना बना कर रखना।',
  'Your Amazon order will be delivered today by 8 PM. Track it in the app.',
  'Congratulations on your promotion beta! Bahut khushi hui, mithai lekar aana.',
  'Papa, maine aapke account mein 2000 rupaye bhej diye hain. Dawai le lena.',
  'Your KYC has been updated successfully. Thank you for banking with us.',
  'मेरा नया नंबर सेव कर लो, पुराना वाला बंद हो गया है। रविवार को मिलते हैं।',
];

void main() {
  group('Indian scams hit a hard rule', () {
    for (final (text, rule) in indianScams) {
      test('${rule.name}: ${text.substring(0, 30)}', () {
        expect(HardRules.match(text), rule);
        expect(ScamChecker.check(text).level, Level.scam);
      });
    }
  });

  group('normal Indian messages stay quiet', () {
    for (final text in indianNormals) {
      test(text.substring(0, 30), () {
        expect(HardRules.match(text), isNull);
        expect(ScamChecker.check(text).level, isNot(anyOf(Level.scam, Level.careful)));
      });
    }
  });

  group('Indian OTPs', () {
    test('real OTPs in English and Hindi give the calm reminder', () {
      for (final t in [
        'आपका OTP 482913 है। इसे किसी के साथ साझा न करें। - SBI',
        'Your OTP for login is 4821. Do not share it with anyone. -HDFC Bank',
        '482913 आपका ओटीपी है। इसे किसी को न बताएं।',
      ]) {
        expect(OtpCheck.isRealOtp(t), isTrue, reason: t);
        expect(ScamChecker.check(t).level, Level.otp, reason: t);
      }
    });

    test('Hindi "do not tell" is not a request, Hindi "tell me" is', () {
      expect(HardRules.match('यह OTP किसी को न बताएं, बैंक कभी नहीं पूछता।'), isNull);
      expect(HardRules.match('यह OTP मुझे बताइए'), HardRule.codeRequest);
      expect(HardRules.match('OTP kisi ko mat batana'), isNull);
    });
  });

  group('Indian numbers and amounts', () {
    test('Indian mobile numbers are phone numbers; long reference numbers are not', () {
      for (final n in ['9876543210', '98765 43210', '+91 98765 43210', '09876543210']) {
        expect(RedFlags.find('call $n'), contains('phone number'), reason: n);
      }
      expect(RedFlags.find('UPI Ref No 987654321012'), isNot(contains('phone number')));
      expect(RedFlags.find('Aadhaar 2345 6789 0123'), isNot(contains('phone number')));
    });

    test('rupee amounts count as money', () {
      for (final t in ['send ₹5,000', 'pay Rs. 499', 'transfer INR 2000', 'bhej do 5 lakh', 'send 300 rupees', '5000 रुपये भेजो']) {
        expect(ConcreteSigns.find(t), contains(Sign.moneyRequest), reason: t);
      }
    });

    test('highlights mark rupees and Indian numbers', () {
      const text = 'Pay ₹4,999 now or call 98765 43210';
      final marks = Highlights.find(text).map((m) => text.substring(m.start, m.end)).toList();
      expect(marks, containsAll(['₹4,999', '98765 43210']));
    });
  });

  group('findings name Indian secrets', () {
    test('UPI PIN and Hindi OTP', () {
      expect(Findings.find('Share your UPI PIN with our agent').first, const Finding.asksForSecret(Secret.upiPin));
      expect(Findings.find('आपके फोन पर आया OTP बताइए').first, const Finding.asksForSecret(Secret.otp));
    });

    test('Hindi blocked account', () {
      expect(Findings.find('आपका खाता ब्लॉक हो जाएगा, bit.ly/x पर क्लिक करें'), contains(const Finding.accountProblem(AccountTrouble.blocked)));
    });
  });
}

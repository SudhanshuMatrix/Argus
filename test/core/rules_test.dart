import 'package:argus/core/concrete_signs.dart';
import 'package:argus/core/findings.dart';
import 'package:argus/core/hard_rules.dart';
import 'package:argus/core/otp_check.dart';
import 'package:argus/core/red_flags.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/skip_filter.dart';
import 'package:argus/core/verdict.dart';
import 'package:flutter_test/flutter_test.dart';

import 'samples.dart';

void main() {
  group('HardRules', () {
    const bvnBlocked = 'Dear customer, your BVN is blocked, click bit.ly/x1abc to update today or your account will be closed';

    test('the BVN blocked link is a hard SCAM', () => expect(HardRules.match(bvnBlocked), HardRule.linkAccount));

    test('a link plus each account word', () {
      for (final w in ['BVN', 'NIN', 'blocked', 'restricted', 'suspended', 'verify', 'update your account']) {
        expect(HardRules.match('Notice: $w here https://x.co/a now'), HardRule.linkAccount, reason: w);
      }
    });

    test('account words without a link are not a hard rule', () => expect(HardRules.match('Your BVN is blocked, visit any branch'), isNull));

    test('a link without account words is not a hard rule', () => expect(HardRules.match('See the wedding photos here https://photos.app.goo.gl/abc'), isNull));

    test('requests to hand over a secret', () {
      for (final t in [
        'Please send me the OTP you just received',
        'Share your BVN with our agent',
        'Kindly forward the 6-digit code sent to your phone',
        'Give me your ATM PIN so I can help',
        'Tell me your password',
        'Abeg send the code wey dem send you',
      ]) {
        expect(HardRules.match(t), HardRule.codeRequest, reason: t);
      }
    });

    test('do not share warnings are not requests', () {
      for (final t in [
        'Your OTP is 482913. Do not share this code with anyone.',
        'Never give your PIN to anybody, not even bank staff.',
        "Don't send your password to anyone.",
      ]) {
        expect(HardRules.match(t), isNull, reason: t);
      }
    });

    test('the checker decides at once', () {
      final result = ScamChecker.check(bvnBlocked);
      expect(result.level, Level.scam);
      expect(result.decidedBy, DecidedBy.hardRule);
      expect(result.verdict.reasonCode, ReasonCode.accountProblem);
    });

    test('a code request is SCAM', () {
      final result = ScamChecker.check('Mummy please send me the OTP the bank sent you now');
      expect(result.level, Level.scam);
      expect(result.verdict.reasonCode, ReasonCode.asksForCode);
    });

    test('a genuine OTP stays OTP', () => expect(ScamChecker.check('Your OTP is 482913. Do not share this code with anyone.').level, Level.otp));

    test('money from a new number', () {
      for (final t in [
        'Mummy this is Tunde, I am using my friend line. Please send 50,000 naira urgently',
        "Hello ma, it's me, this is my new line. Kindly transfer N20,000 for my school fees",
        "Daddy my phone fell in water, I'm on my friend's phone. Send 15k for transport",
        'I lost my phone, this is my new number. Please send money, N10,000',
      ]) {
        expect(HardRules.match(t), HardRule.moneyNewNumber, reason: t);
      }
    });

    test('a new number alone, or money alone, is not a hard rule', () {
      expect(HardRules.match('Hi mummy, this is my new number, save it. See you on Sunday'), isNull);
      expect(HardRules.match('Please send the 5,000 naira contribution to the treasurer by Friday'), isNull);
    });

    test('prize plus a fee or payment', () {
      for (final t in [
        'Congratulations! You have won 2,000,000 naira in the MTN promo. Pay 5,000 processing fee to claim.',
        'You have won a new fridge! Send N3,500 delivery charge to receive it today',
        'MTN PROMO: your number won a prize. Transfer the redemption fee of N2,000 to 0123456789',
        'Congrats o! Congratulations, you don win prize. Pay small charge make we send am',
      ]) {
        expect(HardRules.match(t), HardRule.prizeFee, reason: t);
      }
    });

    test('congratulations or a promo without a fee is not a hard rule', () {
      expect(HardRules.match('Congratulations on your new baby! God bless the family'), isNull);
      expect(HardRules.match('Shoprite promo this weekend: 20% off rice and oil'), isNull);
    });

    test('no normal sample hits a hard rule', () {
      for (final s in normals) {
        expect(HardRules.match(s.text), isNull, reason: s.name);
      }
    });

    test('most scam samples hit a hard rule', () {
      final hits = scams.where((s) => HardRules.match(s.text) != null).map((s) => s.name).toList();
      expect(hits.length, greaterThanOrEqualTo(4), reason: '$hits');
    });
  });

  group('new scam rules', () {
    test('reply with the code is a code request and a hard rule', () {
      expect(HardRules.match('Your Airtel line is due for upgrade. Kindly reply with the 4 digit code sent to you.'), HardRule.codeRequest);
      expect(HardRules.match('Abeg reply this message with the PIN wey dem send you'), HardRule.codeRequest);
      expect(HardRules.match('Please send me the code that just came to your phone'), HardRule.codeRequest);
    });

    test('enter or type the code counts as a code request for the guard', () {
      const t = 'Go to the page and type the code we sent you to receive your reward';
      expect(ConcreteSigns.find(t), contains(Sign.codeRequest));
      expect(ConcreteSigns.find('Enter the OTP on the form so we can release your money'), contains(Sign.codeRequest));
      expect(ConcreteSigns.guardScam(ConcreteSigns.find(t)).level, Level.scam);
    });

    test('real codes that say enter it in the app stay an OTP reminder', () {
      expect(ScamChecker.check('Your verification code is 552901. Enter it in the app to log in. Do not share this code with anyone.').level, Level.otp);
    });

    test('do not reply with the code is a warning, not a request', () {
      expect(HardRules.match('Never reply to anyone with this code, even if they say they are from the bank. Code 4812'), isNull);
    });

    test('an estate gate code is not a loud alarm', () {
      expect(ScamChecker.check('Visitor pass for Saturday: enter code 4421 at the estate gate, the guard will let you in.').level, isNot(Level.scam));
    });

    test('promised returns are an investment sign', () {
      for (final t in [
        'Put 20k in our pool and withdraw 60k by Friday',
        'Join us and double your money in two weeks',
        'Guaranteed profit every month, no risk at all',
        'Our plan pays 15% weekly profit to every member',
        r'I invested $200 in the trading group and got $900 back',
      ]) {
        expect(ConcreteSigns.find(t), contains(Sign.investmentReturn), reason: t);
      }
    });

    test('promised return with a money request is a hard-rule scam', () {
      const t = 'Invest N30,000 with our forex manager this week and receive N95,000 next Monday. Send to the account below.';
      expect(HardRules.match(t), HardRule.investmentMoney);
      expect(ScamChecker.check(t).level, Level.scam);
      expect(ScamChecker.check(t).decidedBy, DecidedBy.hardRule);
    });

    test('promised return alone is never silent', () {
      final result = ScamChecker.check("Good evening ma, my cousin's crypto group helps members double your money in 10 days. Should I add you?");
      expect(result.level, Level.careful);
      expect(result.verdict.reasonCode, ReasonCode.tooGoodOffer);
    });

    test('ordinary savings, prices and bank interest stay quiet', () {
      for (final t in [
        'I put the 50k in the cooperative account like you said. The meeting is next Sunday.',
        'Tomatoes are 18k a basket now, I bought half and got change of 2k.',
        'Your fixed deposit of N500,000 has matured. Interest of N12,500 has been credited.',
      ]) {
        expect(ConcreteSigns.find(t), isNot(contains(Sign.investmentReturn)), reason: t);
        expect(ScamChecker.check(t).level, Level.none, reason: t);
      }
    });

    test('call a number about account trouble is at least CAREFUL', () {
      const t = 'We noticed suspicious logins on your profile. Call 08097771234 now to stop it.';
      expect(ConcreteSigns.find(t), contains(Sign.callAboutAccount));
      expect(ScamChecker.check(t).level, Level.careful);
    });

    test('call about account trouble from a claimed bank is a hard-rule scam', () {
      const t = 'Dear valued customer, your ATM card has been blocked due to fraud. Call our customer care on 07012345678 to reactivate.';
      expect(HardRules.match(t), HardRule.callOfficial);
      expect(ScamChecker.check(t).level, Level.scam);
      expect(HardRules.match('Na fraud desk be dis. Your account don block. Call 09123456780 sharp sharp'), HardRule.callOfficial);
    });

    test('a real bank message asking you to call the number on your card stays quiet', () {
      for (final t in [
        'Dear Customer, a debit of N5,000 occurred on your account. If you did not authorise it, call the number on the back of your card.',
        'Acct 0123***89 credited with NGN20,000. For enquiries call 07001234567. Thank you for banking with us.',
        'Dear customer, your new debit card is ready for pickup at our Ikeja branch. Call 08031112222 if you have questions.',
      ]) {
        expect(HardRules.match(t), isNull, reason: t);
        expect(ConcreteSigns.find(t), isNot(contains(Sign.callAboutAccount)), reason: t);
        expect(ScamChecker.check(t).level, isNot(Level.scam), reason: t);
      }
    });

    test('the normal samples get no investment or call-about-account sign', () {
      for (final s in normals) {
        final signs = ConcreteSigns.find(s.text);
        expect(signs, isNot(contains(Sign.investmentReturn)), reason: s.name);
        expect(signs, isNot(contains(Sign.callAboutAccount)), reason: s.name);
      }
    });
  });

  group('RedFlags', () {
    test('keywords use word boundaries', () {
      expect(RedFlags.find('I went shopping for spinach'), isNot(contains('pin')));
      expect(RedFlags.find('Good morning, dinner is ready'), isNot(contains('nin')));
      expect(RedFlags.find('The loans officer phoned'), isNot(contains('loan')));
      expect(RedFlags.find('Send your PIN now'), contains('pin'));
    });

    test('keywords ignore case', () {
      expect(RedFlags.find('Your BVN is BLOCKED'), containsAll(['bvn', 'blocked']));
    });

    test('multi-word phrases match', () {
      expect(RedFlags.find('Hello, you have won a car'), contains('you have won'));
      expect(RedFlags.find('Call Customer Care today'), contains('customer care'));
    });

    test('links are found', () {
      for (final t in ['click https://x.co', 'go to www.example.com', 'bit.ly/xyz123', 'wa.me/2348011112222', 'tinyurl.com/abc']) {
        expect(RedFlags.find(t), contains('link'), reason: t);
      }
    });

    test('Nigerian phone numbers are found with or without spaces', () {
      for (final n in ['0801 234 5678', '08012345678', '+2348012345678', '0905-123-4567']) {
        expect(RedFlags.find('call $n'), contains('phone number'), reason: n);
      }
    });

    test('account numbers and prices are not phone numbers', () {
      expect(RedFlags.find('Send N5,000 to 2087654321'), isNot(contains('phone number')));
      expect(RedFlags.find('Bal: NGN84,250.50'), isNot(contains('phone number')));
    });

    test('a normal message has no flags', () => expect(RedFlags.find('Mummy good morning. I will come on Saturday.'), isEmpty));
  });

  group('ConcreteSigns', () {
    test('finds links and phone numbers through RedFlags', () {
      expect(ConcreteSigns.find('Update at bit.ly/xyz123'), contains(Sign.link));
      expect(ConcreteSigns.find('Call 0803 456 7890'), contains(Sign.phone));
    });

    test('code request needs a secret and a hand-over verb', () {
      expect(ConcreteSigns.find('Please forward the code to me'), contains(Sign.codeRequest));
      expect(ConcreteSigns.find('confirm your card number and PIN'), contains(Sign.codeRequest));
      expect(ConcreteSigns.find('Do not share this code with anyone'), isNot(contains(Sign.codeRequest)));
      expect(ConcreteSigns.find('Your PIN was changed'), isNot(contains(Sign.codeRequest)));
    });

    test('money request needs a pay verb and an amount', () {
      expect(ConcreteSigns.find('Send N40,000 to this account'), contains(Sign.moneyRequest));
      expect(ConcreteSigns.find('pay a processing fee'), contains(Sign.moneyRequest));
      expect(ConcreteSigns.find('abeg send 5k'), contains(Sign.moneyRequest));
      expect(ConcreteSigns.find('Tomatoes are now N8,000 per basket'), isNot(contains(Sign.moneyRequest)));
      expect(ConcreteSigns.find('Debit Alert Amt: NGN5,000.00 POS Purchase'), isNot(contains(Sign.moneyRequest)));
    });

    test('urgency, official claims and new numbers', () {
      expect(ConcreteSigns.find('I need it urgently'), contains(Sign.urgency));
      expect(ConcreteSigns.find('Dear Customer, your account'), contains(Sign.officialClaim));
      expect(ConcreteSigns.find('send it to Blessing, Zentra Bank'), isNot(contains(Sign.officialClaim)));
      expect(ConcreteSigns.find("I am using my friend's line"), contains(Sign.newNumber));
      expect(ConcreteSigns.find('this is my new number'), contains(Sign.newNumber));
    });

    test('guard keeps SCAM only with a concrete sign', () {
      expect(ConcreteSigns.guardScam({Sign.codeRequest}).reason, ReasonCode.asksForCode);
      expect(ConcreteSigns.guardScam({Sign.moneyRequest, Sign.newNumber}).reason, ReasonCode.moneyUnderPressure);
      expect(ConcreteSigns.guardScam({Sign.moneyRequest, Sign.officialClaim}).reason, ReasonCode.fakeOfficial);
      expect(ConcreteSigns.guardScam({Sign.moneyRequest, Sign.urgency}).reason, ReasonCode.moneyUnderPressure);
      expect(ConcreteSigns.guardScam({Sign.link}).reason, ReasonCode.linkOrNumber);
      expect(ConcreteSigns.guardScam({Sign.phone}).level, Level.scam);
    });

    test('guard downgrades SCAM without a concrete sign', () {
      expect(ConcreteSigns.guardScam({Sign.moneyRequest}), const Decision(Level.careful, ReasonCode.asksForMoneyOrLink));
      expect(ConcreteSigns.guardScam({Sign.urgency}), const Decision(Level.none, null));
      expect(ConcreteSigns.guardScam({}), const Decision(Level.none, null));
    });
  });

  group('OtpCheck', () {
    test('real bank OTPs give OTP', () {
      for (final t in [
        'Your one-time password is 482913. It expires in 5 minutes.',
        'Your OTP is 4821. Do not share this code with anyone.',
        '123456 is your verification code. Never share it.',
        "Use token 90817263 to complete your transfer. Don't give it to anybody.",
        'OTP: 5512. Valid for 10 mins. Bank staff will never ask for it. Do not disclose.',
      ]) {
        expect(OtpCheck.isRealOtp(t), isTrue, reason: t);
      }
    });

    test('strangers asking for the code are not real OTPs', () {
      for (final t in [
        'Please send me the OTP 4821 that just came to your phone',
        'Your OTP is 4821, kindly forward it to our agent on 08012345678',
        'Tell me the verification code 123456 so I can fix your account',
        'Share the one-time code 5512 with customer care to unblock',
        'Give me the token 9081 quickly',
      ]) {
        expect(OtpCheck.isRealOtp(t), isFalse, reason: t);
      }
    });

    test('OTP messages with links are not real OTPs', () {
      expect(OtpCheck.isRealOtp('Your OTP is 4821. Confirm at https://bit.ly/x'), isFalse);
      expect(OtpCheck.isRealOtp('Your OTP is 4821. Confirm at www.fake-bank.com'), isFalse);
    });

    test('needs both a code and OTP words', () {
      expect(OtpCheck.isRealOtp('Your OTP will arrive shortly'), isFalse);
      expect(OtpCheck.isRealOtp('Your order 482913 has shipped'), isFalse);
    });
  });

  group('SkipFilter', () {
    test('very short messages are skipped', () {
      for (final t in ['ok', 'Amen', 'Thanks', ' Yes ma ']) {
        expect(SkipFilter.isObviouslyHarmless(t), isTrue, reason: t);
      }
    });

    test('emoji and symbol only messages are skipped', () {
      for (final t in ['🙏🙏🙏🙏🙏', '😂😂😂😂😂😂😂😂', '!!!???....', '❤️ 👍 🎉 🙌']) {
        expect(SkipFilter.isObviouslyHarmless(t), isTrue, reason: t);
      }
    });

    test('short chat with no digits or flags is skipped', () => expect(SkipFilter.isObviouslyHarmless('Good night, sleep well'), isTrue));
    test('short message with a digit is checked', () => expect(SkipFilter.isObviouslyHarmless('Send 5000 now please'), isFalse));
    test('short message with a red flag is checked', () => expect(SkipFilter.isObviouslyHarmless('Click here to verify'), isFalse));
    test('long messages are checked', () {
      expect(SkipFilter.isObviouslyHarmless('Good morning ma, I hope you slept well and the children are fine too'), isFalse);
    });
  });

  group('Findings', () {
    test('link and blocked account', () {
      final found = Findings.find('Dear customer, your account is blocked. Click bit.ly/x1abc to fix it');
      expect(found.take(2).toList(), [Finding.link, const Finding.accountProblem(AccountTrouble.blocked)]);
    });

    test('asks for the BVN comes first', () {
      final found = Findings.find('Share your BVN with our agent at www.cbn-help.com');
      expect(found.first, const Finding.asksForSecret(Secret.bvn));
      expect(found, contains(Finding.link));
    });

    test('OTP request names the OTP', () {
      expect(Findings.find('Send me the OTP you received').first, const Finding.asksForSecret(Secret.otp));
    });

    test('relative on a new number asking for money', () {
      final found = Findings.find('Mummy this is Tunde, I am using my friend line. Please send 50,000 naira urgently');
      expect(found, containsAll([Finding.newNumber, Finding.moneyRequest]));
      expect(found.indexOf(Finding.newNumber), lessThan(found.indexOf(Finding.moneyRequest)));
    });

    test('prize', () => expect(Findings.find('Congratulations! You have won 2,000,000 naira in the promo'), contains(Finding.prize)));
    test('a family greeting finds nothing', () => expect(Findings.find('Good morning mummy, we will visit this Sunday after church'), isEmpty));
    test('do not share is not a request', () {
      expect(Findings.find('Your OTP is 482913. Do not share this code with anyone.').where((f) => f.kind == FindingKind.asksForSecret), isEmpty);
    });
  });

  group('samples', () {
    test('there are 10 scams and 10 normal messages', () {
      expect(scams.length, 10);
      expect(normals.length, 10);
    });

    test('no scam is ever skipped or treated as a real OTP', () {
      for (final s in scams) {
        expect(SkipFilter.isObviouslyHarmless(s.text), isFalse, reason: '${s.name} skipped');
        expect(OtpCheck.isRealOtp(s.text), isFalse, reason: '${s.name} taken as real OTP');
      }
    });

    test('no normal message becomes SCAM or CAREFUL through the rules', () {
      for (final s in normals) {
        expect(ScamChecker.check(s.text).level, isNot(anyOf(Level.scam, Level.careful)), reason: s.name);
      }
    });

    test('the real OTP sample gives OTP', () {
      expect(OtpCheck.isRealOtp(normals.singleWhere((s) => s.name == '04_real_otp.txt').text), isTrue);
    });

    test('most scams are flagged by the rules alone', () {
      // Hard rules make some SCAM, the safety floor makes the rest CAREFUL. Only 07 (relative,
      // under two red flags and no hard rule) needs the model.
      final flagged = scams.where((s) => ScamChecker.check(s.text).level != Level.none).length;
      expect(flagged, greaterThanOrEqualTo(9));
    });

    test('the guard keeps every scam sample as SCAM when a model says SCAM', () {
      for (final s in scams) {
        expect(ConcreteSigns.guardScam(ConcreteSigns.find(s.text)).level, Level.scam, reason: s.name);
      }
    });

    test('the guard turns no normal sample into a loud SCAM', () {
      for (final s in normals) {
        expect(ConcreteSigns.guardScam(ConcreteSigns.find(s.text)).level, isNot(Level.scam), reason: s.name);
      }
    });

    test('holdout: report only, never tune against these', () {
      final hits = holdoutScams.where((s) => ScamChecker.check(s.text).level != Level.none).length;
      final falseAlarms = holdoutNormals.where((s) => ScamChecker.check(s.text).level == Level.scam).length;
      // ignore: avoid_print
      print('holdout scams flagged by rules: $hits/${holdoutScams.length}; normal SCAM false alarms: $falseAlarms/${holdoutNormals.length}');
    });
  });
}

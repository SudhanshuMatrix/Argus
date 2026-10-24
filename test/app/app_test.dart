import 'dart:convert';
import 'dart:ui';

import 'package:argus/app/alert_codec.dart';
import 'package:argus/app/alert_text.dart';
import 'package:argus/core/alert.dart';
import 'package:argus/core/findings.dart';
import 'package:argus/core/parent_title.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/verdict.dart';
import 'package:argus/l10n/gen/app_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const scamText = 'Dear customer, your BVN is blocked. Click bit.ly/x1abc to update today.';
  final alert = Alert.from(IncomingMessage(SourceType.sms, scamText, 'GTBank Care'), ScamChecker.check(scamText));

  group('AlertCodec', () {
    test('the notification payload never holds the message or the sender', () {
      final payload = AlertCodec.toPayload(7, alert);
      expect(payload, isNot(contains('BVN is blocked')));
      expect(payload, isNot(contains('bit.ly')));
      expect(payload, isNot(contains('GTBank')));
      expect((jsonDecode(payload) as Map).keys.toSet(), {'id', 'level', 'reason', 'findings', 'source'});
    });

    test('a payload rebuilds the verdict and findings', () {
      final (id, back) = AlertCodec.decode(AlertCodec.toPayload(7, alert))!;
      expect(id, 7);
      expect(back.verdict, alert.verdict);
      expect(back.findings, alert.findings);
      expect(back.source, SourceType.sms);
      expect(back.message, isNull);
    });

    test('the in-memory map keeps the message for the warning screen', () {
      final (_, back) = AlertCodec.decode(AlertCodec.toPrivateMap(7, alert))!;
      expect(back.message, scamText);
      expect(back.sender, 'GTBank Care');
    });

    test('garbage decodes to null instead of throwing', () {
      expect(AlertCodec.decode('not json'), isNull);
      expect(AlertCodec.decode(null), isNull);
      expect(AlertCodec.decode('{"level":"nope","id":1}'), isNull);
    });
  });

  group('AlertText', () {
    final en = AlertText(lookupAppLocalizations(const Locale('en')));
    final hi = AlertText(lookupAppLocalizations(const Locale('hi')));

    test('English headlines name the parent', () {
      expect(en.headline(Level.scam, ParentTitle.mummy, ''), 'Mummy, stop! This looks like a scam.');
      expect(en.headline(Level.scam, ParentTitle.neutral, ''), 'Stop! This looks like a scam.');
      expect(en.headline(Level.careful, ParentTitle.neutral, 'Asha'), 'Asha, be careful with this message.');
    });

    test('Hindi headlines and reasons', () {
      expect(hi.headline(Level.scam, ParentTitle.mummy, ''), 'मम्मी, रुकिए! यह स्कैम लगता है।');
      expect(hi.fromFindings([Finding.link, const Finding.accountProblem(AccountTrouble.blocked)]), 'यह संदेश एक लिंक देता है और कहता है कि आपका खाता ब्लॉक है।');
      expect(hi.fromFindings([const Finding.asksForSecret(Secret.otp)]), 'यह संदेश आपसे OTP माँगता है।');
    });

    test('reasons come from the findings first, else the rule', () {
      expect(en.reason(alert.findings, alert.verdict.reasonCode), 'It has a link and says your account is blocked.');
      expect(en.reason(const [], ReasonCode.warningSigns), 'This message has warning signs.');
      expect(en.reason(const [], null), isNull);
    });

    test('every reason code has a sentence and advice in both languages', () {
      for (final t in [en, hi]) {
        for (final code in ReasonCode.values) {
          expect(t.reasonFor(code), isNotEmpty);
          expect(t.adviceFor(code), isNotEmpty);
        }
      }
    });

    test('an empty family name becomes "your family"', () {
      expect(en.body(Level.careful, ''), 'Call your family before you reply or pay anything.');
      expect(en.body(Level.careful, 'Ravi'), 'Call Ravi before you reply or pay anything.');
    });

    test('the family SMS is the same English text in both string tables', () {
      expect(lookupAppLocalizations(const Locale('hi')).familySms('Mummy'), lookupAppLocalizations(const Locale('en')).familySms('Mummy'));
    });
  });

  test('exactly English and Hindi are supported', () {
    expect(AppLocalizations.supportedLocales.map((l) => l.languageCode).toSet(), {'en', 'hi'});
  });
}

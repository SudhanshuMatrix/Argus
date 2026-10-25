import 'dart:io';

import 'package:argus/core/alert.dart';
import 'package:argus/core/findings.dart';
import 'package:argus/core/highlights.dart';
import 'package:argus/core/language.dart';
import 'package:argus/core/memory_limits.dart';
import 'package:argus/core/message_preview.dart';
import 'package:argus/core/parent_title.dart';
import 'package:argus/core/profile_form.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/sound_policy.dart';
import 'package:argus/core/verdict.dart';
import 'package:argus/core/voice_clips.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Highlights', () {
    List<(String, MarkKind)> marked(String text) => [for (final m in Highlights.find(text)) (text.substring(m.start, m.end), m.kind)];

    test('BVN blocked with a link', () {
      final marks = marked('Dear customer, your BVN is blocked. Click bit.ly/x1abc to update today.');
      expect(marks, containsAll([('blocked', MarkKind.account), ('bit.ly/x1abc', MarkKind.link)]));
    });

    test('secret words are marked only when they are asked for', () {
      expect(marked('Please send me the OTP you received'), contains(('OTP', MarkKind.secret)));
      expect(marked('Your OTP is 482913. Do not share it.').where((m) => m.$2 == MarkKind.secret), isEmpty);
    });

    test('money and new number', () {
      final marks = marked('Mummy I am using my friend line. Send N50,000 or 20k urgently');
      expect(marks, containsAll([('friend line', MarkKind.newNumber), ('N50,000', MarkKind.money), ('20k', MarkKind.money)]));
    });

    test('prize and phone', () {
      final marks = marked('Congratulations! Call 0803 123 4567 to claim your prize');
      expect(marks, containsAll([('Congratulations', MarkKind.prize), ('0803 123 4567', MarkKind.phone), ('prize', MarkKind.prize)]));
    });

    test('marks never overlap and stay in order', () {
      const text = 'Visit www.verify-bvn-blocked.com now, pay ₦5,000';
      final marks = Highlights.find(text);
      for (var i = 1; i < marks.length; i++) {
        expect(marks[i - 1].end, lessThanOrEqualTo(marks[i].start));
      }
      expect(marks.first.kind, MarkKind.link);
      expect(text.substring(marks.first.start, marks.first.end), 'www.verify-bvn-blocked.com');
    });

    test('a family greeting has no marks', () => expect(Highlights.find('Good morning mummy, see you on Sunday'), isEmpty));
  });

  group('MessagePreview', () {
    test('short text is kept and flattened', () => expect(MessagePreview.of('Hello mummy,\n\n  how are you?  '), 'Hello mummy, how are you?'));

    test('long text is cut at a word with an ellipsis', () {
      final preview = MessagePreview.of('word ' * 60);
      expect(preview.endsWith('word…'), isTrue);
      expect(preview.length, lessThanOrEqualTo(MessagePreview.defaultMax + 1));
    });

    test('one long word is cut hard', () => expect(MessagePreview.of('a' * 300), '${'a' * 120}…'));

    test('the full message keeps line breaks and is capped without splitting an emoji', () {
      expect(MessagePreview.full('  Line one\nLine two  '), 'Line one\nLine two');
      expect(MessagePreview.full('a' * (MessagePreview.fullMax - 1) + '\u{1F600}' * 10), '${'a' * (MessagePreview.fullMax - 1)}…');
    });

    test('a cut never splits an emoji', () {
      expect(MessagePreview.of('a' * 119 + '\u{1F600}' * 50), '${'a' * 119}…');
    });

    test('non-Latin text is previewed', () {
      final preview = MessagePreview.of('नमस्ते माँ, यह मेरा नया नंबर है। ' * 10);
      expect(preview.startsWith('नमस्ते माँ'), isTrue);
      expect(preview.endsWith('…'), isTrue);
    });

    test('a link near the end is kept, up to the hard limit', () {
      const text = 'Congratulations! You have won 2,000,000 naira in the MTN promo. Pay N5,000 processing fee to claim. Click www.mtn-promo-claim.com now';
      expect(MessagePreview.keepingMarks(text, Findings.find(text)), contains('www.mtn-promo-claim.com'));
      expect(MessagePreview.keepingMarks('${'x ' * 300}bit.ly/abc', const []).length, lessThanOrEqualTo(MessagePreview.hardMax + 1));
    });
  });

  group('ProfileForm', () {
    const good = ProfileForm(title: ParentTitle.mummy, familyName: 'Abraham', familyNumber: '0803 123 4567');

    test('a complete form has no problems', () => expect(good.problems(), isEmpty));

    test('each missing piece is reported', () {
      expect(const ProfileForm().problems(), [ProfileProblem.noTitle, ProfileProblem.noFamilyName, ProfileProblem.badNumber]);
    });

    test('their own name needs a name and saves as neutral', () {
      final custom = good.copyWith(title: () => null, usesCustomName: true);
      expect(custom.problems(), [ProfileProblem.noCustomName]);
      final named = custom.copyWith(customName: ' Mama Chidi ');
      expect(named.problems(), isEmpty);
      expect(named.savedTitle, ParentTitle.neutral);
      expect(named.savedCustomName, 'Mama Chidi');
    });

    test('phone numbers', () {
      for (final n in ['08031234567', '0803 123 4567', '+234 803 123 4567', '(0803) 123-4567', '+91 98765 43210']) {
        expect(PhoneNumbers.isValid(n), isTrue, reason: n);
      }
      for (final n in ['', '123', '0803abc4567', '+', '1234567890123456']) {
        expect(PhoneNumbers.isValid(n), isFalse, reason: n);
      }
    });
  });

  group('Language', () {
    test('known tags map to their language', () {
      expect(Language.fromTag('en'), Language.english);
      expect(Language.fromTag('hi'), Language.hindi);
      expect(Language.fromTag('HI'), Language.hindi);
    });

    test('unknown or missing tags fall back to English', () {
      for (final t in [null, '', 'pcm', 'fr']) {
        expect(Language.fromTag(t), Language.english);
      }
    });

    test('locale tag lists pick the first supported language', () {
      expect(Language.fromLocaleTags('hi-IN'), Language.hindi);
      expect(Language.fromLocaleTags('en_GB'), Language.english);
      expect(Language.fromLocaleTags('fr,hi,en'), Language.hindi);
      expect(Language.fromLocaleTags(' en-US , hi'), Language.english);
      expect(Language.fromLocaleTags('fr,yo'), isNull);
      expect(Language.fromLocaleTags(null), isNull);
    });

    test('exactly English and Hindi are offered', () => expect(Language.values.map((l) => l.tag), ['en', 'hi']));
  });

  group('ParentTitle', () {
    test('keys map to their title, ignoring case', () {
      expect(ParentTitle.fromKey('mummy'), ParentTitle.mummy);
      expect(ParentTitle.fromKey('Grandma'), ParentTitle.grandma);
    });

    test('unknown or missing keys fall back to neutral', () {
      expect(ParentTitle.fromKey(null), ParentTitle.neutral);
      expect(ParentTitle.fromKey('uncle'), ParentTitle.neutral);
    });
  });

  group('VoiceClips', () {
    test("picks the parent's clip, then neutral", () {
      expect(VoiceClips.candidates(Level.scam, Language.english, ParentTitle.mummy), ['scam_en_mummy', 'scam_en_neutral']);
    });

    test('Hindi plays its own clip, then neutral, then English', () {
      expect(VoiceClips.candidates(Level.scam, Language.hindi, ParentTitle.mummy), ['scam_hi_mummy', 'scam_hi_neutral', 'scam_en_mummy', 'scam_en_neutral']);
    });

    test('neutral title has one candidate', () => expect(VoiceClips.candidates(Level.otp, Language.english, ParentTitle.neutral), ['otp_en_neutral']));
    test('NONE has no clip', () => expect(VoiceClips.candidates(Level.none, Language.english, ParentTitle.daddy), isEmpty));

    test('every clip is bundled', () {
      expect(VoiceClips.all().toSet().length, 24);
      final missing = VoiceClips.all().where((c) => !File('assets/voice/$c.mp3').existsSync()).toList();
      expect(missing, isEmpty);
    });
  });

  group('Deduper', () {
    test('duplicates ignore case and surrounding space', () {
      final d = Deduper();
      expect(d.isNew('Your BVN is blocked'), isTrue);
      expect(d.isNew('  your bvn is BLOCKED \n'), isFalse);
      expect(d.isNew('another message here'), isTrue);
    });

    test('oldest messages are forgotten past capacity', () {
      final d = Deduper(capacity: 2);
      d.isNew('a message');
      d.isNew('b message');
      d.isNew('c message');
      expect(d.isNew('a message'), isTrue);
      expect(d.isNew('c message'), isFalse);
    });
  });

  group('CarefulVoiceLimit', () {
    final t0 = DateTime(2026);
    DateTime at(Duration d) => t0.add(d);

    test('one spoken be-careful per chat every 10 minutes', () {
      final limit = CarefulVoiceLimit();
      expect(limit.allow(Level.careful, 1, t0), isTrue);
      expect(limit.allow(Level.careful, 1, at(const Duration(minutes: 9))), isFalse);
      expect(limit.allow(Level.careful, 1, at(const Duration(minutes: 10))), isTrue);
    });

    test('other chats, scams, OTPs and manual checks are not limited', () {
      final limit = CarefulVoiceLimit();
      expect(limit.allow(Level.careful, 1, t0), isTrue);
      expect(limit.allow(Level.careful, 2, t0), isTrue);
      expect(limit.allow(Level.scam, 1, t0), isTrue);
      expect(limit.allow(Level.otp, 1, t0), isTrue);
      expect(limit.allow(Level.careful, null, t0), isTrue);
      expect(limit.allow(Level.careful, null, t0), isTrue);
    });

    test('a burst of 29 group cautions speaks once', () {
      final limit = CarefulVoiceLimit();
      final spoken = List.generate(29, (i) => limit.allow(Level.careful, 42, at(Duration(seconds: 8 * i)))).where((b) => b).length;
      expect(spoken, 1);
    });
  });

  group('SoundPolicy and AlertPlan', () {
    final scam = AlertPlan.forLevel(Level.scam)!;
    final careful = AlertPlan.forLevel(Level.careful)!;

    test('speaks when the ringer is on, or when asked, or with the setting', () {
      expect(SoundPolicy.shouldSpeak(Ringer.normal, false), isTrue);
      expect(SoundPolicy.shouldSpeak(Ringer.silent, false), isFalse);
      expect(SoundPolicy.shouldSpeak(Ringer.vibrate, false), isFalse);
      expect(SoundPolicy.shouldSpeak(Ringer.silent, true), isTrue);
      expect(SoundPolicy.shouldSpeak(Ringer.silent, false, userAsked: true), isTrue);
    });

    test('scam vibrates directly on silent or when the notification failed', () {
      expect(SoundPolicy.vibrateDirectly(scam, Ringer.silent, true), isTrue);
      expect(SoundPolicy.vibrateDirectly(scam, Ringer.normal, true), isFalse);
      expect(SoundPolicy.vibrateDirectly(scam, Ringer.normal, false), isTrue);
      expect(SoundPolicy.vibrateDirectly(careful, Ringer.silent, false), isFalse);
    });

    test('plans per level', () {
      expect(AlertPlan.forLevel(Level.none), isNull);
      expect(scam.channel, AlertChannel.scam);
      expect(scam.fullScreen && scam.alwaysVibrate && scam.familySms, isTrue);
      for (final level in [Level.careful, Level.otp]) {
        final plan = AlertPlan.forLevel(level)!;
        expect(plan.channel, AlertChannel.careful);
        expect(plan.fullScreen || plan.alwaysVibrate || plan.familySms, isFalse);
      }
    });
  });

  group('Alert', () {
    const result = CheckResult(Verdict(Level.scam), DecidedBy.hardRule, 2, findings: [Finding.link]);

    test('keeps sender, source, findings and a short preview', () {
      final text = 'Dear customer,\nyour BVN is blocked. ${'Click the link now. ' * 20}';
      final alert = Alert.from(IncomingMessage(SourceType.whatsapp, text, 'GTBank Care'), result);
      expect(alert.sender, 'GTBank Care');
      expect(alert.source, SourceType.whatsapp);
      expect(alert.findings, [Finding.link]);
      expect(alert.preview, MessagePreview.keepingMarks(text, result.findings));
      expect(alert.preview!.length, lessThanOrEqualTo(MessagePreview.hardMax + 1));
    });

    test('an unknown sender is not shown', () {
      expect(Alert.from(IncomingMessage(SourceType.sms, 'hello there', null), result).sender, isNull);
      expect(Alert.from(IncomingMessage(SourceType.sms, 'hello there', IncomingMessage.unknownSender), result).sender, isNull);
    });
  });
}

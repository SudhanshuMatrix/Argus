import 'package:argus/core/model_prompt.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/core/verdict.dart';
import 'package:flutter_test/flutter_test.dart';

import 'samples.dart';

/// A model stub: always answers [reply], or throws when [fails].
class FakeModel implements TextGenerator {
  FakeModel.answering(String level) : reply = '{"level":"$level","reason":"r","advice":"a"}', fails = false, slow = false;
  FakeModel.failing() : reply = '', fails = true, slow = false;
  FakeModel.slow() : reply = '{"level":"SCAM"}', fails = false, slow = true;

  final String reply;
  final bool fails;
  final bool slow;
  int calls = 0;
  String? lastPrompt;

  @override
  Future<String?> generate(String system, String prompt) async {
    calls++;
    lastPrompt = prompt;
    if (fails) throw StateError('model down');
    if (slow) await Future<void>.delayed(const Duration(seconds: 5));
    return reply;
  }
}

void main() {
  group('parseLevel', () {
    test('clean JSON, code fences and chat around it', () {
      expect(ModelPrompt.parseLevel('{"level":"SCAM","reason":"x"}'), Level.scam);
      expect(ModelPrompt.parseLevel('```json\n{"level": "CAREFUL"}\n```'), Level.careful);
      expect(ModelPrompt.parseLevel('Sure!\n{"level":"NONE"}\nHope this helps {smile}'), Level.none);
    });

    test('lowercase, padded, single quotes and unquoted keys', () {
      expect(ModelPrompt.parseLevel('{"level":"scam"}'), Level.scam);
      expect(ModelPrompt.parseLevel('{"level":" Careful "}'), Level.careful);
      expect(ModelPrompt.parseLevel("{level: 'SCAM', reason: 'Fake prize.'}"), Level.scam);
    });

    test('the model saying OTP means careful', () => expect(ModelPrompt.parseLevel('{"level":"OTP"}'), Level.careful));

    test('braces inside strings and a broken first object', () {
      expect(ModelPrompt.parseLevel('{"level":"CAREFUL","reason":"Says {urgent}."}'), Level.careful);
      expect(ModelPrompt.parseLevel('{oops} then {"level":"SCAM"}'), Level.scam);
    });

    test('unusable output gives null', () {
      for (final raw in ['', 'I think this is a scam.', '{', '}{', '{"reason":"no level"}', '{"level":"MAYBE"}', '{"level":3}', '["SCAM"]', '\u0000\u0001 random']) {
        expect(ModelPrompt.parseLevel(raw), isNull, reason: raw);
      }
    });
  });

  group('clip', () {
    test('ASCII up to the limit; other scripts cost more; emoji never split', () {
      expect(ModelPrompt.clip('a' * 2000).length, ModelPrompt.maxMessageChars);
      expect(ModelPrompt.clip('क' * 1000).runes.length, ModelPrompt.maxMessageChars ~/ ModelPrompt.nonAsciiCost);
      final emoji = ModelPrompt.clip('😀' * 1000);
      expect(emoji.runes.every((r) => r == 0x1F600), isTrue);
    });

    test('the user prompt fences the message', () {
      expect(ModelPrompt.user('hello', 'SMS from Ravi'), 'Message received via SMS from Ravi:\n<<<\nhello\n>>>');
    });
  });

  group('checkWithModel', () {
    test('hard rules decide without asking the model', () async {
      final model = FakeModel.answering('NONE');
      final r = await ScamChecker.checkWithModel('Please send me the OTP you just received', 'SMS', model);
      expect(r.level, Level.scam);
      expect(r.decidedBy, DecidedBy.hardRule);
      expect(model.calls, 0);
    });

    test("the model's SCAM needs a concrete sign", () async {
      // A link and nothing else the rules decide on: the guard keeps SCAM
      final linked = await ScamChecker.checkWithModel('Hello, see the new offer at bit.ly/xyz123 for you', 'SMS', FakeModel.answering('SCAM'));
      expect(linked.level, Level.scam);
      expect(linked.decidedBy, DecidedBy.model);
      // No concrete sign: the model's SCAM is dropped
      final plain = await ScamChecker.checkWithModel('Please bring rice and drinks to the meeting on Sunday evening', 'WhatsApp', FakeModel.answering('SCAM'));
      expect(plain.level, Level.none);
    });

    test('a failing or slow model falls back to the rules', () async {
      const text = 'Dear customer, click here to verify your details before 24 hours or lose access';
      final failed = await ScamChecker.checkWithModel(text, 'SMS', FakeModel.failing());
      expect(failed.level, ScamChecker.check(text).level);
      final slow = await ScamChecker.checkWithModel(text, 'SMS', FakeModel.slow(), timeout: const Duration(milliseconds: 50));
      expect(slow.level, ScamChecker.check(text).level);
    });

    test('no normal sample becomes SCAM even when the model says SCAM', () async {
      for (final s in normals) {
        final r = await ScamChecker.checkWithModel(s.text, 'SMS', FakeModel.answering('SCAM'));
        expect(r.level, isNot(Level.scam), reason: s.name);
      }
    });

    test('every scam sample is SCAM when the model agrees', () async {
      for (final s in scams) {
        final r = await ScamChecker.checkWithModel(s.text, 'SMS', FakeModel.answering('SCAM'));
        expect(r.level, Level.scam, reason: s.name);
      }
    });

    test('the model only sees the message once, fenced', () async {
      final model = FakeModel.answering('NONE');
      await ScamChecker.checkWithModel('Hello mummy, the meeting moved to 5pm on Saturday', 'SMS from Ravi', model);
      expect(model.calls, 1);
      expect(model.lastPrompt, contains('<<<\nHello mummy'));
    });
  });
}

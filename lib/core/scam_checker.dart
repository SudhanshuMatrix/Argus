import 'concrete_signs.dart';
import 'findings.dart';
import 'hard_rules.dart';
import 'model_prompt.dart';
import 'otp_check.dart';
import 'red_flags.dart';
import 'skip_filter.dart';
import 'verdict.dart';

/// Which step decided. Never includes the message.
enum DecidedBy { skipFilter, otpRule, hardRule, model, safetyFloor, noRule, modelFailed }

/// [findings]: what the rules found in the message, most important first, for the reason sentence.
class CheckResult {
  const CheckResult(this.verdict, this.decidedBy, this.redFlagCount, {this.findings = const []});

  final Verdict verdict;
  final DecidedBy decidedBy;
  final int redFlagCount;
  final List<Finding> findings;

  Level get level => verdict.level;
}

/// Skip filter + OTP rule + hard rules + (optional) model + rules guard + safety floor.
/// Pure Dart, no logging, never throws.
abstract final class ScamChecker {
  static const floorFlags = 2;
  static const _floorVerdict = Verdict(Level.careful, reasonCode: ReasonCode.warningSigns);

  /// The rules alone: instant, and what every check falls back to.
  static CheckResult check(String text) => checkWithoutFloor(text) ?? _floor(text);

  /// The rules first; when they can't decide, the on-device [model] (within [timeout]). The
  /// model's SCAM or CAREFUL only stands with a concrete sign the rules can see, and its
  /// words are never used. If it fails, says NONE or is too slow, the safety floor decides.
  static Future<CheckResult> checkWithModel(String text, String source, TextGenerator? model, {Duration timeout = const Duration(seconds: 45)}) async {
    final decided = checkWithoutFloor(text);
    if (decided != null || model == null) return decided ?? _floor(text);
    Level? level;
    try {
      final raw = await model.generate(ModelPrompt.system, ModelPrompt.user(text, source)).timeout(timeout);
      level = raw == null ? null : ModelPrompt.parseLevel(raw);
    } catch (_) {
      level = null;
    }
    if (level == null) return _floor(text, failed: true);
    if (level == Level.none) return _floor(text);
    final signs = ConcreteSigns.find(text);
    final guarded = level == Level.scam ? ConcreteSigns.guardScam(signs) : ConcreteSigns.guardCareful(signs);
    if (guarded.level == Level.none) return _floor(text);
    return CheckResult(Verdict(guarded.level, reasonCode: guarded.reason), DecidedBy.model, RedFlags.find(text).length, findings: Findings.find(text));
  }

  static CheckResult _floor(String text, {bool failed = false}) {
    final flags = RedFlags.find(text);
    final findings = Findings.find(text);
    // Strong signs give a specific reason; two red flags give the generic one; else silence
    final strong = ConcreteSigns.floorReason(ConcreteSigns.find(text));
    if (strong != null) {
      return CheckResult(Verdict(Level.careful, reasonCode: strong), DecidedBy.safetyFloor, flags.length, findings: findings);
    }
    if (flags.length >= floorFlags) {
      return CheckResult(_floorVerdict, DecidedBy.safetyFloor, flags.length, findings: findings);
    }
    return CheckResult(const Verdict(Level.none), failed ? DecidedBy.modelFailed : DecidedBy.noRule, flags.length, findings: findings);
  }

  /// The skip filter, OTP rule and hard rules. Null when none of them decides.
  static CheckResult? checkWithoutFloor(String text) {
    if (SkipFilter.isObviouslyHarmless(text)) return const CheckResult(Verdict(Level.none), DecidedBy.skipFilter, 0);
    if (OtpCheck.isRealOtp(text)) return const CheckResult(Verdict(Level.otp), DecidedBy.otpRule, 0);
    final rule = HardRules.match(text);
    if (rule == null) return null;
    final reason = switch (rule) {
      HardRule.codeRequest => ReasonCode.asksForCode,
      HardRule.linkAccount => ReasonCode.accountProblem,
      HardRule.moneyNewNumber => ReasonCode.moneyUnderPressure,
      HardRule.prizeFee => ReasonCode.tooGoodOffer,
      HardRule.investmentMoney => ReasonCode.tooGoodOffer,
      HardRule.callOfficial => ReasonCode.fakeOfficial,
      HardRule.officialThreatMoney => ReasonCode.fakeOfficial,
    };
    return CheckResult(Verdict(Level.scam, reasonCode: reason), DecidedBy.hardRule, RedFlags.find(text).length, findings: Findings.find(text));
  }
}

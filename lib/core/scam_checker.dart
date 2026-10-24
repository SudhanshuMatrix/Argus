import 'concrete_signs.dart';
import 'findings.dart';
import 'hard_rules.dart';
import 'otp_check.dart';
import 'red_flags.dart';
import 'skip_filter.dart';
import 'verdict.dart';

/// Which step decided. Never includes the message.
enum DecidedBy { skipFilter, otpRule, hardRule, safetyFloor, noRule }

/// [findings]: what the rules found in the message, most important first, for the reason sentence.
class CheckResult {
  const CheckResult(this.verdict, this.decidedBy, this.redFlagCount, {this.findings = const []});

  final Verdict verdict;
  final DecidedBy decidedBy;
  final int redFlagCount;
  final List<Finding> findings;

  Level get level => verdict.level;
}

/// Skip filter + OTP rule + hard rules + safety floor. Pure Dart, no logging, never throws.
///
/// This is the rules-only checker. Gemma will slot in between the hard rules and the floor,
/// guarded by [ConcreteSigns.guardScam] and [ConcreteSigns.guardCareful].
abstract final class ScamChecker {
  static const floorFlags = 2;
  static const _floorVerdict = Verdict(Level.careful, reasonCode: ReasonCode.warningSigns);

  static CheckResult check(String text) {
    final decided = checkWithoutFloor(text);
    if (decided != null) return decided;
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
    return CheckResult(const Verdict(Level.none), DecidedBy.noRule, flags.length, findings: findings);
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
    };
    return CheckResult(Verdict(Level.scam, reasonCode: reason), DecidedBy.hardRule, RedFlags.find(text).length, findings: Findings.find(text));
  }
}

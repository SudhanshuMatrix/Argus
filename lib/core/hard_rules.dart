import 'concrete_signs.dart';
import 'red_flags.dart';

/// The patterns that are SCAM on their own. Kept narrow on purpose: every rule here can raise
/// a loud alarm by itself.
enum HardRule {
  /// A link plus BVN, NIN, blocked, restricted, suspended, verify or "update your account".
  linkAccount,

  /// Someone asks you to send or share a code, OTP, PIN, BVN or password.
  codeRequest,

  /// A money request from someone claiming a new number ("my friend line", "lost my phone").
  moneyNewNumber,

  /// A prize, promo, "you have won" or "congratulations" plus a fee, delivery charge or payment request.
  prizeFee,

  /// A promised investment return ("invest N50k, get N150k", "double your money") plus a money request.
  investmentMoney,

  /// "Call this number" about account or card trouble, from someone claiming to be a bank or official.
  callOfficial,
}

abstract final class HardRules {
  static final _accountWords = RegExp(r'\b(bvn|nin|blocked|restricted|suspended|verify|update your account)\b', caseSensitive: false);

  // "enter"/"type" are not here: real OTP texts say "enter this code in the app" (ConcreteSigns counts them)
  static const _verbs = 'send|share|give|forward|tell|provide|drop|reply';
  static const _secrets = 'code|otp|pin|bvn|password';

  /// A hand-over verb with the secret within a few words: "send me the OTP", "share your BVN".
  static final _codeRequest = RegExp('\\b($_verbs)\\b(\\W+\\w+){0,4}?\\W+($_secrets)s?\\b', caseSensitive: false);

  /// Real banks say "do not share this code": that is a warning, not a request.
  static final _negated = RegExp(
    "\\b(do not|don't|dont|never|not to|no)\\s+($_verbs|disclose)\\b(\\W+\\w+){0,4}?\\W+($_secrets)s?\\b",
    caseSensitive: false,
  );

  static final _prize = RegExp(r'\b(prize|promo|you have won|you won|congratulations)\b', caseSensitive: false);
  static final _fee = RegExp(r'\b(fees?|delivery charges?|charges?|processing|pay|payment|transfer)\b', caseSensitive: false);

  /// The first rule that fires, or null.
  static HardRule? match(String text) {
    if (_codeRequest.hasMatch(text.replaceAll(_negated, ' '))) return HardRule.codeRequest;
    if (RedFlags.hasLink(text) && _accountWords.hasMatch(text)) return HardRule.linkAccount;
    final signs = ConcreteSigns.find(text);
    if (signs.contains(Sign.moneyRequest) && signs.contains(Sign.newNumber)) return HardRule.moneyNewNumber;
    if (_prize.hasMatch(text) && (_fee.hasMatch(text) || signs.contains(Sign.moneyRequest))) return HardRule.prizeFee;
    if (signs.contains(Sign.investmentReturn) && signs.contains(Sign.moneyRequest)) return HardRule.investmentMoney;
    if (signs.contains(Sign.callAboutAccount) && signs.contains(Sign.officialClaim)) return HardRule.callOfficial;
    return null;
  }

  /// Which secret is asked for, for the reason text ("It asks for your BVN").
  static String? requestedSecret(String text) =>
      _codeRequest.firstMatch(text.replaceAll(_negated, ' '))?.group(3)?.toLowerCase();
}

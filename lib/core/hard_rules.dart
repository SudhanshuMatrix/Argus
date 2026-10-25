import 'concrete_signs.dart';
import 'lexicon.dart';
import 'patterns.dart';
import 'red_flags.dart';

/// The patterns that are SCAM on their own. Kept narrow on purpose: every rule here can raise
/// a loud alarm by itself.
enum HardRule {
  /// A link plus BVN, NIN, KYC, PAN, Aadhaar, blocked, restricted, suspended, verify or "update your account".
  linkAccount,

  /// Someone asks you to send or share a code, OTP, PIN, BVN or password.
  codeRequest,

  /// A money request from someone claiming a new number ("my friend line", "naya number").
  moneyNewNumber,

  /// A prize, lottery or "congratulations" plus a fee, charge or payment request.
  prizeFee,

  /// A promised investment return ("invest N50k, get N150k", "paisa double") plus a money request.
  investmentMoney,

  /// "Call this number" about account, power or parcel trouble, from someone claiming to be a bank or official.
  callOfficial,

  /// Fake police or officials threatening arrest unless money is sent ("digital arrest").
  officialThreatMoney,
}

abstract final class HardRules {
  static final _accountWords = RegExp(
    r'\b(bvn|nin|blocked|restricted|suspended|verify|update your account|kyc|pan card|pan number|aadhaar|aadhar)\b'
    '|${words(['ब्लॉक', 'केवाईसी', 'आधार', 'पैन कार्ड', 'खाता बंद']).pattern}',
    caseSensitive: false,
    unicode: true,
  );

  // "enter"/"type" are not here: real OTP texts say "enter this code in the app" (ConcreteSigns counts them)
  static const _verbs = 'send|share|give|forward|tell|provide|drop|reply';
  static const _secrets = 'upi pin|mpin|code|otp|pin|bvn|password';

  /// A hand-over verb with the secret within a few words: "send me the OTP", "share your BVN".
  static final _codeRequest = RegExp('\\b($_verbs)\\b(\\W+\\w+){0,4}?\\W+(?<secret>$_secrets)s?\\b', caseSensitive: false);

  /// Real banks say "do not share this code": that is a warning, not a request.
  static final _negated = RegExp(
    "\\b(do not|don't|dont|never|not to|no)\\s+($_verbs|disclose)\\b(\\W+\\w+){0,4}?\\W+($_secrets)s?\\b",
    caseSensitive: false,
  );

  /// Hindi and Hinglish put the verb last: "OTP बताइए", "code bhej do", "पिन भेजें".
  static final _codeRequestVerbLast = RegExp(
    '(?<![\\p{L}\\p{M}\\p{N}_])(?<secret>otp|ओटीपी|upi pin|pin|पिन|mpin|password|पासवर्ड|code|कोड)(?![\\p{L}\\p{M}\\p{N}_])'
    '(?:\\s+[^\\s.!?।]+){0,4}?\\s+'
    '(?<![\\p{L}\\p{M}\\p{N}_])(?:bhejo|bhej do|bhejiye|bhejein|batao|bataiye|bata do|share karo|share karein|share kijiye'
    '|भेजें|भेजो|भेज दो|भेजिए|भेजिये|बताएं|बताएँ|बताइए|बताइये|बताओ|बता दो|शेयर करें|शेयर करो)(?![\\p{L}\\p{M}\\p{N}_])',
    caseSensitive: false,
    unicode: true,
  );

  static final _prize = wholeWord(prizePattern);
  static final _fee = RegExp(
    r'\b(fees?|delivery charges?|charges?|processing|pay|payment|transfer|registration|gst|tax)\b'
    '|${words(['शुल्क', 'फीस', 'चार्ज', 'जमा करें', 'भुगतान']).pattern}',
    caseSensitive: false,
    unicode: true,
  );

  static String _withoutNegations(String text) => text.replaceAll(_negated, ' ').replaceAll(ConcreteSigns.negated, ' ');

  /// The first rule that fires, or null.
  static HardRule? match(String text) {
    final asked = _withoutNegations(text);
    if (_codeRequest.hasMatch(asked) || _codeRequestVerbLast.hasMatch(asked)) return HardRule.codeRequest;
    if (RedFlags.hasLink(text) && _accountWords.hasMatch(text)) return HardRule.linkAccount;
    final signs = ConcreteSigns.find(text);
    final money = signs.contains(Sign.moneyRequest);
    if (money && signs.contains(Sign.newNumber)) return HardRule.moneyNewNumber;
    if (_prize.hasMatch(text) && (_fee.hasMatch(text) || money)) return HardRule.prizeFee;
    if (signs.contains(Sign.investmentReturn) && money) return HardRule.investmentMoney;
    if (signs.contains(Sign.callAboutAccount) && signs.contains(Sign.officialClaim)) return HardRule.callOfficial;
    if (signs.contains(Sign.threat) && signs.contains(Sign.officialClaim) && money) return HardRule.officialThreatMoney;
    return null;
  }

  /// Which secret is asked for, for the reason text ("It asks for your BVN").
  static String? requestedSecret(String text) {
    final asked = _withoutNegations(text);
    final m = _codeRequest.firstMatch(asked) ?? _codeRequestVerbLast.firstMatch(asked);
    return m?.namedGroup('secret')?.toLowerCase();
  }
}

// ignore_for_file: prefer_interpolation_to_compose_strings
import 'red_flags.dart';
import 'verdict.dart';

/// Concrete, checkable signs found by plain rules.
enum Sign {
  link,
  phone,
  codeRequest,
  moneyRequest,
  urgency,
  officialClaim,
  newNumber,

  /// A scheme promising a return: "invest N50k, get N150k", "double your money", "guaranteed profit".
  investmentReturn,

  /// "Call" (or WhatsApp) a phone number about account or card trouble: fraud, blocked, unusual activity.
  callAboutAccount,
}

/// A level and its fixed reason, as decided by the rules.
class Decision {
  const Decision(this.level, this.reason);

  final Level level;
  final ReasonCode? reason;

  @override
  bool operator ==(Object other) => other is Decision && other.level == level && other.reason == reason;

  @override
  int get hashCode => Object.hash(level, reason);

  @override
  String toString() => 'Decision($level, $reason)';
}

RegExp _words(List<String> w) => RegExp('\\b(${w.join('|')})\\b', caseSensitive: false);

abstract final class ConcreteSigns {
  static final _secret = _words(['otp', 'pin', 'bvn', 'nin', 'password', 'code', 'token', 'cvv', 'card number', 'card details']);
  static final _handOver = _words(['send', 'share', 'give', 'forward', 'tell', 'confirm', 'provide', 'enter', 'type', 'reply', 'drop']);
  static final _negated = RegExp(
    r"\b(do not|don't|dont|never|no)\s+(send|share|give|forward|tell|disclose|provide|reply|enter|type)\b",
    caseSensitive: false,
  );

  static final _pay = _words(['send', 'pay', 'transfer', 'deposit', 'remit', 'credit', 'invest']);
  static final _amount = RegExp(r'(₦|\bN\s?\d|\bNGN\s?\d|\bnaira\b|\d+\s?k\b|\bmoney\b|\bfee\b|\bcharge\b|\bcash\b)', caseSensitive: false);

  static final _urgency = _words(['urgent', 'urgently', 'now', 'immediately', 'today', 'quick', 'quickly', 'fast', 'within 24', 'sharp sharp', 'asap']);
  static final _official = _words([
    'dear customer', 'dear valued customer', 'valued customer', 'customer care', 'customer service',
    'customer support', 'cbn', 'efcc', 'central bank', 'government', 'ministry', 'our agent',
    'account officer', 'management', 'hr', 'fraud desk', 'fraud department', 'fraud unit', 'security team',
  ]);

  /// Money words for the investment pattern: N50,000, ₦50k, 50k, $200, 100,000 naira.
  static const _money = r'(?:₦\s?\d[\d,.]*k?|\bN\s?\d[\d,.]*k?|\bNGN\s?\d[\d,.]*|\$\s?\d[\d,.]*|\b\d[\d,.]*\s?(?:k|naira|usdt|dollars?)\b)';
  static final _investment = RegExp(
    r'\b(?:invest\w*|put|deposit\w*)\b[^.!?\n]{0,40}?' + _money +
        r'[^.!?\n]{0,60}?\b(?:get|got|receive|received|earn|earned|withdraw|withdrew|make|made|returns?)\b[^.!?\n]{0,30}?' + _money +
        r'|\b(?:double|triple)\s+(?:your|ur)\s+(?:money|investment|cash|capital)\b'
            r'|\bguaranteed\s+(?:profit|returns?|income|payout|interest)\b'
            r'|\b\d+\s?%\s+(?:daily|weekly|monthly)\s+(?:profit|returns?|roi|interest)\b',
    caseSensitive: false,
  );

  static final _callVerb = _words(['call', 'phone', 'contact', 'reach', 'dial', 'whatsapp', 'text']);

  /// Account or card trouble, not just the word "account": a real credit alert stays quiet.
  static final _accountTrouble = _words([
    'fraud', 'fraudulent', 'unusual', 'suspicious', 'unauthorised', 'unauthorized', 'compromised',
    'blocked', 'block', 'restricted', 'suspended', 'closure', 'closed', 'deactivated', 'frozen', 'hacked',
  ]);

  static final newNumber = RegExp(
    r"\b(new number|new line|friend'?s? (line|phone|number)|phone (fell|spoil|spoilt|got lost|was stolen)|lost my phone|don'?t call (the|my) old)",
    caseSensitive: false,
  );

  static Set<Sign> find(String text) {
    final flags = RedFlags.find(text);
    final asksHandOver = _handOver.hasMatch(text.replaceAll(_negated, ' '));
    return {
      if (flags.contains('link')) Sign.link,
      if (flags.contains('phone number')) Sign.phone,
      if (_secret.hasMatch(text) && asksHandOver) Sign.codeRequest,
      if (_pay.hasMatch(text) && _amount.hasMatch(text)) Sign.moneyRequest,
      if (_urgency.hasMatch(text)) Sign.urgency,
      if (_official.hasMatch(text)) Sign.officialClaim,
      if (newNumber.hasMatch(text)) Sign.newNumber,
      if (_investment.hasMatch(text)) Sign.investmentReturn,
      if (flags.contains('phone number') && _callVerb.hasMatch(text) && _accountTrouble.hasMatch(text)) Sign.callAboutAccount,
    };
  }

  /// What a model's SCAM becomes (kept for the Gemma step). Kept as SCAM only with a concrete
  /// sign; a money request alone becomes CAREFUL; anything else NONE.
  static Decision guardScam(Set<Sign> signs) {
    final money = signs.contains(Sign.moneyRequest);
    if (signs.contains(Sign.codeRequest)) return const Decision(Level.scam, ReasonCode.asksForCode);
    if (money && signs.contains(Sign.newNumber)) return const Decision(Level.scam, ReasonCode.moneyUnderPressure);
    if (money && signs.contains(Sign.officialClaim)) return const Decision(Level.scam, ReasonCode.fakeOfficial);
    if (money && signs.contains(Sign.urgency)) return const Decision(Level.scam, ReasonCode.moneyUnderPressure);
    if (signs.contains(Sign.investmentReturn)) return const Decision(Level.scam, ReasonCode.tooGoodOffer);
    if (signs.contains(Sign.link) || signs.contains(Sign.phone)) return const Decision(Level.scam, ReasonCode.linkOrNumber);
    if (money) return const Decision(Level.careful, ReasonCode.asksForMoneyOrLink);
    return const Decision(Level.none, null);
  }

  /// What a model's CAREFUL becomes: kept only with something concrete to be careful about.
  static Decision guardCareful(Set<Sign> signs) =>
      signs.any(_carefulSigns.contains) ? Decision(Level.careful, carefulReason(signs)) : const Decision(Level.none, null);

  static const _carefulSigns = {
    Sign.link, Sign.phone, Sign.codeRequest, Sign.moneyRequest, Sign.newNumber, Sign.investmentReturn, Sign.callAboutAccount,
  };

  /// Signs strong enough to make a message at least CAREFUL on their own: a promised investment
  /// return, or "call this number" about account trouble.
  static ReasonCode? floorReason(Set<Sign> signs) {
    if (signs.contains(Sign.callAboutAccount)) return ReasonCode.linkOrNumber;
    if (signs.contains(Sign.investmentReturn)) return ReasonCode.tooGoodOffer;
    return null;
  }

  /// A fixed reason for a CAREFUL.
  static ReasonCode carefulReason(Set<Sign> signs) {
    if (signs.contains(Sign.moneyRequest)) return ReasonCode.asksForMoneyOrLink;
    if (signs.contains(Sign.link) || signs.contains(Sign.phone)) return ReasonCode.linkOrNumber;
    return ReasonCode.warningSigns;
  }
}

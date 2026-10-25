// ignore_for_file: prefer_interpolation_to_compose_strings
import 'lexicon.dart';
import 'patterns.dart';
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

  /// Arrest, warrant or "money laundering" threats (fake police, "digital arrest").
  threat,
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

abstract final class ConcreteSigns {
  static final _secret = words(secretWords);
  static final _handOver = words(handOverVerbs);

  /// "Do not share", and "साझा न करें" / "mat batana": a warning, not a request.
  static final negated = RegExp(
    r"\b(?:do not|don't|dont|never|no)\s+(?:send|share|give|forward|tell|disclose|provide|reply|enter|type)\b"
    '|${words(hindiNegations).pattern}\\s+${words(hindiVerbs).pattern}',
    caseSensitive: false,
    unicode: true,
  );

  static final _pay = words(payVerbs);
  static final _amount = RegExp('(?:$amountPattern)|${words(hindiAmountWords).pattern}', caseSensitive: false, unicode: true);

  static final _urgency = words(urgencyWords);
  static final _official = words(officialWords);

  /// Money words for the investment pattern: N50,000, ₦50k, 50k, $200, ₹10,000, Rs 5000, 2 lakh.
  static const _money =
      r'(?:₦\s?\d[\d,.]*k?|₹\s?\d[\d,.]*k?|\bN\s?\d[\d,.]*k?|\bNGN\s?\d[\d,.]*|\bRs\.?\s?\d[\d,.]*|\bINR\s?\d[\d,.]*|\$\s?\d[\d,.]*'
      r'|\b\d[\d,.]*\s?(?:k|naira|usdt|dollars?|lakh|crore|rupees?)\b)';
  static final _investment = RegExp(
    r'\b(?:invest\w*|put|deposit\w*)\b[^.!?\n]{0,40}?' + _money +
        r'[^.!?\n]{0,60}?\b(?:get|got|receive|received|earn|earned|withdraw|withdrew|make|made|returns?)\b[^.!?\n]{0,30}?' + _money +
        r'|\b(?:double|triple)\s+(?:your|ur)\s+(?:money|investment|cash|capital)\b'
            r'|\bguaranteed\s+(?:profit|returns?|income|payout|interest)\b'
            r'|\b\d+\s?%\s+(?:daily|weekly|monthly)\s+(?:profit|returns?|roi|interest)\b'
            r'|\b(?:paisa|paise|money)\s+double\b|पैसा डबल|पैसे डबल|दोगुना',
    caseSensitive: false,
    unicode: true,
  );

  static final _callVerb = words(callVerbs);
  static final _accountTrouble = words(accountTroubleWords);
  static final _threat = words(threatWords);

  static final newNumber = wholeWord(newNumberPattern);

  static Set<Sign> find(String text) {
    final flags = RedFlags.find(text);
    final asksHandOver = _handOver.hasMatch(text.replaceAll(negated, ' '));
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
      if (_threat.hasMatch(text)) Sign.threat,
    };
  }

  /// What a model's SCAM becomes. Kept as SCAM only with a concrete sign; a money request
  /// alone becomes CAREFUL; anything else NONE.
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

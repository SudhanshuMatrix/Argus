/// SCAM: loud alert. CAREFUL: quiet alert. OTP: "never share this code". NONE: stay silent.
enum Level { scam, careful, otp, none }

/// Why a rule fired. The app turns each into a fixed English or Hindi sentence, so the parent
/// never sees machine-written words.
enum ReasonCode {
  asksForCode,
  accountProblem,
  tooGoodOffer,
  fakeOfficial,
  asksForMoneyOrLink,
  pressure,

  /// Account problem or a promised prize/job/loan, plus a request to act.
  accountOrOffer,

  /// Money asked for in a hurry, or from a "new number".
  moneyUnderPressure,

  /// A link or a phone number the reader is meant to use.
  linkOrNumber,

  /// Safety floor: no rule decided, but two or more red flags were found.
  warningSigns,
}

/// The level and the fixed reason behind it. [reasonCode] is null only for NONE and OTP.
class Verdict {
  const Verdict(this.level, {this.reasonCode});

  final Level level;
  final ReasonCode? reasonCode;

  @override
  bool operator ==(Object other) => other is Verdict && other.level == level && other.reasonCode == reasonCode;

  @override
  int get hashCode => Object.hash(level, reasonCode);

  @override
  String toString() => 'Verdict($level, $reasonCode)';
}

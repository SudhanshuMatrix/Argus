import 'concrete_signs.dart';
import 'hard_rules.dart';

/// What kind of secret a message asks for.
enum Secret { bvn, nin, otp, pin, password, card, code }

/// The account trouble a message claims.
enum AccountTrouble { blocked, suspended, restricted, closed }

enum FindingKind { asksForSecret, link, accountProblem, verifyAccount, newNumber, prize, moneyRequest, officialClaim, phoneNumber, urgency }

/// A concrete thing found in a message. The app turns the most important two into a fixed
/// English or Hindi sentence ("It has a link and says your account is blocked"), so the
/// parent sees why without any machine-written words.
class Finding {
  const Finding(this.kind, {this.secret, this.trouble});

  const Finding.asksForSecret(Secret this.secret) : kind = FindingKind.asksForSecret, trouble = null;
  const Finding.accountProblem(AccountTrouble this.trouble) : kind = FindingKind.accountProblem, secret = null;

  static const link = Finding(FindingKind.link);
  static const verifyAccount = Finding(FindingKind.verifyAccount);
  static const newNumber = Finding(FindingKind.newNumber);
  static const prize = Finding(FindingKind.prize);
  static const moneyRequest = Finding(FindingKind.moneyRequest);
  static const officialClaim = Finding(FindingKind.officialClaim);
  static const phoneNumber = Finding(FindingKind.phoneNumber);
  static const urgency = Finding(FindingKind.urgency);

  final FindingKind kind;
  final Secret? secret;
  final AccountTrouble? trouble;

  @override
  bool operator ==(Object other) => other is Finding && other.kind == kind && other.secret == secret && other.trouble == trouble;

  @override
  int get hashCode => Object.hash(kind, secret, trouble);

  @override
  String toString() => 'Finding(${kind.name}${secret != null ? ', ${secret!.name}' : ''}${trouble != null ? ', ${trouble!.name}' : ''})';
}

RegExp _words(List<String> w) => RegExp('\\b(${w.join('|')})\\b', caseSensitive: false);

bool _fullMatch(RegExp re, String s) {
  final m = re.matchAsPrefix(s);
  return m != null && m.end == s.length;
}

abstract final class Findings {
  static final _trouble = [
    (AccountTrouble.blocked, _words(['blocked', 'block'])),
    (AccountTrouble.suspended, _words(['suspended', 'suspend'])),
    (AccountTrouble.restricted, _words(['restricted', 'restrict'])),
    (AccountTrouble.closed, _words(['deactivated', 'closed', 'disabled'])),
  ];
  static final _verify = _words(['verify', 'verification', 'update your account', 'update your bvn', 'update your details', 'upgrade']);
  static final _prize = _words(['you have won', 'you won', 'winner', 'prize', 'promo', 'congratulations', 'job offer', 'loan', 'grant', 'investment', 'double your']);
  static final _secretWords = [
    (Secret.bvn, _words(['bvn'])),
    (Secret.nin, _words(['nin'])),
    (Secret.otp, _words(['otp', 'token'])),
    (Secret.pin, _words(['pin'])),
    (Secret.password, _words(['password'])),
    (Secret.card, _words(['card number', 'card details', 'cvv'])),
    (Secret.code, _words(['code'])),
  ];

  /// In order of importance; the reason uses the first two. Empty when nothing was found.
  static List<Finding> find(String text) {
    final signs = ConcreteSigns.find(text);
    final secret = _secret(text, signs.contains(Sign.codeRequest));
    final trouble = _trouble.where((t) => t.$2.hasMatch(text)).firstOrNull;
    return [
      if (secret != null) Finding.asksForSecret(secret),
      if (signs.contains(Sign.link)) Finding.link,
      if (trouble != null) Finding.accountProblem(trouble.$1),
      if (_verify.hasMatch(text)) Finding.verifyAccount,
      if (signs.contains(Sign.newNumber)) Finding.newNumber,
      if (_prize.hasMatch(text) || signs.contains(Sign.investmentReturn)) Finding.prize,
      if (signs.contains(Sign.moneyRequest)) Finding.moneyRequest,
      if (signs.contains(Sign.officialClaim)) Finding.officialClaim,
      if (signs.contains(Sign.phone)) Finding.phoneNumber,
      if (signs.contains(Sign.urgency)) Finding.urgency,
    ];
  }

  /// The secret asked for: the hard rule's exact one, else any secret word in a looser request.
  static Secret? _secret(String text, bool looseRequest) {
    final word = HardRules.requestedSecret(text);
    if (word != null) return _secretWords.firstWhere((s) => _fullMatch(s.$2, word)).$1;
    if (!looseRequest) return null;
    return _secretWords.where((s) => s.$2.hasMatch(text)).firstOrNull?.$1;
  }
}

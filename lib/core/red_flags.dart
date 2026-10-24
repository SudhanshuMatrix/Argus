/// Warning signs. They never decide on their own: two or more only set the safety floor
/// in ScamChecker. Grow the keyword list from real scam samples.
abstract final class RedFlags {
  static final _link = RegExp(r'https?://|www\.|bit\.ly|wa\.me|tinyurl', caseSensitive: false);

  /// Nigerian mobile numbers: 080x, 070x, 090x, 081x, 091x... or +234 / 234 forms.
  static final _phone = RegExp(r'(\+?234|0)[789][01]\d{8}');

  /// Spaces or dashes between digits ("0801 234 5678") are removed before the phone check.
  static final _digitSeparator = RegExp(r'(?<=\d)[\s-](?=\d)');

  static final _keywords = [
    'bvn', 'nin', 'blocked', 'suspended', 'restricted', 'deactivated', 'upgrade',
    'reversal', 'refund', 'winner', 'you have won', 'prize', 'promo', 'congratulations',
    'verify', 'update your', 'click', 'pin', 'password', 'urgent', 'urgently', 'within 24',
    'loan', 'investment', 'double your', 'crypto', 'job offer', 'customer care',
    'cbn', 'efcc', 'grant', 'dollar', 'otp',
  ].map((k) => (k, RegExp('\\b${RegExp.escape(k)}\\b'))).toList(); // "pin" must not match "shopping"

  static bool hasLink(String text) => _link.hasMatch(text);

  static List<String> find(String text) {
    final lower = text.toLowerCase();
    return [
      if (_link.hasMatch(text)) 'link',
      if (_phone.hasMatch(text.replaceAll(_digitSeparator, ''))) 'phone number',
      for (final (word, re) in _keywords)
        if (re.hasMatch(lower)) word,
    ];
  }
}

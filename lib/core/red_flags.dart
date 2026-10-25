import 'patterns.dart';

/// Warning signs. They never decide on their own: two or more only set the safety floor
/// in ScamChecker. Grow the keyword list from real scam samples.
abstract final class RedFlags {
  static final _link = RegExp(r'https?://|www\.|bit\.ly|wa\.me|tinyurl', caseSensitive: false);

  /// Nigerian mobile numbers (080x, 070x, 090x, 081x, 091x... or +234 / 234 forms) and Indian
  /// mobile numbers (10 digits starting 6-9, optionally +91 / 91 / 0), not part of a longer number.
  static final _phone = RegExp(r'(\+?234|0)[789][01]\d{8}|(?<!\d)(?:\+?91|0)?[6-9]\d{9}(?!\d)');

  /// Spaces or dashes between digits ("0801 234 5678", "98765 43210") are removed before the phone check.
  static final _digitSeparator = RegExp(r'(?<=\d)[\s-](?=\d)');

  static const _englishKeywords = [
    'bvn', 'nin', 'blocked', 'suspended', 'restricted', 'deactivated', 'upgrade',
    'reversal', 'refund', 'winner', 'you have won', 'prize', 'promo', 'congratulations',
    'verify', 'update your', 'click', 'pin', 'password', 'urgent', 'urgently', 'within 24',
    'loan', 'investment', 'double your', 'crypto', 'job offer', 'customer care',
    'cbn', 'efcc', 'grant', 'dollar', 'otp',
    // India
    'kyc', 'aadhaar', 'aadhar', 'pan card', 'upi pin', 'lottery', 'lucky draw', 'kbc', 'disconnected',
    'digital arrest', 'money laundering', 'customs', 'cbi', 'cashback', 'reward points', 'work from home',
  ];
  static final _keywords = [
    for (final k in _englishKeywords) (k, RegExp('\\b${RegExp.escape(k)}\\b')), // "pin" must not match "shopping"
  ];

  /// Hindi warning words, each reported under its English name so a message in Hindi
  /// counts the same as one in English.
  static final _hindiKeywords = [
    ('blocked', words(['ब्लॉक'])),
    ('kyc', words(['केवाईसी'])),
    ('otp', words(['ओटीपी'])),
    ('pin', words(['पिन'])),
    ('password', words(['पासवर्ड'])),
    ('click', words(['क्लिक'])),
    ('prize', words(['इनाम'])),
    ('lottery', words(['लॉटरी'])),
    ('congratulations', words(['बधाई हो'])),
    ('urgent', words(['तुरंत'])),
    ('aadhaar', words(['आधार'])),
    ('disconnected', words(['कनेक्शन काट', 'बिजली काट'])),
  ];

  static bool hasLink(String text) => _link.hasMatch(text);

  static List<String> find(String text) {
    final lower = text.toLowerCase();
    final found = <String>[
      if (_link.hasMatch(text)) 'link',
      if (_phone.hasMatch(text.replaceAll(_digitSeparator, ''))) 'phone number',
      for (final (word, re) in _keywords)
        if (re.hasMatch(lower)) word,
    ];
    for (final (word, re) in _hindiKeywords) {
      if (!found.contains(word) && re.hasMatch(text)) found.add(word);
    }
    return found;
  }
}

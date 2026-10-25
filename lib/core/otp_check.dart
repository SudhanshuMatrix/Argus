import 'concrete_signs.dart';

/// Spots a genuine one-time code so the parent gets "never share this code", not a scam alert.
abstract final class OtpCheck {
  static final _code = RegExp(r'\b\d{4,8}\b');
  static final _otpWords = RegExp(r'\b(otp|one[- ]time|verification code|token)\b|ओटीपी|वन टाइम पासवर्ड|सत्यापन कोड', caseSensitive: false, unicode: true);
  static final _askWords = RegExp(
    r'\b(send|share|give|forward|tell|bhejo|batao|bataiye)\b|भेजें|भेजो|भेजिए|बताएं|बताइए|बताओ|शेयर करें|साझा करें',
    caseSensitive: false,
    unicode: true,
  );

  /// Real banks say "Do not share this code" / "Never give it to anyone": that is not a request.
  static final _negatedAsk = RegExp(r"\b(do not|don't|dont|never|not to|no)\s+(send|share|give|forward|tell|disclose)\b", caseSensitive: false);

  /// A genuine OTP: a code and OTP words, but no link and nobody asking for it.
  static bool isRealOtp(String text) {
    if (!_otpWords.hasMatch(text) || !_code.hasMatch(text)) return false;
    final lower = text.toLowerCase();
    if (lower.contains('http') || lower.contains('www.')) return false;
    return !_askWords.hasMatch(text.replaceAll(_negatedAsk, ' ').replaceAll(ConcreteSigns.negated, ' '));
  }
}

import 'red_flags.dart';

/// Answers "is this obviously harmless?", never "does this look like a scam?".
abstract final class SkipFilter {
  static const minLength = 8;
  static const _shortLength = 40;

  static final _letterOrDigit = RegExp(r'[\p{L}\p{N}]', unicode: true);
  static final _digit = RegExp(r'\p{Nd}', unicode: true);

  /// True only for messages that clearly need no check.
  static bool isObviouslyHarmless(String text) {
    final t = text.trim();
    if (t.length < minLength) return true; // "ok", "Amen", "Thank you"
    if (!_letterOrDigit.hasMatch(t)) return true; // only emojis or symbols
    return t.length < _shortLength && !_digit.hasMatch(t) && RedFlags.find(t).isEmpty;
  }
}

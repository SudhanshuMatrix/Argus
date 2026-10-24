import 'findings.dart';
import 'highlights.dart';

bool _isHighSurrogate(int unit) => unit >= 0xD800 && unit <= 0xDBFF;

/// The start of a message for the warning screen: one line of spaces, at most [defaultMax] characters.
abstract final class MessagePreview {
  static const defaultMax = 120;

  /// Never longer than this, even to keep a dangerous part.
  static const hardMax = 200;
  static const fullMax = 4000;

  static final _spaces = RegExp(r'\s+');

  /// Like [of], but stretched (up to [hardMax]) so a dangerous part that starts early, such
  /// as the link at the end of a scam, isn't cut off.
  static String keepingMarks(String text, List<Finding> findings) {
    final flat = _flatten(text);
    final ends = Highlights.find(flat, findings).where((m) => m.start < hardMax).map((m) => m.end);
    final keepUntil = ends.isEmpty ? 0 : ends.reduce((a, b) => a > b ? a : b);
    final max = keepUntil.clamp(0, hardMax);
    return of(flat, max > defaultMax ? max : defaultMax);
  }

  /// The whole message for the warning screen: trimmed, line breaks kept, never past [fullMax].
  static String full(String text) {
    final t = text.trim();
    if (t.length <= fullMax) return t;
    var cut = fullMax;
    if (_isHighSurrogate(t.codeUnitAt(cut - 1))) cut--;
    return '${t.substring(0, cut).trimRight()}…';
  }

  static String _flatten(String text) => text.replaceAll(_spaces, ' ').trim();

  static String of(String text, [int max = defaultMax]) {
    final flat = _flatten(text);
    if (flat.length <= max) return flat;
    // Cut at a word boundary when one is near, so the preview doesn't end mid-word
    final space = flat.lastIndexOf(' ', max);
    var cut = space >= max - 20 ? space : max;
    // Never split an emoji (a surrogate pair) in half
    if (cut > 0 && _isHighSurrogate(flat.codeUnitAt(cut - 1))) cut--;
    return '${flat.substring(0, cut).trimRight()}…';
  }
}

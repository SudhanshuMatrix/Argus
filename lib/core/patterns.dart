/// Whole-word patterns that work for English, Hinglish and Hindi (Devanagari) alike.
///
/// Dart's `\b` only knows ASCII word characters, so it never matches next to a Hindi word.
/// These boundaries treat any letter, combining mark (Hindi vowel signs), digit or `_` as
/// part of a word.
const _before = r'(?<![\p{L}\p{M}\p{N}_])';
const _after = r'(?![\p{L}\p{M}\p{N}_])';

/// Any of [alternatives] as a whole word, ignoring case. Alternatives are regex fragments.
RegExp words(Iterable<String> alternatives) => RegExp('$_before(${alternatives.join('|')})$_after', caseSensitive: false, unicode: true);

/// [pattern] with whole-word boundaries on both ends, ignoring case.
RegExp wholeWord(String pattern) => RegExp('$_before(?:$pattern)$_after', caseSensitive: false, unicode: true);

/// True when [re] matches all of [s].
bool fullMatch(RegExp re, String s) {
  final m = re.matchAsPrefix(s);
  return m != null && m.end == s.length;
}

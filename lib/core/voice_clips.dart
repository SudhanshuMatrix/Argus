import 'language.dart';
import 'parent_title.dart';
import 'verdict.dart';

/// Names of the bundled clips in assets/voice: `{level}_{language}_{title}.mp3`, e.g.
/// `scam_en_mummy.mp3`. Only [recorded] languages ship clips: 3 levels × 4 titles = 12 files.
/// Hindi clips aren't recorded yet, so Hindi asks for its own clips first and then falls back
/// to English; adding `scam_hi_mummy.mp3` and friends (and listing them in [recorded]) is enough.
abstract final class VoiceClips {
  static const recorded = {Language.english};

  /// The clip for this parent, then the neutral one, in the parent's language and then in
  /// English. Missing clips are skipped by the player. Empty for NONE.
  static List<String> candidates(Level level, Language language, ParentTitle title) {
    final prefix = switch (level) {
      Level.scam => 'scam',
      Level.careful => 'careful',
      Level.otp => 'otp',
      Level.none => null,
    };
    if (prefix == null) return const [];
    final names = <String>[];
    for (final lang in {language, Language.english}) {
      if (!recorded.contains(lang)) continue;
      final base = '${prefix}_${lang.tag}';
      for (final name in ['${base}_${title.key}', '${base}_${ParentTitle.neutral.key}']) {
        if (!names.contains(name)) names.add(name);
      }
    }
    return names;
  }

  /// Every bundled clip name.
  static List<String> all() => [
    for (final level in [Level.scam, Level.careful, Level.otp])
      for (final language in recorded)
        for (final title in ParentTitle.values) candidates(level, language, title).first,
  ];
}

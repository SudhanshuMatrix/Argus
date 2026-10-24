/// Languages the app speaks. [tag] is the BCP 47 tag used for strings and voice clips.
enum Language {
  english('en'),
  hindi('hi');

  const Language(this.tag);
  final String tag;

  /// Unknown or missing tags fall back to English.
  static Language fromTag(String? tag) =>
      Language.values.where((l) => l.tag == tag?.toLowerCase()).firstOrNull ?? Language.english;

  /// A comma-separated tag list ("hi", "en-IN,fr"). The first tag whose language we support
  /// wins; null when none is set or supported.
  static Language? fromLocaleTags(String? tags) {
    for (final raw in (tags ?? '').split(',')) {
      final base = raw.trim().split('-').first.split('_').first.toLowerCase();
      final match = Language.values.where((l) => l.tag == base).firstOrNull;
      if (match != null) return match;
    }
    return null;
  }
}

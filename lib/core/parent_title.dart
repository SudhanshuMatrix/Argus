/// How the app addresses the parent. Picks the voice clip set; NEUTRAL uses "Please stop".
enum ParentTitle {
  mummy('mummy'),
  daddy('daddy'),
  grandma('grandma'),
  neutral('neutral');

  const ParentTitle(this.key);
  final String key;

  /// Unknown or missing keys fall back to NEUTRAL.
  static ParentTitle fromKey(String? key) =>
      ParentTitle.values.where((t) => t.key == key?.toLowerCase()).firstOrNull ?? ParentTitle.neutral;
}

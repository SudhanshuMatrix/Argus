import 'concrete_signs.dart';
import 'findings.dart';
import 'lexicon.dart';
import 'patterns.dart';

/// What a marked stretch of the message is, for the screen reader ("Marked: BVN, link").
enum MarkKind { link, secret, account, money, newNumber, prize, phone }

class Mark {
  const Mark(this.start, this.end, this.kind);

  final int start;
  final int end;
  final MarkKind kind;

  @override
  bool operator ==(Object other) => other is Mark && other.start == start && other.end == end && other.kind == kind;

  @override
  int get hashCode => Object.hash(start, end, kind);

  @override
  String toString() => 'Mark($start, $end, ${kind.name})';
}


/// The exact dangerous parts of a message, to highlight on the warning screen: links, the
/// secret words asked for, account threats, money amounts, "new number" phrases, prizes and
/// phone numbers. Same patterns as the rules. Ranges never overlap.
abstract final class Highlights {
  static final _link = RegExp(r'(https?://\S+|www\.\S+|\b(bit\.ly|wa\.me|tinyurl\.com|t\.me)/\S*)', caseSensitive: false);
  static final _secret = words(['upi pin', 'mpin', 'bvn', 'nin', 'pin', 'otp', 'password', 'cvv', 'card number', 'card details', 'token', 'code', 'aadhaar', 'aadhar', 'ओटीपी', 'पिन', 'पासवर्ड', 'कोड']);
  static final _account = words([
    'blocked', 'block', 'suspended', 'restricted', 'deactivated', 'closed', 'verify', 'verification',
    'update your account', 'update your bvn', 'upgrade', 'kyc', 'disconnected', 'ब्लॉक', 'केवाईसी', 'बंद हो',
  ]);
  static final _money = RegExp(
    r'(₦\s?\d[\d,]*(\.\d+)?|₹\s?\d[\d,]*(\.\d+)?|\bN\s?\d[\d,]*(\.\d+)?\b|\bNGN\s?\d[\d,]*\b|\bRs\.?\s?\d[\d,]*(\.\d+)?|\bINR\s?\d[\d,]*'
    r'|\b\d[\d,]*(\.\d+)?\s?(naira|k|lakh|crore|rupees?|rupaye)\b|\d[\d,]*\s?(रुपये|रुपए|लाख))',
    caseSensitive: false,
    unicode: true,
  );
  static final _prize = wholeWord(prizePattern);
  static final _phone = RegExp(r'(\+?234|\b0)[789][01]\d[\s-]?\d{3}[\s-]?\d{4}\b|(?<![\d+])(\+91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}(?!\d)');

  /// Marks for [text]; secret words only when the rules found a request for one.
  static List<Mark> find(String text, [List<Finding>? findings]) {
    final found = findings ?? Findings.find(text);
    Iterable<Mark> all(RegExp re, MarkKind kind) => re.allMatches(text).map((m) => Mark(m.start, m.end, kind));
    final candidates = <Mark>[
      for (final m in _link.allMatches(text)) Mark(m.start, _trimEnd(text, m.end), MarkKind.link),
      if (found.any((f) => f.kind == FindingKind.asksForSecret)) ...all(_secret, MarkKind.secret),
      ...all(_account, MarkKind.account),
      ...all(_money, MarkKind.money),
      ...all(ConcreteSigns.newNumber, MarkKind.newNumber),
      ...all(_prize, MarkKind.prize),
      ...all(_phone, MarkKind.phone),
    ];
    // Earliest first, longest first; drop anything overlapping a mark already taken
    candidates.sort((a, b) => a.start != b.start ? a.start.compareTo(b.start) : (b.end - b.start).compareTo(a.end - a.start));
    final taken = <Mark>[];
    for (final m in candidates) {
      if (m.end > m.start && !taken.any((t) => t.start < m.end && m.start < t.end)) taken.add(m);
    }
    return taken;
  }

  /// A link at the end of a sentence shouldn't swallow the full stop.
  static int _trimEnd(String text, int end) {
    var e = end;
    while (e > 0 && '.,;:!?)"\'…'.contains(text[e - 1])) {
      e--;
    }
    return e;
  }
}

import 'verdict.dart';

/// At most one spoken "be careful" per chat every [window], so a busy group can't keep the
/// phone talking. SCAM and OTP always speak. Remembers a hash of the chat and a time, in memory only.
class CarefulVoiceLimit {
  CarefulVoiceLimit({this.window = defaultWindow, this.capacity = 100});

  static const defaultWindow = Duration(minutes: 10);

  final Duration window;
  final int capacity;
  final _lastSpoken = <int, DateTime>{}; // insertion-ordered

  /// True when the voice may play for [level] now. A null [chatId] (manual checks, tests) always may.
  bool allow(Level level, int? chatId, DateTime now) {
    if (level != Level.careful || chatId == null) return true;
    final last = _lastSpoken.remove(chatId);
    if (last != null && now.difference(last) < window) {
      _lastSpoken[chatId] = last;
      return false;
    }
    _lastSpoken[chatId] = now;
    if (_lastSpoken.length > capacity) _lastSpoken.remove(_lastSpoken.keys.first);
    return true;
  }
}

/// WhatsApp re-posts the same notification as messages stack up. Remembers recent message
/// hashes in memory only (never the text), oldest dropped after [capacity].
class Deduper {
  Deduper({this.capacity = 200});

  final int capacity;
  final _seen = <int>{}; // insertion-ordered

  bool isNew(String text) {
    final key = text.trim().toLowerCase().hashCode;
    if (_seen.remove(key)) {
      _seen.add(key); // refresh: a repeat keeps it remembered
      return false;
    }
    _seen.add(key);
    if (_seen.length > capacity) _seen.remove(_seen.first);
    return true;
  }
}

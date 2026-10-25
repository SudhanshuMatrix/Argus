import 'dart:ui';

import '../core/alert.dart';
import '../core/memory_limits.dart';
import '../core/scam_checker.dart';
import 'edition.dart';
import 'gemma.dart';
import '../l10n/gen/app_localizations.dart';
import 'alert_bus.dart';
import 'notifier.dart';
import 'settings.dart';
import 'voice.dart';

/// Checks one incoming message and warns. Runs in whichever isolate received the message.
/// The text is only held in memory: never logged, stored, or put in a notification.
abstract final class Guard {
  static final _deduper = Deduper();
  static final _carefulLimit = CarefulVoiceLimit();
  static final _voice = VoicePlayer();
  static final _store = SettingsStore();
  static int _lastId = 0;

  /// The alert when the message needed one, else null. [test] alerts skip the duplicate check.
  static Future<(int, Alert)?> handle(IncomingMessage message, {bool test = false}) async {
    try {
      if (!test && !_deduper.isNew(message.text)) return null;
      // Hard-rule scams are decided instantly; the model only sees what the rules can't decide
      final model = Edition.current.usesModel ? await GemmaModel.available() : null;
      final result = await ScamChecker.checkWithModel(message.text, '${message.type.label} from ${message.sender}', model);
      final plan = AlertPlan.forLevel(result.level);
      if (plan == null) return null;

      final settings = await _store.load();
      final l = lookupAppLocalizations(Locale(settings.language.tag));
      var alert = Alert.from(message, result);
      if (test) alert = Alert(verdict: alert.verdict, findings: alert.findings, source: alert.source, sender: alert.sender, preview: alert.preview, message: alert.message);
      final id = _nextId();

      AlertBus.publish(id, alert);
      await Notifier.show(id, alert, settings, l);
      if (_carefulLimit.allow(alert.level, alert.chatId, DateTime.now())) {
        await _voice.play(alert.level, settings.language, settings.title, loud: settings.speakOnSilent, l: l, customName: settings.customName);
      }
      if (plan.familySms) await FamilySms.send(settings, _store);
      return (id, alert);
    } catch (_) {
      // Never crash in the background; the next message is checked as usual
      return null;
    }
  }

  static int _nextId() {
    var id = DateTime.now().millisecondsSinceEpoch ~/ 1000 % 0x3FFFFFFF;
    if (id <= _lastId) id = _lastId + 1;
    return _lastId = id;
  }
}

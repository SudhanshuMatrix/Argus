import 'dart:ui';

import 'package:firebase_messaging/firebase_messaging.dart';

import '../core/alert.dart';
import '../core/message_preview.dart';
import '../core/scam_checker.dart';
import '../core/verdict.dart';
import '../l10n/gen/app_localizations.dart';
import 'alert_bus.dart';
import 'firebase_config.dart';
import 'notifier.dart';
import 'settings.dart';
import 'voice.dart';

/// Live scam alerts: the Argus server sends one message to every phone (FCM topic), and each
/// phone shows the red full-screen warning straight away. Online and Demo builds only.
///
/// The server sends data-only, high-priority messages: {type: "live", id, message}. The phone
/// never sends anything back except Firebase's own registration.
abstract final class LiveAlerts {
  static final _voice = VoicePlayer();
  static final _shown = <String>{};

  /// Call once from main(), before runApp. Never throws.
  static Future<bool> start() async {
    if (!await FirebaseConfig.initialize()) return false;
    try {
      FirebaseMessaging.onBackgroundMessage(onLiveAlertInBackground);
      FirebaseMessaging.onMessage.listen((m) => show(m.data));
      await FirebaseMessaging.instance.subscribeToTopic(FirebaseConfig.topic);
      return true;
    } catch (_) {
      return false;
    }
  }

  static bool get configured => FirebaseConfig.configured;

  /// Shows one live alert: the red warning (notification over the lock screen, and the
  /// warning screen when the app is open) and the spoken warning.
  static Future<void> show(Map<String, dynamic> data) async {
    try {
      if (data['type'] != 'live') return;
      final text = (data['message'] as String? ?? '').trim();
      final key = data['id'] as String? ?? text;
      if (text.isEmpty || !_shown.add(key)) return;
      final rules = ScamChecker.check(text);
      final alert = Alert(
        verdict: Verdict(Level.scam, reasonCode: rules.verdict.reasonCode),
        findings: rules.findings,
        source: SourceType.live,
        sender: 'Argus',
        preview: MessagePreview.keepingMarks(text, rules.findings),
        message: MessagePreview.full(text),
      );
      final settings = await SettingsStore().load();
      final l = lookupAppLocalizations(Locale(settings.language.tag));
      final id = key.hashCode & 0x3FFFFFFF;
      AlertBus.publish(id, alert);
      await Notifier.show(id, alert, settings, l);
      await _voice.play(Level.scam, settings.language, settings.title, loud: true, l: l, customName: settings.customName);
    } catch (_) {
      // A broken alert must never crash the app
    }
  }
}

/// A live alert that arrived while the app wasn't open.
@pragma('vm:entry-point')
Future<void> onLiveAlertInBackground(RemoteMessage message) async {
  DartPluginRegistrant.ensureInitialized();
  await FirebaseConfig.initialize();
  await Notifier.init();
  await LiveAlerts.show(message.data);
}

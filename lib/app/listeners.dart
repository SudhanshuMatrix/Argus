import 'dart:ui';

import 'package:another_telephony/telephony.dart';
import 'package:flutter_notification_listener/flutter_notification_listener.dart';

import '../core/alert.dart';
import '../l10n/gen/app_localizations.dart';
import 'guard.dart';
import 'notifier.dart';
import 'settings.dart';

/// The chat apps whose notifications are checked.
const watchedPackages = {'com.whatsapp', 'com.whatsapp.w4b'};

/// Android's FLAG_GROUP_SUMMARY: "5 new messages" summaries carry no message of their own.
const _flagGroupSummary = 0x200;

bool _backgroundReady = false;

Future<void> _ensureBackground() async {
  if (_backgroundReady) return;
  DartPluginRegistrant.ensureInitialized();
  await Notifier.init();
  _backgroundReady = true;
}

/// An SMS that arrived while the app's screen wasn't running.
@pragma('vm:entry-point')
Future<void> onBackgroundSms(SmsMessage sms) async {
  await _ensureBackground();
  await Guard.handle(IncomingMessage(SourceType.sms, sms.body ?? '', sms.address));
}

/// An SMS that arrived while the app was open.
void onForegroundSms(SmsMessage sms) {
  Guard.handle(IncomingMessage(SourceType.sms, sms.body ?? '', sms.address));
}

/// Every posted notification, in the listener service's own isolate. Only WhatsApp is checked.
@pragma('vm:entry-point')
Future<void> onNotificationEvent(NotificationEvent evt) async {
  if (!watchedPackages.contains(evt.packageName)) return;
  if (((evt.flags ?? 0) & _flagGroupSummary) != 0) return;
  final text = evt.text ?? '';
  if (text.trim().isEmpty) return;
  await _ensureBackground();
  await Guard.handle(IncomingMessage(SourceType.whatsapp, text, evt.title, chatKey: '${evt.packageName}:${evt.title}'));
}

/// Starts SMS and WhatsApp checking. Safe to call again; never throws.
abstract final class Listeners {
  static Future<void> start() async {
    try {
      Telephony.instance.listenIncomingSms(onNewMessage: onForegroundSms, onBackgroundMessage: onBackgroundSms, listenInBackground: true);
    } catch (_) {}
    await startWhatsApp();
  }

  static Future<bool> hasWhatsAppAccess() async {
    try {
      return await NotificationsListener.hasPermission ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<void> openWhatsAppAccessSettings() async {
    try {
      await NotificationsListener.openPermissionSettings();
    } catch (_) {}
  }

  static Future<void> startWhatsApp() async {
    try {
      await NotificationsListener.initialize(callbackHandle: onNotificationEvent);
      if (!await hasWhatsAppAccess()) return;
      if (await NotificationsListener.isRunning ?? false) return;
      final s = await SettingsStore().load();
      final l = lookupAppLocalizations(Locale(s.language.tag));
      await NotificationsListener.startService(foreground: true, title: l.protectingPlain, description: l.homeProtectedDetail);
    } catch (_) {}
  }
}

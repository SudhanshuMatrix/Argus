import 'dart:typed_data';
import 'dart:ui';

import 'package:another_telephony/telephony.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../core/alert.dart';
import '../core/profile_form.dart';
import '../core/verdict.dart';
import '../l10n/gen/app_localizations.dart';
import '../ui/colors.dart';
import 'alert_codec.dart';
import 'alert_text.dart';
import 'settings.dart';

/// Posts the alert notifications. SCAM: loud, full screen (also over the lock screen), strong
/// vibration. CAREFUL and OTP: a quiet note. The text is fixed words only, never the message.
abstract final class Notifier {
  static final _plugin = FlutterLocalNotificationsPlugin();
  static bool _ready = false;

  static Future<void> init({void Function(NotificationResponse)? onTap}) async {
    try {
      await _plugin.initialize(
        settings: const InitializationSettings(android: AndroidInitializationSettings('ic_notification')),
        onDidReceiveNotificationResponse: onTap,
      );
      _ready = true;
    } catch (_) {
      _ready = false;
    }
  }

  static FlutterLocalNotificationsPlugin get plugin => _plugin;

  static AndroidFlutterLocalNotificationsPlugin? get android =>
      _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();

  /// The payload of the notification that opened the app, if one did.
  static Future<String?> launchPayload() async {
    try {
      final details = await _plugin.getNotificationAppLaunchDetails();
      return details?.didNotificationLaunchApp == true ? details?.notificationResponse?.payload : null;
    } catch (_) {
      return null;
    }
  }

  /// True when the notification was posted.
  static Future<bool> show(int id, Alert alert, ArgusSettings s, AppLocalizations l) async {
    if (!_ready) await init();
    final text = AlertText(l);
    final level = alert.level;
    final live = alert.source == SourceType.live;
    final headline = live ? l.liveAlertHeadline : text.headline(level, s.title, s.customName);
    final body = live
        ? [l.liveAlertBody, ?alert.preview, l.notificationTapToSee].join('\n')
        : [text.body(level, s.familyName), ?text.reason(alert.findings, alert.verdict.reasonCode), l.notificationTapToSee].join('\n');
    final scam = level == Level.scam;
    final details = AndroidNotificationDetails(
      live ? 'argus_live' : (scam ? 'argus_scam' : 'argus_careful'),
      live ? l.channelLive : (scam ? l.channelScam : l.channelCareful),
      importance: scam ? Importance.max : Importance.defaultImportance,
      priority: scam ? Priority.max : Priority.defaultPriority,
      category: scam ? AndroidNotificationCategory.alarm : AndroidNotificationCategory.message,
      fullScreenIntent: scam,
      visibility: NotificationVisibility.public,
      color: scam ? ArgusColors.alarm : ArgusColors.pine,
      enableVibration: true,
      vibrationPattern: scam ? Int64List.fromList([0, 900, 400, 900, 400, 900]) : null,
      styleInformation: BigTextStyleInformation(body),
      autoCancel: true,
    );
    try {
      await _plugin.show(
        id: id,
        title: headline,
        body: body,
        notificationDetails: NotificationDetails(android: details),
        payload: AlertCodec.toPayload(id, alert),
      );
      return true;
    } catch (_) {
      return false;
    }
  }
}

/// The optional SMS to one family member when a scam arrives. Always English, because it goes
/// to the family; never includes the message, who sent it or why it was flagged.
abstract final class FamilySms {
  static Future<void> send(ArgusSettings s, SettingsStore store) async {
    if (!s.familyAlerts || !PhoneNumbers.isValid(s.familyNumber)) return;
    final en = lookupAppLocalizations(const Locale('en'));
    final name = AlertText(en).titleName(s.title, s.customName);
    final message = name != null ? en.familySms(name) : en.familySmsNeutral;
    try {
      await Telephony.instance.sendSms(
        to: s.familyNumber,
        message: message,
        isMultipart: true,
        statusListener: (status) => store.setFamilySmsFailed(status != SendStatus.SENT && status != SendStatus.DELIVERED),
      );
    } catch (_) {
      await store.setFamilySmsFailed(true);
    }
  }
}

import 'package:permission_handler/permission_handler.dart';

import 'edition.dart';
import 'gemma.dart';
import 'listeners.dart';
import 'notifier.dart';

/// What Argus needs to protect the phone, checked fresh each time. Never throws.
class ProtectionStatus {
  const ProtectionStatus({required this.sms, required this.notifications, required this.whatsapp, required this.battery, this.model = false});

  final bool sms;
  final bool notifications;
  final bool whatsapp;
  final bool battery;

  /// The AI safety checker is installed (Offline and Online builds).
  final bool model;

  /// What this build needs to protect the phone. The Demo build only shows warnings.
  bool get allOn => Edition.current.checksMessages ? sms && notifications && whatsapp : notifications;

  static Future<ProtectionStatus> check() async {
    final checks = Edition.current.checksMessages;
    return ProtectionStatus(
      sms: checks && await _granted(Permission.sms),
      notifications: await _granted(Permission.notification),
      whatsapp: checks && await Listeners.hasWhatsAppAccess(),
      battery: await _granted(Permission.ignoreBatteryOptimizations),
      model: Edition.current.usesModel && await GemmaModel.isInstalled(),
    );
  }

  static Future<bool> _granted(Permission p) async {
    try {
      return await p.isGranted;
    } catch (_) {
      return false;
    }
  }
}

abstract final class Permissions {
  static Future<bool> requestSms() => _request(Permission.sms);

  static Future<bool> requestNotifications() async {
    final ok = await _request(Permission.notification);
    try {
      // Android 14+: the red warning over the lock screen needs its own switch
      await Notifier.android?.requestFullScreenIntentPermission();
    } catch (_) {}
    return ok;
  }

  static Future<bool> requestBattery() => _request(Permission.ignoreBatteryOptimizations);

  /// Android's App info page for Argus (permission_handler's [openAppSettings]).
  static Future<void> openSettings() async {
    try {
      await openAppSettings();
    } catch (_) {}
  }

  static Future<bool> _request(Permission p) async {
    try {
      final status = await p.request();
      if (status.isPermanentlyDenied) await openAppSettings();
      return status.isGranted;
    } catch (_) {
      return false;
    }
  }
}

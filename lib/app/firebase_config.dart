import 'package:firebase_core/firebase_core.dart';

import 'edition.dart';

/// Firebase settings for live scam alerts, passed at build time with --dart-define (see
/// docs/BACKEND.md). Without them the app builds and works; live alerts are just off.
abstract final class FirebaseConfig {
  static const _apiKey = String.fromEnvironment('ARGUS_FIREBASE_API_KEY');
  static const _appId = String.fromEnvironment('ARGUS_FIREBASE_APP_ID');
  static const _projectId = String.fromEnvironment('ARGUS_FIREBASE_PROJECT_ID');
  static const _senderId = String.fromEnvironment('ARGUS_FIREBASE_SENDER_ID');

  /// The FCM topic every phone listens on; the server sends to it.
  static const topic = 'argus-alerts';

  static bool get configured => Edition.current.receivesLiveAlerts && [_apiKey, _appId, _projectId, _senderId].every((v) => v.isNotEmpty);

  static FirebaseOptions get options => const FirebaseOptions(apiKey: _apiKey, appId: _appId, messagingSenderId: _senderId, projectId: _projectId);

  static Future<bool> initialize() async {
    if (!configured) return false;
    try {
      if (Firebase.apps.isEmpty) await Firebase.initializeApp(options: options);
      return true;
    } catch (_) {
      return false;
    }
  }
}

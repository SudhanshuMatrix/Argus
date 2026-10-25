import 'package:flutter/services.dart' show appFlavor;

/// Which APK this is. Set at build time with `flutter build apk --flavor <name>`.
enum Edition {
  /// No internet at all. Checks SMS and WhatsApp; Gemma ships inside the APK.
  offline,

  /// Checks SMS and WhatsApp, downloads Gemma once, and shows live scam alerts sent from the
  /// Argus server. The internet is only used for those two things; messages never leave the phone.
  online,

  /// For recording demos: only notifications and the full-screen warning, plus live alerts.
  /// No SMS, no WhatsApp reading, no model.
  demo;

  static Edition get current => switch (appFlavor) {
    'online' => Edition.online,
    'demo' => Edition.demo,
    _ => Edition.offline,
  };

  /// True when CI put Gemma inside the Offline APK (it needs the HF_TOKEN secret). An Offline
  /// APK built without it runs on the rules alone.
  static const modelBundled = bool.fromEnvironment('ARGUS_MODEL_BUNDLED');

  bool get checksMessages => this != Edition.demo;
  bool get receivesLiveAlerts => this != Edition.offline;
  bool get usesModel => this == Edition.online || (this == Edition.offline && modelBundled);
  bool get bundlesModel => this == Edition.offline;
}

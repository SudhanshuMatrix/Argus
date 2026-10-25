import 'package:flutter/material.dart';

import 'app/app_state.dart';
import 'app/live_alerts.dart';
import 'ui/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppState.instance.load();
  // Online and Demo builds: live scam alerts from the Argus server (no-op elsewhere)
  await LiveAlerts.start();
  runApp(const ArgusApp());
}

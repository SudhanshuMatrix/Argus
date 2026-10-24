import 'package:flutter/material.dart';

import 'app/app_state.dart';
import 'ui/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppState.instance.load();
  runApp(const ArgusApp());
}

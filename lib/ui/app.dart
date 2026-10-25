import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../app/alert_bus.dart';
import '../app/alert_codec.dart';
import '../app/app_state.dart';
import '../app/edition.dart';
import '../app/listeners.dart';
import '../app/notifier.dart';
import '../core/alert.dart';
import '../l10n/gen/app_localizations.dart';
import 'home_screen.dart';
import 'setup_screen.dart';
import 'theme.dart';
import 'warning_screen.dart';

final navigatorKey = GlobalKey<NavigatorState>();

/// Opens the warning screen for [alert] on top of whatever is showing.
void openWarning(Alert alert) {
  navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => WarningScreen(alert: alert)));
}

void _openFromPayload(String? payload) {
  final decoded = AlertCodec.decode(payload);
  if (decoded == null) return;
  final (id, fromPayload) = decoded;
  openWarning(AlertBus.resolve(id, fromPayload));
}

class ArgusApp extends StatefulWidget {
  const ArgusApp({super.key});

  @override
  State<ArgusApp> createState() => _ArgusAppState();
}

class _ArgusAppState extends State<ArgusApp> with WidgetsBindingObserver {
  final state = AppState.instance;
  AppLifecycleState _lifecycle = AppLifecycleState.resumed;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // A scam that arrives while the app is open shows its warning straight away
    AlertBus.listen((_, alert) {
      if (_lifecycle == AppLifecycleState.resumed) openWarning(alert);
    });
    _start();
  }

  Future<void> _start() async {
    await Notifier.init(onTap: (NotificationResponse r) => _openFromPayload(r.payload));
    final launch = await Notifier.launchPayload();
    if (launch != null) WidgetsBinding.instance.addPostFrameCallback((_) => _openFromPayload(launch));
    if (state.settings.setupComplete && Edition.current.checksMessages) await Listeners.start();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState s) => _lifecycle = s;

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: state,
    builder: (context, _) => MaterialApp(
      navigatorKey: navigatorKey,
      onGenerateTitle: (context) => 'Argus',
      debugShowCheckedModeBanner: false,
      theme: argusTheme(),
      locale: Locale(state.settings.language.tag),
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: state.settings.setupComplete ? const HomeScreen() : const SetupScreen(),
    ),
  );
}

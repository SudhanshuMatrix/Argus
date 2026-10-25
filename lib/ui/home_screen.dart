import 'package:flutter/material.dart';

import '../app/alert_text.dart';
import '../app/app_state.dart';
import '../app/edition.dart';
import '../app/gemma.dart';
import '../app/live_alerts.dart';
import '../app/listeners.dart';
import '../app/permissions.dart';
import '../l10n/gen/app_localizations.dart';
import 'about_screen.dart';
import 'check_screen.dart';
import 'colors.dart';
import 'settings_screen.dart';
import 'test_alert.dart';
import 'widgets.dart';

/// Says whether the phone is protected, and offers one "Turn on" for anything that's off.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  ProtectionStatus? _status;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Permissions are often changed in Android's Settings and then the parent comes back
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final status = await ProtectionStatus.check();
    if (status.whatsapp && Edition.current.checksMessages) await Listeners.startWhatsApp();
    await AppState.instance.load();
    if (mounted) setState(() => _status = status);
  }

  Future<void> _fix(Future<void> Function() action) async {
    await action();
    await _refresh();
  }

  Future<void> _open(Widget screen) => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => screen));

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final s = AppState.instance.settings;
    final status = _status;
    final name = AlertText(l).titleName(s.title, s.customName);
    final on = status?.allOn ?? true;
    final edition = Edition.current;
    return Scaffold(
      appBar: AppBar(title: const Text('Argus')),
      body: ArgusPage(
        bottom: FilledButton.icon(onPressed: () => _open(const CheckScreen()), icon: const Icon(Icons.search, size: 28), label: Text(l.homeCheck)),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: on ? ArgusColors.pine : ArgusColors.alarm, borderRadius: BorderRadius.circular(20)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(on ? Icons.verified_user : Icons.gpp_maybe, color: ArgusColors.paper, size: 48),
                const Gap(8),
                Text(
                  on ? (name != null ? l.homeProtected(name) : l.homeProtectedPlain) : l.homeOffTitle,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: ArgusColors.paper),
                ),
                const Gap(8),
                Text(edition == Edition.demo ? l.demoIntro : (on ? l.homeProtectedDetail : l.homeIntro), style: const TextStyle(color: ArgusColors.paper, fontSize: 18, height: 1.4)),
              ],
            ),
          ),
          if (s.familySmsFailed) ...[
            const Gap(),
            Card(child: Padding(padding: const EdgeInsets.all(16), child: Text(l.homeFamilySmsFailed, style: const TextStyle(fontSize: 18, color: ArgusColors.alarm)))),
          ],
          const Gap(),
          if (status != null)
            Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Column(
                  children: [
                    if (edition.checksMessages)
                      StatusRow(label: l.statusLabelSms, ok: status.sms, stateText: status.sms ? l.stateOn : l.stateOff, fixLabel: l.homeTurnOn, onFix: () => _fix(Permissions.requestSms)),
                    StatusRow(label: l.statusLabelNotifications, ok: status.notifications, stateText: status.notifications ? l.stateOn : l.stateOff, fixLabel: l.homeTurnOn, onFix: () => _fix(Permissions.requestNotifications)),
                    if (edition.checksMessages)
                      StatusRow(label: l.statusLabelWhatsapp, ok: status.whatsapp, stateText: status.whatsapp ? l.stateOn : l.stateOff, fixLabel: l.homeTurnOn, onFix: () => _fix(Listeners.openWhatsAppAccessSettings)),
                    StatusRow(label: l.statusLabelBattery, ok: status.battery, stateText: status.battery ? l.stateOn : l.stateOff, fixLabel: l.homeTurnOn, onFix: () => _fix(Permissions.requestBattery)),
                    if (edition.usesModel)
                      StatusRow(label: l.statusLabelModelShort, ok: status.model, stateText: status.model ? l.stateOn : l.stateOff, fixLabel: l.homeTurnOn, onFix: () => _fix(() => GemmaModel.install())),
                    if (edition.receivesLiveAlerts)
                      StatusRow(label: l.statusLabelLive, ok: LiveAlerts.configured, stateText: LiveAlerts.configured ? l.stateOn : l.statusLiveNotSetUp),
                  ],
                ),
              ),
            ),
          const Gap(),
          OutlinedButton(onPressed: () => sendTestScam(context), child: Text(l.setupTestButton)),
          const Gap(10),
          OutlinedButton(onPressed: () => _open(const SettingsScreen()).then((_) => _refresh()), child: Text(l.homeChangeSettings)),
          const Gap(10),
          OutlinedButton(onPressed: () => _open(const AboutScreen()), child: Text(l.aboutTitle)),
        ],
      ),
    );
  }
}

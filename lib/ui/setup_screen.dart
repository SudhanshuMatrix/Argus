import 'package:flutter/material.dart';

import '../app/app_state.dart';
import '../app/listeners.dart';
import '../app/permissions.dart';
import '../core/profile_form.dart';
import '../l10n/gen/app_localizations.dart';
import 'colors.dart';
import 'profile_fields.dart';
import 'test_alert.dart';
import 'widgets.dart';

enum _Step { welcome, who, sms, warnings, whatsapp, battery, maker, test }

/// The ten-minute setup the son or daughter does once: who it's for, every permission with a
/// plain reason, the phone-maker battery steps, and a real test alert.
class SetupScreen extends StatefulWidget {
  const SetupScreen({super.key});

  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> with WidgetsBindingObserver {
  var _step = _Step.welcome;
  late ProfileForm _form = AppState.instance.settings.form;
  bool _tried = false;
  bool _testSent = false;
  bool _testFailed = false;
  ProtectionStatus _status = const ProtectionStatus(sms: false, notifications: false, whatsapp: false, battery: false);

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
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final s = await ProtectionStatus.check();
    if (mounted) setState(() => _status = s);
  }

  Future<void> _run(Future<Object?> Function() action) async {
    await action();
    await _refresh();
  }

  Future<void> _next() async {
    if (_step == _Step.who) {
      setState(() => _tried = true);
      if (_form.problems().isNotEmpty) return;
      await AppState.instance.update(AppState.instance.settings.withForm(_form));
    }
    if (_step == _Step.whatsapp) await Listeners.startWhatsApp();
    if (_step == _Step.values.last) return _finish();
    setState(() => _step = _Step.values[_step.index + 1]);
  }

  Future<void> _finish() async {
    await Listeners.start();
    await AppState.instance.update(AppState.instance.settings.copyWith(setupComplete: true));
  }

  Future<void> _test() async {
    final ok = await sendTestScam(context);
    if (mounted) {
      setState(() {
        _testSent = ok;
        _testFailed = !ok;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final t = Theme.of(context).textTheme;
    final state = AppState.instance;
    final last = _step == _Step.values.last;

    Widget title(String s) => Padding(padding: const EdgeInsets.only(top: 8, bottom: 12), child: Text(s, style: t.headlineMedium));
    Widget para(String s) => Padding(padding: const EdgeInsets.only(bottom: 12), child: Text(s, style: t.bodyLarge));
    Widget action(String label, bool done, Future<Object?> Function() run) => Padding(
      padding: const EdgeInsets.only(top: 8, bottom: 8),
      child: done
          ? Row(children: [const Icon(Icons.check_circle, color: ArgusColors.pine, size: 32), const SizedBox(width: 10), Text(l.setupAllowed, style: t.titleMedium?.copyWith(color: ArgusColors.pine))])
          : FilledButton(onPressed: () => _run(run), child: Text(label, textAlign: TextAlign.center)),
    );

    final content = switch (_step) {
      _Step.welcome => [title(l.setupWelcomeTitle), para(l.homeIntro), para(l.aboutOffline)],
      _Step.who => [
        title(l.setupWhoTitle),
        ProfileFields(
          form: _form,
          onForm: (f) => setState(() => _form = f),
          language: state.settings.language,
          onLanguage: (lang) => state.setLanguage(lang).then((_) => setState(() {})),
          showProblems: _tried,
        ),
      ],
      _Step.sms => [
        title(l.setupSmsTitle),
        para(l.setupSmsWhy),
        if (_form.familyAlerts) para(l.setupSmsWhySend),
        action(l.setupSmsButton, _status.sms, Permissions.requestSms),
      ],
      _Step.warnings => [
        title(l.setupNotificationsTitle),
        para(l.setupNotificationsWhy),
        action(l.setupNotificationsButton, _status.notifications, Permissions.requestNotifications),
        const Gap(12),
        Text(l.setupFullScreenTitle, style: t.titleLarge),
        const Gap(8),
        para(l.setupFullScreenWhy),
      ],
      _Step.whatsapp => [
        title(l.setupAccessTitle),
        para(l.setupAccessWhy),
        action(l.setupAccessButton, _status.whatsapp, Listeners.openWhatsAppAccessSettings),
        if (!_status.whatsapp) ...[
          const Gap(12),
          Text(l.setupRestrictedTitle, style: t.titleLarge),
          const Gap(8),
          para(l.setupRestrictedWhy),
          para(l.setupRestrictedStep1),
          para(l.setupRestrictedStep2),
          para(l.setupRestrictedStep3),
          para(l.setupRestrictedStep4),
          OutlinedButton(onPressed: Permissions.openSettings, child: Text(l.setupOpenAppInfo)),
        ],
      ],
      _Step.battery => [title(l.setupBatteryTitle), para(l.setupBatteryWhy), action(l.setupBatteryButton, _status.battery, Permissions.requestBattery)],
      _Step.maker => [
        title(l.setupMakerTitle),
        para(l.setupMakerXiaomi),
        para(l.setupMakerXiaomiStep1),
        para(l.setupMakerXiaomiStep2),
        para(l.setupMakerXiaomiStep3),
        const Gap(8),
        para(l.setupMakerTranssion),
        para(l.setupMakerTranssionStep1),
        para(l.setupMakerTranssionStep2),
        OutlinedButton(onPressed: Permissions.openSettings, child: Text(l.setupOpenAppInfo)),
      ],
      _Step.test => [
        title(l.setupTestTitle),
        para(l.setupTestWhy),
        if (_form.familyAlerts && _form.familyName.trim().isNotEmpty) para(l.setupTestFamilyNote(_form.familyName.trim())),
        OutlinedButton(onPressed: _test, child: Text(_testSent ? l.setupTestAgain : l.setupTestButton)),
        if (_testFailed) ...[const Gap(), Text(l.setupTestFailed, style: const TextStyle(fontSize: 18, color: ArgusColors.alarm))],
      ],
    };

    return Scaffold(
      appBar: AppBar(
        title: Text(l.setupStep(_step.index + 1, _Step.values.length)),
        leading: _step == _Step.welcome ? null : IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => setState(() => _step = _Step.values[_step.index - 1])),
      ),
      body: ArgusPage(
        bottom: FilledButton(onPressed: _next, child: Text(_step == _Step.welcome ? l.setupStart : last ? l.setupFinish : l.setupNext)),
        children: [LinearProgressIndicator(value: (_step.index + 1) / _Step.values.length, color: ArgusColors.pine, minHeight: 8), const Gap(8), ...content],
      ),
    );
  }
}

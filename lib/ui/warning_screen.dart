import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../app/alert_text.dart';
import '../app/app_state.dart';
import '../app/voice.dart';
import '../core/alert.dart';
import '../core/highlights.dart';
import '../core/profile_form.dart';
import '../core/verdict.dart';
import '../l10n/gen/app_localizations.dart';
import 'colors.dart';
import 'widgets.dart';

/// The full-screen warning: red for SCAM, amber for CAREFUL, green for a real secret code.
/// Shows only fixed words, plus the message itself with the dangerous parts marked.
class WarningScreen extends StatefulWidget {
  const WarningScreen({super.key, required this.alert});

  final Alert alert;

  @override
  State<WarningScreen> createState() => _WarningScreenState();
}

class _WarningScreenState extends State<WarningScreen> {
  final _voice = VoicePlayer();

  @override
  void dispose() {
    _voice.stop();
    super.dispose();
  }

  Future<void> _call(String number) async {
    try {
      await launchUrl(Uri(scheme: 'tel', path: number.replaceAll(RegExp(r'[\s\-().]'), '')));
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final s = AppState.instance.settings;
    final text = AlertText(l);
    final alert = widget.alert;
    final level = alert.level;
    final (background, foreground, icon) = switch (level) {
      Level.scam => (ArgusColors.alarm, ArgusColors.paper, Icons.gpp_bad),
      Level.careful => (ArgusColors.amberTint, ArgusColors.ink, Icons.warning_amber_rounded),
      _ => (ArgusColors.pineTint, ArgusColors.ink, Icons.lock),
    };
    final from = text.from(alert.source, alert.sender);
    final reason = text.reason(alert.findings, alert.verdict.reasonCode);
    final advice = text.advice(alert.verdict.reasonCode);
    final message = alert.message;
    final family = s.familyName.trim();
    final canCall = PhoneNumbers.isValid(s.familyNumber);
    final onColor = TextStyle(color: foreground);

    Widget label(String t) => Padding(
      padding: const EdgeInsets.only(top: 18, bottom: 6),
      child: Text(t.toUpperCase(), style: TextStyle(color: foreground.withValues(alpha: 0.85), fontSize: 16, fontWeight: FontWeight.w700, letterSpacing: 1)),
    );

    return Scaffold(
      backgroundColor: background,
      body: ArgusPage(
        bottom: Column(
          children: [
            if (canCall)
              FilledButton.icon(
                style: FilledButton.styleFrom(backgroundColor: level == Level.scam ? ArgusColors.paper : ArgusColors.pine, foregroundColor: level == Level.scam ? ArgusColors.alarm : ArgusColors.paper),
                onPressed: () => _call(s.familyNumber),
                icon: const Icon(Icons.call, size: 28),
                label: Text(family.isEmpty ? l.warningCallFamily : l.warningCall(family)),
              ),
            const Gap(10),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(foregroundColor: foreground, side: BorderSide(color: foreground, width: 2)),
                    onPressed: () => _voice.play(level, s.language, s.title, loud: true, l: l, customName: s.customName),
                    child: Text(l.warningHearAgain, textAlign: TextAlign.center),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(foregroundColor: foreground, side: BorderSide(color: foreground, width: 2)),
                    onPressed: () => Navigator.of(context).maybePop(),
                    child: Text(l.warningUnderstand, textAlign: TextAlign.center),
                  ),
                ),
              ],
            ),
          ],
        ),
        children: [
          Icon(icon, size: 72, color: foreground),
          const Gap(8),
          Text(text.headline(level, s.title, s.customName), style: Theme.of(context).textTheme.headlineLarge?.merge(onColor)),
          const Gap(12),
          Text(text.body(level, family), style: Theme.of(context).textTheme.bodyLarge?.merge(onColor).copyWith(fontWeight: FontWeight.w600)),
          if (from != null) ...[const Gap(12), Text(from, style: Theme.of(context).textTheme.bodyLarge?.merge(onColor))],
          if (reason != null) ...[label(l.warningWhy), Text(reason, style: Theme.of(context).textTheme.bodyLarge?.merge(onColor))],
          if (advice != null) ...[label(l.warningWhatToDo), Text(advice, style: Theme.of(context).textTheme.bodyLarge?.merge(onColor))],
          if (message != null) ...[label(l.warningMessageLabel), MarkedMessage(text: message, marks: Highlights.find(message, alert.findings))],
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../app/alert_text.dart';
import '../app/app_state.dart';
import '../app/voice.dart';
import '../core/highlights.dart';
import '../core/scam_checker.dart';
import '../core/verdict.dart';
import '../l10n/gen/app_localizations.dart';
import 'colors.dart';
import 'widgets.dart';

/// Paste or type any message; Argus checks it on this phone.
class CheckScreen extends StatefulWidget {
  const CheckScreen({super.key});

  @override
  State<CheckScreen> createState() => _CheckScreenState();
}

class _CheckScreenState extends State<CheckScreen> {
  final _input = TextEditingController();
  final _voice = VoicePlayer();
  CheckResult? _result;
  String _checked = '';
  bool _empty = false;

  @override
  void dispose() {
    _input.dispose();
    _voice.stop();
    super.dispose();
  }

  void _check() {
    final text = _input.text.trim();
    FocusScope.of(context).unfocus();
    setState(() {
      _empty = text.isEmpty;
      _checked = text;
      _result = text.isEmpty ? null : ScamChecker.check(text);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final s = AppState.instance.settings;
    final text = AlertText(l);
    final result = _result;
    return Scaffold(
      appBar: AppBar(title: Text(l.checkTitle)),
      body: ArgusPage(
        children: [
          Text(l.checkIntro, style: Theme.of(context).textTheme.bodyLarge),
          const Gap(),
          TextField(
            controller: _input,
            minLines: 4,
            maxLines: 10,
            style: const TextStyle(fontSize: 20),
            decoration: InputDecoration(hintText: l.checkHint),
          ),
          const Gap(),
          FilledButton(onPressed: _check, child: Text(l.checkButton)),
          if (_empty) ...[const Gap(), Text(l.checkEmpty, style: const TextStyle(fontSize: 18, color: ArgusColors.alarm))],
          if (result != null) ...[
            const Gap(24),
            _ResultCard(
              level: result.level,
              headline: text.headline(result.level, s.title, s.customName),
              body: text.body(result.level, s.familyName),
              reason: text.reason(result.findings, result.verdict.reasonCode),
              advice: text.advice(result.verdict.reasonCode),
            ),
            const Gap(),
            MarkedMessage(text: _checked, marks: Highlights.find(_checked, result.findings)),
            if (result.level != Level.none) ...[
              const Gap(),
              OutlinedButton.icon(
                onPressed: () => _voice.play(result.level, s.language, s.title, loud: true, l: l, customName: s.customName),
                icon: const Icon(Icons.volume_up, size: 28),
                label: Text(l.checkHearIt),
              ),
            ],
          ],
        ],
      ),
    );
  }
}

class _ResultCard extends StatelessWidget {
  const _ResultCard({required this.level, required this.headline, required this.body, this.reason, this.advice});

  final Level level;
  final String headline;
  final String body;
  final String? reason;
  final String? advice;

  @override
  Widget build(BuildContext context) {
    final (bg, fg) = switch (level) {
      Level.scam => (ArgusColors.alarm, ArgusColors.paper),
      Level.careful => (ArgusColors.amberTint, ArgusColors.ink),
      Level.otp => (ArgusColors.pineTint, ArgusColors.ink),
      Level.none => (ArgusColors.paper, ArgusColors.ink),
    };
    final style = TextStyle(color: fg, fontSize: 20, height: 1.4);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(16), border: Border.all(color: level == Level.none ? ArgusColors.line : bg)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(headline, style: TextStyle(color: fg, fontSize: 28, fontWeight: FontWeight.w700, height: 1.2)),
          const Gap(10),
          Text(body, style: style.copyWith(fontWeight: FontWeight.w600)),
          if (reason != null) ...[const Gap(10), Text(reason!, style: style)],
          if (advice != null) ...[const Gap(10), Text(advice!, style: style)],
        ],
      ),
    );
  }
}

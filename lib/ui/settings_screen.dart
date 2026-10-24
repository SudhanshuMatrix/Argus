import 'package:flutter/material.dart';

import '../app/app_state.dart';
import '../core/language.dart';
import '../core/profile_form.dart';
import '../l10n/gen/app_localizations.dart';
import 'profile_fields.dart';
import 'widgets.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late ProfileForm _form = AppState.instance.settings.form;
  late bool _speakOnSilent = AppState.instance.settings.speakOnSilent;
  bool _tried = false;
  bool _saved = false;

  Future<void> _save() async {
    setState(() => _tried = true);
    if (_form.problems().isNotEmpty) return;
    final s = AppState.instance.settings.withForm(_form).copyWith(speakOnSilent: _speakOnSilent);
    await AppState.instance.update(s);
    if (mounted) setState(() => _saved = true);
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final state = AppState.instance;
    return Scaffold(
      appBar: AppBar(title: Text(l.settingsTitle)),
      body: ArgusPage(
        bottom: FilledButton(onPressed: _save, child: Text(_saved ? l.settingsSaved : l.settingsSave)),
        children: [
          ProfileFields(
            form: _form,
            onForm: (f) => setState(() {
              _form = f;
              _saved = false;
            }),
            language: state.settings.language,
            onLanguage: (Language lang) => state.setLanguage(lang).then((_) => setState(() {})),
            showProblems: _tried,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _speakOnSilent,
            onChanged: (v) => setState(() {
              _speakOnSilent = v;
              _saved = false;
            }),
            title: Text(l.settingsSpeakOnSilent, style: Theme.of(context).textTheme.bodyLarge),
          ),
        ],
      ),
    );
  }
}

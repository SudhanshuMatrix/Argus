import 'package:shared_preferences/shared_preferences.dart';

import '../core/language.dart';
import '../core/parent_title.dart';
import '../core/profile_form.dart';

/// Who is protected and how to warn them. Never holds any message text.
class ArgusSettings {
  const ArgusSettings({
    this.language = Language.english,
    this.title = ParentTitle.neutral,
    this.customName = '',
    this.familyName = '',
    this.familyNumber = '',
    this.familyAlerts = false,
    this.speakOnSilent = false,
    this.setupComplete = false,
    this.familySmsFailed = false,
  });

  final Language language;
  final ParentTitle title;
  final String customName;
  final String familyName;
  final String familyNumber;
  final bool familyAlerts;
  final bool speakOnSilent;
  final bool setupComplete;

  /// The last family SMS couldn't be sent (no airtime, no signal). Shown on the home screen.
  final bool familySmsFailed;

  ProfileForm get form => ProfileForm(
    // Before setup nothing is picked yet; their own name is saved as the neutral title
    title: customName.isNotEmpty || !setupComplete ? null : title,
    usesCustomName: customName.isNotEmpty,
    customName: customName,
    familyName: familyName,
    familyNumber: familyNumber,
    familyAlerts: familyAlerts,
  );

  ArgusSettings copyWith({Language? language, bool? speakOnSilent, bool? setupComplete, bool? familySmsFailed}) => ArgusSettings(
    language: language ?? this.language,
    title: title,
    customName: customName,
    familyName: familyName,
    familyNumber: familyNumber,
    familyAlerts: familyAlerts,
    speakOnSilent: speakOnSilent ?? this.speakOnSilent,
    setupComplete: setupComplete ?? this.setupComplete,
    familySmsFailed: familySmsFailed ?? this.familySmsFailed,
  );

  ArgusSettings withForm(ProfileForm form) => ArgusSettings(
    language: language,
    title: form.savedTitle,
    customName: form.savedCustomName,
    familyName: form.familyName.trim(),
    familyNumber: form.familyNumber.trim(),
    familyAlerts: form.familyAlerts,
    speakOnSilent: speakOnSilent,
    setupComplete: setupComplete,
    familySmsFailed: familySmsFailed,
  );
}

/// Reads and writes [ArgusSettings]. Uncached, so the background isolates that check messages
/// always see the latest choices from the app.
class SettingsStore {
  SettingsStore([SharedPreferencesAsync? prefs]) : _given = prefs;

  final SharedPreferencesAsync? _given;

  // Created on first use: it needs the platform plugin, which a background isolate registers first
  late final SharedPreferencesAsync _prefs = _given ?? SharedPreferencesAsync();

  static const _language = 'language';
  static const _title = 'parent_title';
  static const _customName = 'custom_name';
  static const _familyName = 'family_name';
  static const _familyNumber = 'family_number';
  static const _familyAlerts = 'family_alerts';
  static const _speakOnSilent = 'speak_on_silent';
  static const _setupComplete = 'setup_complete';
  static const _familySmsFailed = 'family_sms_failed';

  Future<ArgusSettings> load() async {
    try {
      final values = await _prefs.getAll();
      String str(String k) => values[k] is String ? values[k] as String : '';
      bool flag(String k) => values[k] == true;
      return ArgusSettings(
        language: Language.fromTag(values[_language] as String?),
        title: ParentTitle.fromKey(values[_title] as String?),
        customName: str(_customName),
        familyName: str(_familyName),
        familyNumber: str(_familyNumber),
        familyAlerts: flag(_familyAlerts),
        speakOnSilent: flag(_speakOnSilent),
        setupComplete: flag(_setupComplete),
        familySmsFailed: flag(_familySmsFailed),
      );
    } catch (_) {
      // Storage unavailable: protect with the defaults rather than fail
      return const ArgusSettings();
    }
  }

  Future<void> save(ArgusSettings s) async {
    try {
      await _save(s);
    } catch (_) {
      // Storage unavailable: keep the settings in memory for this session
    }
  }

  Future<void> _save(ArgusSettings s) async {
    await _prefs.setString(_language, s.language.tag);
    await _prefs.setString(_title, s.title.key);
    await _prefs.setString(_customName, s.customName);
    await _prefs.setString(_familyName, s.familyName);
    await _prefs.setString(_familyNumber, s.familyNumber);
    await _prefs.setBool(_familyAlerts, s.familyAlerts);
    await _prefs.setBool(_speakOnSilent, s.speakOnSilent);
    await _prefs.setBool(_setupComplete, s.setupComplete);
    await _prefs.setBool(_familySmsFailed, s.familySmsFailed);
  }

  Future<void> setFamilySmsFailed(bool failed) async {
    try {
      await _prefs.setBool(_familySmsFailed, failed);
    } catch (_) {}
  }
}

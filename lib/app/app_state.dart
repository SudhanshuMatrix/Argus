import 'package:flutter/foundation.dart';

import '../core/language.dart';
import 'settings.dart';

/// The settings the screens show, kept in sync with storage.
class AppState extends ChangeNotifier {
  AppState._();

  static final instance = AppState._();

  final store = SettingsStore();
  ArgusSettings _settings = const ArgusSettings();

  ArgusSettings get settings => _settings;

  Future<void> load() async {
    _settings = await store.load();
    notifyListeners();
  }

  Future<void> update(ArgusSettings s) async {
    _settings = s;
    notifyListeners();
    await store.save(s);
  }

  Future<void> setLanguage(Language language) => update(_settings.copyWith(language: language));
}

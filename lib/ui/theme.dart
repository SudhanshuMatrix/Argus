import 'package:flutter/material.dart';

import 'colors.dart';

/// Body text 18sp+, headlines 28sp+, buttons 56dp tall and full width.
ThemeData argusTheme() {
  const font = 'Atkinson';
  final scheme = ColorScheme.fromSeed(seedColor: ArgusColors.pine, primary: ArgusColors.pine, surface: ArgusColors.paper, error: ArgusColors.alarm);
  const text = TextTheme(
    headlineLarge: TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: ArgusColors.ink, height: 1.2),
    headlineMedium: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, color: ArgusColors.ink, height: 1.2),
    titleLarge: TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: ArgusColors.ink),
    titleMedium: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: ArgusColors.ink),
    bodyLarge: TextStyle(fontSize: 20, color: ArgusColors.ink, height: 1.4),
    bodyMedium: TextStyle(fontSize: 18, color: ArgusColors.slate, height: 1.4),
    labelLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
  );
  final buttonShape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(14));
  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    fontFamily: font,
    textTheme: text,
    scaffoldBackgroundColor: ArgusColors.mist,
    appBarTheme: const AppBarTheme(
      backgroundColor: ArgusColors.mist,
      foregroundColor: ArgusColors.ink,
      titleTextStyle: TextStyle(fontFamily: font, fontSize: 24, fontWeight: FontWeight.w700, color: ArgusColors.ink),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(60),
        backgroundColor: ArgusColors.pine,
        foregroundColor: ArgusColors.paper,
        textStyle: const TextStyle(fontFamily: font, fontSize: 20, fontWeight: FontWeight.w700),
        shape: buttonShape,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(60),
        foregroundColor: ArgusColors.pine,
        side: const BorderSide(color: ArgusColors.pine, width: 2),
        textStyle: const TextStyle(fontFamily: font, fontSize: 20, fontWeight: FontWeight.w700),
        shape: buttonShape,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: ArgusColors.paper,
      labelStyle: const TextStyle(fontSize: 18, color: ArgusColors.slate),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
    ),
    cardTheme: CardThemeData(
      color: ArgusColors.paper,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16), side: const BorderSide(color: ArgusColors.line)),
    ),
    switchTheme: SwitchThemeData(thumbColor: WidgetStateProperty.resolveWith((s) => s.contains(WidgetState.selected) ? ArgusColors.pine : null)),
  );
}

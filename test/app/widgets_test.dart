import 'package:argus/core/alert.dart';
import 'package:argus/core/scam_checker.dart';
import 'package:argus/l10n/gen/app_localizations.dart';
import 'package:argus/ui/check_screen.dart';
import 'package:argus/ui/theme.dart';
import 'package:argus/ui/warning_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _wrap(Widget child, {String locale = 'en'}) => MaterialApp(
  theme: argusTheme(),
  locale: Locale(locale),
  supportedLocales: AppLocalizations.supportedLocales,
  localizationsDelegates: const [AppLocalizations.delegate, GlobalMaterialLocalizations.delegate, GlobalWidgetsLocalizations.delegate, GlobalCupertinoLocalizations.delegate],
  home: child,
);

void main() {
  testWidgets('Check screen flags a pasted scam', (tester) async {
    await tester.pumpWidget(_wrap(const CheckScreen()));
    await tester.enterText(find.byType(TextField), 'Mummy please send me the OTP the bank sent you now');
    await tester.tap(find.text('Check'));
    await tester.pumpAndSettle();
    expect(find.text('Stop! This looks like a scam.'), findsOneWidget);
    expect(find.text('It asks for your OTP and rushes you to act fast.'), findsOneWidget);
  });

  testWidgets('Check screen never calls a message safe', (tester) async {
    await tester.pumpWidget(_wrap(const CheckScreen()));
    await tester.enterText(find.byType(TextField), 'Good morning mummy, we will visit this Sunday after church');
    await tester.tap(find.text('Check'));
    await tester.pumpAndSettle();
    expect(find.text('No warning signs found.'), findsOneWidget);
    expect(find.textContaining('safe'), findsNothing);
  });

  testWidgets('the warning screen speaks Hindi', (tester) async {
    const text = 'Dear customer, your BVN is blocked. Click bit.ly/x1abc to update today.';
    final alert = Alert.from(IncomingMessage(SourceType.sms, text, 'Bank'), ScamChecker.check(text));
    await tester.pumpWidget(_wrap(WarningScreen(alert: alert), locale: 'hi'));
    await tester.pumpAndSettle();
    expect(find.text('रुकिए! यह स्कैम लगता है।'), findsOneWidget);
    expect(find.text('भेजने वाला: Bank (SMS)'), findsOneWidget);
    expect(find.text('मैं समझ गया/गई'), findsOneWidget);
  });
}

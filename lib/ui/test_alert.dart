import 'package:flutter/widgets.dart';

import '../app/guard.dart';
import '../core/alert.dart';
import '../l10n/gen/app_localizations.dart';

/// Runs a pretend scam through the real alert path: check, notification, voice, warning screen
/// (and the family SMS when it is turned on). The warning opens through the alert bus.
Future<bool> sendTestScam(BuildContext context) async {
  final l = AppLocalizations.of(context);
  final result = await Guard.handle(IncomingMessage(SourceType.sms, l.setupTestMessage, l.setupTestSender), test: true);
  return result != null;
}

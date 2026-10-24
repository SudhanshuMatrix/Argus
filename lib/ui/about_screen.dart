import 'package:flutter/material.dart';

import '../l10n/gen/app_localizations.dart';
import 'widgets.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final t = Theme.of(context).textTheme;
    Widget h(String s) => Padding(padding: const EdgeInsets.only(top: 22, bottom: 8), child: Text(s, style: t.titleLarge));
    Widget p(String s) => Padding(padding: const EdgeInsets.only(bottom: 10), child: Text(s, style: t.bodyLarge));
    return Scaffold(
      appBar: AppBar(title: Text(l.aboutTitle)),
      body: ArgusPage(
        children: [
          p(l.aboutOffline),
          h(l.aboutHowTitle),
          p(l.aboutHowRead),
          p(l.aboutHowRules),
          p(l.aboutHowRulesOnly),
          p(l.aboutHowAlert),
          h(l.aboutPrivacyTitle),
          p(l.aboutPrivacy),
          h(l.aboutLicences),
          p(l.aboutBasedOn),
          p(l.aboutFont),
          p(l.aboutVoice),
          p(l.aboutVersion('0.1.0')),
        ],
      ),
    );
  }
}

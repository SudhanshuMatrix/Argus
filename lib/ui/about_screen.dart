import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;

import '../app/edition.dart';
import '../l10n/gen/app_localizations.dart';
import 'widgets.dart';

class AboutScreen extends StatefulWidget {
  const AboutScreen({super.key});

  @override
  State<AboutScreen> createState() => _AboutScreenState();
}

class _AboutScreenState extends State<AboutScreen> {
  String? _terms;

  Future<void> _toggleTerms() async {
    if (_terms != null) return setState(() => _terms = null);
    String text;
    try {
      text = await rootBundle.loadString('assets/gemma_terms.txt');
    } catch (_) {
      text = 'ai.google.dev/gemma/terms';
    }
    if (mounted) setState(() => _terms = text);
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final t = Theme.of(context).textTheme;
    final edition = Edition.current;
    Widget h(String s) => Padding(padding: const EdgeInsets.only(top: 22, bottom: 8), child: Text(s, style: t.titleLarge));
    Widget p(String s) => Padding(padding: const EdgeInsets.only(bottom: 10), child: Text(s, style: t.bodyLarge));
    return Scaffold(
      appBar: AppBar(title: Text(l.aboutTitle)),
      body: ArgusPage(
        children: [
          p(edition == Edition.offline ? l.aboutOffline : l.aboutOfflineOnline),
          h(l.aboutHowTitle),
          if (edition.checksMessages) ...[p(l.aboutHowRead), p(l.aboutHowRules)],
          if (edition.usesModel) p(l.aboutHowRulesOnly),
          p(l.aboutHowAlert),
          if (edition.receivesLiveAlerts) p(l.aboutLive),
          h(l.aboutPrivacyTitle),
          p(l.aboutPrivacy),
          h(l.aboutLicences),
          p(l.aboutBasedOn),
          if (edition.usesModel) ...[
            p(l.aboutModel),
            p(l.aboutGemmaTerms),
            OutlinedButton(onPressed: _toggleTerms, child: Text(_terms == null ? l.aboutTermsShow : l.aboutTermsHide)),
            if (_terms != null) ...[const Gap(12), SelectableText(_terms!, style: const TextStyle(fontSize: 16, height: 1.4))],
            const Gap(16),
          ],
          p(l.aboutFont),
          p(l.aboutVoice),
          p(l.aboutVersion('0.2.0')),
        ],
      ),
    );
  }
}

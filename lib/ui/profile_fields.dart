import 'package:flutter/material.dart';

import '../core/language.dart';
import '../core/parent_title.dart';
import '../core/profile_form.dart';
import '../l10n/gen/app_localizations.dart';
import 'colors.dart';
import 'widgets.dart';

/// Who this phone is for, the language, and who to call. Shared by setup and settings.
class ProfileFields extends StatelessWidget {
  const ProfileFields({
    super.key,
    required this.form,
    required this.onForm,
    required this.language,
    required this.onLanguage,
    required this.showProblems,
  });

  final ProfileForm form;
  final ValueChanged<ProfileForm> onForm;
  final Language language;
  final ValueChanged<Language> onLanguage;
  final bool showProblems;

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final problems = showProblems ? form.problems() : const <ProfileProblem>[];
    Widget problem(ProfileProblem p, String text) => problems.contains(p)
        ? Padding(padding: const EdgeInsets.only(top: 4, bottom: 8), child: Text(text, style: const TextStyle(color: ArgusColors.alarm, fontSize: 18, fontWeight: FontWeight.w600)))
        : const SizedBox.shrink();
    Widget section(String text) => Padding(padding: const EdgeInsets.only(top: 20, bottom: 10), child: Text(text, style: Theme.of(context).textTheme.titleLarge));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        section(l.setupWhoCallThem),
        for (final (title, label) in [(ParentTitle.mummy, l.titleMummy), (ParentTitle.daddy, l.titleDaddy), (ParentTitle.grandma, l.titleGrandma)])
          ChoiceRow(label: label, selected: !form.usesCustomName && form.title == title, onTap: () => onForm(form.copyWith(title: () => title, usesCustomName: false))),
        ChoiceRow(label: l.titleTheirName, selected: form.usesCustomName, onTap: () => onForm(form.copyWith(title: () => null, usesCustomName: true))),
        if (form.usesCustomName)
          TextFormField(
            initialValue: form.customName,
            decoration: InputDecoration(labelText: l.setupWhoCustomName),
            style: const TextStyle(fontSize: 20),
            textCapitalization: TextCapitalization.words,
            onChanged: (v) => onForm(form.copyWith(customName: v)),
          ),
        problem(ProfileProblem.noTitle, l.problemNoTitle),
        problem(ProfileProblem.noCustomName, l.problemNoCustomName),
        section(l.setupWhoLanguage),
        ChoiceRow(label: l.languageEnglish, selected: language == Language.english, onTap: () => onLanguage(Language.english)),
        ChoiceRow(label: l.languageHindi, selected: language == Language.hindi, onTap: () => onLanguage(Language.hindi)),
        const Gap(20),
        TextFormField(
          initialValue: form.familyName,
          decoration: InputDecoration(labelText: l.setupWhoFamilyName),
          style: const TextStyle(fontSize: 20),
          textCapitalization: TextCapitalization.words,
          onChanged: (v) => onForm(form.copyWith(familyName: v)),
        ),
        problem(ProfileProblem.noFamilyName, l.problemNoFamilyName),
        const Gap(12),
        TextFormField(
          initialValue: form.familyNumber,
          decoration: InputDecoration(labelText: l.setupWhoFamilyNumber),
          style: const TextStyle(fontSize: 20),
          keyboardType: TextInputType.phone,
          onChanged: (v) => onForm(form.copyWith(familyNumber: v)),
        ),
        problem(ProfileProblem.badNumber, l.problemBadNumber),
        const Gap(8),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: form.familyAlerts,
          onChanged: (v) => onForm(form.copyWith(familyAlerts: v)),
          title: Text(l.setupWhoFamilyAlerts, style: Theme.of(context).textTheme.bodyLarge),
          subtitle: Text(l.setupWhoFamilyAlertsHint, style: Theme.of(context).textTheme.bodyMedium),
        ),
      ],
    );
  }
}

import 'parent_title.dart';

enum ProfileProblem { noTitle, noCustomName, noFamilyName, badNumber }

/// Who Argus protects and who to call. Shared by the setup wizard and the settings screen.
class ProfileForm {
  const ProfileForm({this.title, this.customName = '', this.usesCustomName = false, this.familyName = '', this.familyNumber = '', this.familyAlerts = false});

  /// Null until one is picked. NEUTRAL with [customName] means "their name".
  final ParentTitle? title;
  final String customName;

  /// True when "Their name" is picked.
  final bool usesCustomName;
  final String familyName;
  final String familyNumber;
  final bool familyAlerts;

  ProfileForm copyWith({ParentTitle? Function()? title, String? customName, bool? usesCustomName, String? familyName, String? familyNumber, bool? familyAlerts}) =>
      ProfileForm(
        title: title != null ? title() : this.title,
        customName: customName ?? this.customName,
        usesCustomName: usesCustomName ?? this.usesCustomName,
        familyName: familyName ?? this.familyName,
        familyNumber: familyNumber ?? this.familyNumber,
        familyAlerts: familyAlerts ?? this.familyAlerts,
      );

  List<ProfileProblem> problems() => [
    if (title == null && !usesCustomName) ProfileProblem.noTitle,
    if (usesCustomName && customName.trim().isEmpty) ProfileProblem.noCustomName,
    if (familyName.trim().isEmpty) ProfileProblem.noFamilyName,
    if (!PhoneNumbers.isValid(familyNumber)) ProfileProblem.badNumber,
  ];

  /// The title to save: their own name uses the neutral voice clips.
  ParentTitle get savedTitle => usesCustomName ? ParentTitle.neutral : title ?? ParentTitle.neutral;
  String get savedCustomName => usesCustomName ? customName.trim() : '';
}

abstract final class PhoneNumbers {
  static final _separators = RegExp(r'[\s\-().]');
  static final _digits = RegExp(r'^\d+$');

  /// 7 to 15 digits, with an optional leading +; spaces, dashes and brackets are fine.
  static bool isValid(String number) {
    var n = number.trim().replaceAll(_separators, '');
    if (n.startsWith('+')) n = n.substring(1);
    return n.length >= 7 && n.length <= 15 && _digits.hasMatch(n);
  }
}

import '../core/alert.dart';
import '../core/findings.dart';
import '../core/parent_title.dart';
import '../core/verdict.dart';
import '../l10n/gen/app_localizations.dart';

/// The fixed words for each level, in the app's language. Shared by the notification, the
/// warning screen and the Check screen. Never the message or its sender.
class AlertText {
  const AlertText(this.l);

  final AppLocalizations l;

  /// "Mummy", "Daddy", "Grandma", or their own name; null for the neutral title without a name.
  String? titleName(ParentTitle title, String customName) => switch (title) {
    ParentTitle.mummy => l.titleMummy,
    ParentTitle.daddy => l.titleDaddy,
    ParentTitle.grandma => l.titleGrandma,
    ParentTitle.neutral => customName.trim().isEmpty ? null : customName.trim(),
  };

  /// The family member's name, or "your family" when setup left it empty.
  String familyName(String name) => name.trim().isEmpty ? l.yourFamily : name.trim();

  String headline(Level level, ParentTitle title, String customName) {
    final name = titleName(title, customName);
    return switch (level) {
      Level.scam => name != null ? l.alertScamHeadlineNamed(name) : l.alertScamHeadline,
      Level.careful => name != null ? l.alertCarefulHeadlineNamed(name) : l.alertCarefulHeadline,
      Level.otp => l.alertOtpHeadline,
      Level.none => l.alertNoneHeadline,
    };
  }

  /// What to do, the same every time for a level.
  String body(Level level, String family) => switch (level) {
    Level.scam => l.alertScamBody,
    Level.careful => l.alertCarefulBody(familyName(family)),
    Level.otp => l.alertOtpBody,
    Level.none => l.alertNoneBody(familyName(family)),
  };

  /// Built from what the rules found ("It has a link and says your account is blocked."),
  /// else the fixed reason for the rule that decided, else null.
  String? reason(List<Finding> findings, ReasonCode? code) => fromFindings(findings) ?? (code == null ? null : reasonFor(code));

  String? advice(ReasonCode? code) => code == null ? null : adviceFor(code);

  /// "From: Tunde (WhatsApp)"; null when the sender isn't known.
  String? from(SourceType? source, String? sender) {
    if (sender == null || sender.trim().isEmpty || source == null) return null;
    return switch (source) {
      SourceType.sms => l.fromSms(sender),
      SourceType.whatsapp => l.fromWhatsapp(sender),
    };
  }

  /// The two most important findings, joined into one fixed sentence.
  String? fromFindings(List<Finding> findings) {
    final phrases = findings.take(2).map(_phrase).toList();
    return switch (phrases.length) {
      0 => null,
      1 => l.reasonOne(phrases[0]),
      _ => l.reasonTwo(phrases[0], phrases[1]),
    };
  }

  String _phrase(Finding f) => switch (f.kind) {
    FindingKind.asksForSecret => l.findingAsksFor(_secret(f.secret ?? Secret.code)),
    FindingKind.accountProblem => l.findingAccount(_trouble(f.trouble ?? AccountTrouble.blocked)),
    FindingKind.link => l.findingLink,
    FindingKind.verifyAccount => l.findingVerify,
    FindingKind.newNumber => l.findingNewNumber,
    FindingKind.prize => l.findingPrize,
    FindingKind.moneyRequest => l.findingMoney,
    FindingKind.officialClaim => l.findingOfficial,
    FindingKind.phoneNumber => l.findingPhone,
    FindingKind.urgency => l.findingUrgency,
  };

  // BVN, NIN, OTP and PIN stay as these Latin acronyms in English and Hindi
  String _secret(Secret s) => switch (s) {
    Secret.bvn => 'BVN',
    Secret.nin => 'NIN',
    Secret.otp => 'OTP',
    Secret.pin => 'PIN',
    Secret.password => l.secretPassword,
    Secret.card => l.secretCard,
    Secret.code => l.secretCode,
  };

  String _trouble(AccountTrouble t) => switch (t) {
    AccountTrouble.blocked => l.troubleBlocked,
    AccountTrouble.suspended => l.troubleSuspended,
    AccountTrouble.restricted => l.troubleRestricted,
    AccountTrouble.closed => l.troubleClosed,
  };

  String reasonFor(ReasonCode code) => switch (code) {
    ReasonCode.asksForCode => l.reasonAsksForCode,
    ReasonCode.accountProblem => l.reasonAccountProblem,
    ReasonCode.tooGoodOffer => l.reasonTooGoodOffer,
    ReasonCode.fakeOfficial => l.reasonFakeOfficial,
    ReasonCode.asksForMoneyOrLink => l.reasonAsksForMoneyOrLink,
    ReasonCode.pressure => l.reasonPressure,
    ReasonCode.accountOrOffer => l.reasonAccountOrOffer,
    ReasonCode.moneyUnderPressure => l.reasonMoneyUnderPressure,
    ReasonCode.linkOrNumber => l.reasonLinkOrNumber,
    ReasonCode.warningSigns => l.reasonWarningSigns,
  };

  String adviceFor(ReasonCode code) => switch (code) {
    ReasonCode.asksForCode => l.adviceAsksForCode,
    ReasonCode.accountProblem => l.adviceAccountProblem,
    ReasonCode.tooGoodOffer => l.adviceTooGoodOffer,
    ReasonCode.fakeOfficial => l.adviceFakeOfficial,
    ReasonCode.asksForMoneyOrLink => l.adviceAsksForMoneyOrLink,
    ReasonCode.pressure => l.advicePressure,
    ReasonCode.accountOrOffer => l.adviceAccountOrOffer,
    ReasonCode.moneyUnderPressure => l.adviceMoneyUnderPressure,
    ReasonCode.linkOrNumber => l.adviceLinkOrNumber,
    ReasonCode.warningSigns => l.adviceWarningSigns,
  };
}

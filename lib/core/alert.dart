import 'findings.dart';
import 'message_preview.dart';
import 'scam_checker.dart';
import 'verdict.dart';

enum SourceType {
  sms('SMS'),
  whatsapp('WhatsApp');

  const SourceType(this.label);
  final String label;
}

/// One incoming message. Lives only in memory; never logged.
class IncomingMessage {
  IncomingMessage(this.type, this.text, String? sender, {String? chatKey})
    : sender = (sender == null || sender.trim().isEmpty) ? unknownSender : sender,
      chatKey = chatKey ?? '${type.label}:$sender';

  /// When SMS or WhatsApp gives no sender. Not shown to the parent.
  static const unknownSender = 'someone';

  final SourceType type;
  final String text;
  final String sender;
  final String chatKey;
}

/// What the warning screen shows: the verdict, what was found, who sent it and the message.
/// Held in memory only; never logged, written to disk or put in a notification payload.
class Alert {
  const Alert({required this.verdict, this.findings = const [], this.source, this.sender, this.preview, this.message, this.chatId});

  factory Alert.from(IncomingMessage message, CheckResult result) {
    final preview = MessagePreview.keepingMarks(message.text, result.findings);
    final full = MessagePreview.full(message.text);
    return Alert(
      verdict: result.verdict,
      findings: result.findings,
      source: message.type,
      sender: message.sender == IncomingMessage.unknownSender ? null : message.sender,
      preview: preview.isEmpty ? null : preview,
      message: full.isEmpty ? null : full,
      chatId: message.chatKey.hashCode,
    );
  }

  final Verdict verdict;
  final List<Finding> findings;
  final SourceType? source;
  final String? sender;
  final String? preview;
  final String? message;

  /// Hash of the chat it came from, for "one spoken be-careful per chat". Null for test alerts.
  final int? chatId;

  Level get level => verdict.level;
}

/// Which notification channel an alert uses. SCAM interrupts; CAREFUL is quiet.
enum AlertChannel { scam, careful }

/// How strongly one level alerts.
class AlertPlan {
  const AlertPlan({required this.channel, required this.fullScreen, required this.alwaysVibrate, required this.familySms});

  final AlertChannel channel;

  /// Full-screen red warning, also over the lock screen.
  final bool fullScreen;

  /// Strong vibration even when the phone is on silent.
  final bool alwaysVibrate;

  /// Family SMS, when turned on in Settings.
  final bool familySms;

  /// Null for NONE: no notification, no sound, nothing.
  static AlertPlan? forLevel(Level level) => switch (level) {
    Level.scam => const AlertPlan(channel: AlertChannel.scam, fullScreen: true, alwaysVibrate: true, familySms: true),
    Level.careful || Level.otp => const AlertPlan(channel: AlertChannel.careful, fullScreen: false, alwaysVibrate: false, familySms: false),
    Level.none => null,
  };
}

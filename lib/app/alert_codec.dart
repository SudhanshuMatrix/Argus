import 'dart:convert';

import '../core/alert.dart';
import '../core/findings.dart';
import '../core/verdict.dart';

/// Turns an [Alert] into plain values and back.
///
/// [toPayload] is what goes into a notification (and so into an Android Intent): the verdict
/// and the finding codes only, never the text or the sender of a received message. (Live
/// alerts are the exception: their text is a public warning from the Argus server.) [toPrivateMap] adds them,
/// and is only ever sent between isolates of this process in memory.
abstract final class AlertCodec {
  static String toPayload(int id, Alert alert) => jsonEncode({
    'id': id,
    ..._public(alert),
    // A live alert's text was written by the Argus team for everyone, not received privately
    if (alert.source == SourceType.live) 'message': alert.message,
  });

  static Map<String, Object?> toPrivateMap(int id, Alert alert) => {
    'id': id,
    ..._public(alert),
    'sender': alert.sender,
    'preview': alert.preview,
    'message': alert.message,
    'chatId': alert.chatId,
  };

  static Map<String, Object?> _public(Alert a) => {
    'level': a.verdict.level.name,
    'reason': a.verdict.reasonCode?.name,
    'findings': [for (final f in a.findings) _finding(f)],
    'source': a.source?.name,
  };

  static String _finding(Finding f) => [f.kind.name, if (f.secret != null) f.secret!.name, if (f.trouble != null) f.trouble!.name].join(':');

  /// (id, alert) from a payload or a private map; null when it can't be read.
  static (int, Alert)? decode(Object? data) {
    try {
      final map = data is String ? jsonDecode(data) as Map<String, Object?> : (data as Map).cast<String, Object?>();
      final level = Level.values.byName(map['level'] as String);
      final reason = map['reason'] == null ? null : ReasonCode.values.byName(map['reason'] as String);
      final findings = [for (final f in (map['findings'] as List? ?? const [])) ?_decodeFinding(f as String)];
      final source = map['source'] == null ? null : SourceType.values.byName(map['source'] as String);
      return (
        map['id'] as int,
        Alert(
          verdict: Verdict(level, reasonCode: reason),
          findings: findings,
          source: source,
          sender: map['sender'] as String?,
          preview: map['preview'] as String?,
          message: map['message'] as String?,
          chatId: map['chatId'] as int?,
        ),
      );
    } catch (_) {
      return null;
    }
  }

  static Finding? _decodeFinding(String code) {
    final parts = code.split(':');
    final kind = FindingKind.values.asNameMap()[parts.first];
    if (kind == null) return null;
    return switch (kind) {
      FindingKind.asksForSecret => Finding.asksForSecret(Secret.values.asNameMap()[parts.elementAtOrNull(1)] ?? Secret.code),
      FindingKind.accountProblem => Finding.accountProblem(AccountTrouble.values.asNameMap()[parts.elementAtOrNull(1)] ?? AccountTrouble.blocked),
      _ => Finding(kind),
    };
  }
}

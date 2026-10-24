import 'dart:isolate';
import 'dart:ui';

import '../core/alert.dart';
import 'alert_codec.dart';

/// Hands alerts from the background isolates (SMS, WhatsApp) to the app's screen, in memory.
/// Nothing here is written to disk or put in an Intent.
abstract final class AlertBus {
  static const _portName = 'argus_alerts';
  static const _keep = 20;

  /// The latest alerts with their message text, for the warning screen. UI isolate only.
  static final Map<int, Alert> _recent = {};

  static void publish(int id, Alert alert) {
    try {
      IsolateNameServer.lookupPortByName(_portName)?.send(AlertCodec.toPrivateMap(id, alert));
    } catch (_) {
      // The app's screen isn't running: the notification still opens the warning
    }
  }

  /// Starts listening in the UI isolate. [onAlert] gets every new alert.
  static void listen(void Function(int id, Alert alert) onAlert) {
    final port = ReceivePort();
    IsolateNameServer.removePortNameMapping(_portName);
    IsolateNameServer.registerPortWithName(port.sendPort, _portName);
    port.listen((data) {
      final decoded = AlertCodec.decode(data);
      if (decoded == null) return;
      final (id, alert) = decoded;
      remember(id, alert);
      onAlert(id, alert);
    });
  }

  static void remember(int id, Alert alert) {
    _recent[id] = alert;
    while (_recent.length > _keep) {
      _recent.remove(_recent.keys.first);
    }
  }

  /// The full alert when this process still has it, else the one rebuilt from the payload.
  static Alert resolve(int id, Alert fromPayload) => _recent[id] ?? fromPayload;
}

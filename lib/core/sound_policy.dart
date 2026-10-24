import 'alert.dart';

/// The phone's ringer switch.
enum Ringer { normal, vibrate, silent }

/// Silent mode is respected by default: the voice stays quiet unless the ringer is on or the
/// "Speak warnings even on silent" setting is on. A SCAM always vibrates and shows the red screen.
abstract final class SoundPolicy {
  /// [userAsked]: the parent tapped "Hear it again", so play whatever the ringer says.
  static bool shouldSpeak(Ringer ringer, bool speakOnSilent, {bool userAsked = false}) =>
      userAsked || ringer == Ringer.normal || speakOnSilent;

  /// On silent, or when the notification couldn't be posted at all, the app vibrates itself.
  static bool vibrateDirectly(AlertPlan plan, Ringer ringer, bool notificationPosted) =>
      plan.alwaysVibrate && (ringer == Ringer.silent || !notificationPosted);
}

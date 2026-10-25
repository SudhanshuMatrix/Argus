import 'package:audioplayers/audioplayers.dart';

import '../core/language.dart';
import '../core/parent_title.dart';
import '../core/verdict.dart';
import '../core/voice_clips.dart';

/// Plays the bundled warning clip for a level, in the parent's language and title, falling back
/// to the neutral clip and then to English. English clips are by ElevenLabs; Hindi clips are
/// made with tool/make_hindi_voice.py. Fully offline; nothing here throws.
///
/// Silent mode: by default the clip plays on the notification stream, which Android mutes on
/// silent and vibrate. With "Speak warnings even on silent" (or "Hear it again") it plays on
/// the alarm stream, which is heard anyway.
class VoicePlayer {
  VoicePlayer();

  AudioPlayer? _player;

  Future<void> play(Level level, Language language, ParentTitle title, {required bool loud}) async {
    final names = VoiceClips.candidates(level, language, title);
    if (names.isEmpty) return;
    await stop();
    try {
      final player = AudioPlayer();
      _player = player;
      await player.setAudioContext(
        AudioContext(
          android: AudioContextAndroid(
            usageType: loud ? AndroidUsageType.alarm : AndroidUsageType.notification,
            contentType: AndroidContentType.speech,
            audioFocus: AndroidAudioFocus.gainTransientMayDuck,
          ),
        ),
      );
      await player.setReleaseMode(ReleaseMode.release);
      await player.play(AssetSource('voice/${names.first}.mp3'));
    } catch (_) {
      // No sound is better than a crash; the screen and vibration still warn
    }
  }

  Future<void> stop() async {
    final p = _player;
    _player = null;
    try {
      await p?.dispose();
    } catch (_) {}
  }
}

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter_tts/flutter_tts.dart';

import '../core/language.dart';
import '../core/parent_title.dart';
import '../core/verdict.dart';
import '../core/voice_clips.dart';
import '../l10n/gen/app_localizations.dart';
import 'alert_text.dart';

/// Speaks the warning. English plays the bundled ElevenLabs clip; Hindi uses the phone's own
/// offline text-to-speech voice (hi-IN) to say the fixed Hindi warning, and falls back to the
/// English clip when no Hindi voice is installed. Fully offline; nothing here throws.
///
/// Silent mode: by default the clip plays on the notification stream, which Android mutes on
/// silent and vibrate. With "Speak warnings even on silent" (or "Hear it again") it plays on
/// the alarm stream, which is heard anyway. The Hindi voice uses the media volume.
class VoicePlayer {
  VoicePlayer();

  AudioPlayer? _player;
  FlutterTts? _tts;

  Future<void> play(Level level, Language language, ParentTitle title, {required bool loud, AppLocalizations? l, String customName = ''}) async {
    if (level == Level.none) return;
    await stop();
    if (language == Language.hindi && l != null && await _speakHindi(level, title, customName, l)) return;
    await _playClip(level, language, title, loud: loud);
  }

  Future<bool> _speakHindi(Level level, ParentTitle title, String customName, AppLocalizations l) async {
    try {
      final tts = _tts ??= FlutterTts();
      // Wait for the whole sentence: a background alert's isolate may end as soon as this returns
      await tts.awaitSpeakCompletion(true);
      // "Available" is not enough: the Hindi voice data must also be on the phone (LANG_MISSING_DATA)
      if (await tts.isLanguageAvailable('hi-IN') != true) return false;
      if (await tts.setLanguage('hi-IN') != 1) return false;
      await tts.setSpeechRate(0.45); // slower is clearer for older ears
      await tts.setVolume(1.0);
      final text = AlertText(l);
      final words = '${text.headline(level, title, customName)} ${text.body(level, '')}';
      // 1 means spoken; anything else (no engine, no voice) falls back to the English clip.
      // The plugin can hold a refused call forever, so give up waiting after a while; by then
      // the sentence has been spoken or never will be, and a second voice must not start over it.
      final spoken = await tts.speak(words).timeout(const Duration(seconds: 30), onTimeout: () => 1);
      return spoken == 1;
    } catch (_) {
      return false;
    }
  }

  Future<void> _playClip(Level level, Language language, ParentTitle title, {required bool loud}) async {
    final names = VoiceClips.candidates(level, language, title);
    if (names.isEmpty) return;
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
    try {
      await _tts?.stop();
    } catch (_) {}
  }
}

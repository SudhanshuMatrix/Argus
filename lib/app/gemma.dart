import 'dart:async';

import 'package:flutter_edge_ai/flutter_edge_ai.dart';
import 'package:flutter_edge_ai_litertlm/flutter_edge_ai_litertlm.dart';

import '../core/model_prompt.dart';
import 'edition.dart';

/// Gemma 3 1B on the phone, through flutter_edge_ai and LiteRT-LM. CPU only, greedy decoding,
/// short replies. Loaded on first use and closed after a few idle minutes to give the memory
/// back. Never logs prompts or replies; every failure returns null so the rules decide.
class GemmaModel implements TextGenerator {
  GemmaModel._();

  static final instance = GemmaModel._();

  /// The file name inside the Offline APK (android/app/src/offline/assets/models/) and on the phone.
  static const modelFile = 'gemma3-1b-it-int4.litertlm';

  /// Where the Online APK downloads the model from (a GitHub Release asset by default).
  static const modelUrl = String.fromEnvironment(
    'ARGUS_MODEL_URL',
    defaultValue: 'https://github.com/SudhanshuMatrix/Argus/releases/download/model-gemma3-1b/$modelFile',
  );

  static const _idle = Duration(minutes: 3);

  static bool _initialized = false;
  InferenceModel? _model;
  Timer? _idleTimer;
  Future<void> _queue = Future.value();

  static Future<bool> _init() async {
    if (_initialized) return true;
    try {
      // The library logs chat history at verbose level; message text must never reach a log
      FlutterEdgeAi.logLevel = EdgeAiLogLevel.none;
      await FlutterEdgeAi.initialize(inferenceEngines: const [LiteRtLmEngine()]);
      _initialized = true;
    } catch (_) {
      _initialized = false;
    }
    return _initialized;
  }

  static Future<bool> isInstalled() async {
    if (!Edition.current.usesModel || !await _init()) return false;
    try {
      return await FlutterEdgeAi.isModelInstalled(modelFile);
    } catch (_) {
      return false;
    }
  }

  /// Offline: registers the model that ships inside the APK. Online: downloads it once.
  /// [onProgress] gets 0-100 for downloads. True when the model is ready.
  static Future<bool> install({void Function(int percent)? onProgress}) async {
    if (!Edition.current.usesModel || !await _init()) return false;
    try {
      final builder = FlutterEdgeAi.installModel(modelType: ModelType.gemmaIt, fileType: ModelFileType.litertlm);
      if (Edition.current.bundlesModel) {
        await builder.fromBundled(modelFile).install();
      } else {
        await builder.fromNetwork(modelUrl).withProgress((p) => onProgress?.call(p)).install();
      }
      return await isInstalled();
    } catch (_) {
      return false;
    }
  }

  /// The checker's model when it is installed, else null (the rules decide alone).
  static Future<TextGenerator?> available() async => await isInstalled() ? instance : null;

  @override
  Future<String?> generate(String system, String prompt) {
    // One question at a time: the phone has room for one model and one answer
    final result = _queue.then((_) => _generate(system, prompt));
    _queue = result.then((_) {}, onError: (_) {});
    return result;
  }

  Future<String?> _generate(String system, String prompt) async {
    _idleTimer?.cancel();
    InferenceChat? chat;
    try {
      final model = _model ??= await FlutterEdgeAi.getActiveModel(maxTokens: 2048, preferredBackend: PreferredBackend.cpu);
      chat = await model.createChat(systemInstruction: system, maxOutputTokens: 96, temperature: 0, topK: 1);
      await chat.addQueryChunk(Message.text(text: prompt, isUser: true));
      final reply = await chat.generateChatResponse();
      return reply is TextResponse ? reply.token : null;
    } catch (_) {
      await _unload();
      return null;
    } finally {
      try {
        await chat?.close();
      } catch (_) {}
      _idleTimer = Timer(_idle, _unload);
    }
  }

  Future<void> _unload() async {
    _idleTimer?.cancel();
    final m = _model;
    _model = null;
    try {
      await m?.close();
    } catch (_) {}
  }
}

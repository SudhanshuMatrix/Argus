import 'package:argus/app/edition.dart';
import 'package:argus/app/firebase_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('each edition does exactly its job', () {
    expect(Edition.offline.checksMessages && Edition.offline.bundlesModel, isTrue);
    // The Offline APK only uses the model when CI bundled it (ARGUS_MODEL_BUNDLED)
    expect(Edition.offline.usesModel, Edition.modelBundled);
    expect(Edition.offline.receivesLiveAlerts, isFalse);
    expect(Edition.online.checksMessages && Edition.online.usesModel && Edition.online.receivesLiveAlerts, isTrue);
    expect(Edition.online.bundlesModel, isFalse);
    expect(Edition.demo.receivesLiveAlerts, isTrue);
    expect(Edition.demo.checksMessages || Edition.demo.usesModel, isFalse);
  });

  test('without a flavor (tests, plain flutter run) it is the offline build, with live alerts off', () {
    expect(Edition.current, Edition.offline);
    expect(FirebaseConfig.configured, isFalse);
  });
}

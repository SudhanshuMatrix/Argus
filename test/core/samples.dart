import 'dart:io';

/// The synthetic messages in test/samples. Tests run from the package root.
class Sample {
  Sample(this.name, this.text);
  final String name;
  final String text;
}

List<Sample> _load(String folder) {
  final dir = Directory('test/samples/$folder');
  final files = dir.listSync().whereType<File>().where((f) => f.path.endsWith('.txt')).toList()
    ..sort((a, b) => a.path.compareTo(b.path));
  return [for (final f in files) Sample(f.uri.pathSegments.last, f.readAsStringSync().trim())];
}

final scams = _load('scam');
final normals = _load('normal');
final holdoutScams = _load('holdout/scam');
final holdoutNormals = _load('holdout/normal');

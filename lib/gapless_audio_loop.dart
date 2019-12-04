import 'dart:async';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/services.dart' show rootBundle;

import 'package:flutter/services.dart';

class GaplessAudioLoop {
  static const MethodChannel _channel =
      const MethodChannel('gapless_audio_loop');

  /// A reference to the loaded file.
  String _loadedFile;
  int _id;
  double _volumeleft = 1.0;
  double _volumeright = 1.0;

  double get volumeleft => _volumeleft;
  double get volumeright => _volumeright;

  void setVolume(double volumeleft, double volumeright) async {
    _volumeleft = volumeleft;
    _volumeright = volumeright;

    if (_id != null) {
      await _channel
          .invokeMethod("setVolume", {'playerId': _id, "volumeleft": _volumeleft, "volumeright": _volumeright});
    }
  }

  Future<ByteData> _fetchAsset(String fileName) async {
    return await rootBundle.load('assets/$fileName');
  }

  Future<File> _fetchToMemory(String fileName) async {
    final file = File('${(await getTemporaryDirectory()).path}/$fileName');
    await file.create(recursive: true);
    return await file
        .writeAsBytes((await _fetchAsset(fileName)).buffer.asUint8List());
  }

  /// Load the [fileName] for playing
  ///
  Future<void> loadAsset(String fileName) async {
    if (_loadedFile != null) {
      return _loadedFile;
    }

    final result = await _fetchToMemory(fileName);
    _loadedFile = result.path;
  }

  void loadFile(String path) {
    _loadedFile = path;
  }

  Future<void> play() async {
    assert(_loadedFile != null, 'File is not loaded');

    // Do nothing when it  is already playing
    if (_id == null) {
      _id = await _channel
          .invokeMethod("play", {'url': _loadedFile, 'volumeleft': _volumeleft, 'volumeright': _volumeright});
    }
  }

  Future<void> playLR(double volumeleft, double volumeright) async {
    assert(_loadedFile != null, 'File is not loaded');

    _volumeleft = volumeleft;
    _volumeright = volumeright;

    // Do nothing when it  is already playing
    if (_id == null) {
      _id = await _channel
          .invokeMethod("play", {'url': _loadedFile, 'volumeleft': _volumeleft, 'volumeright': _volumeright});
    }
  }

  Future<void> pause() async {
    assert(_id != null, 'Loop is not playing');

    await _channel.invokeMethod("pause", {'playerId': _id});
  }

  Future<void> resume() async {
    assert(_loadedFile != null, 'File is not loaded');
    assert(_id != null, 'Loop is not playing');

    await _channel.invokeMethod("resume", {'playerId': _id});
  }

  Future<void> stop() async {
    assert(_loadedFile != null, 'File is not loaded');
    assert(_id != null, 'Loop is not playing');

    await _channel.invokeMethod("stop", {'playerId': _id});
  }

  Future<void> seek(Duration duration) async {
    assert(_loadedFile != null, 'File is not loaded');
    assert(_id != null, 'Loop is not playing');

    await _channel.invokeMethod(
        "seek", {'playerId': _id, "position": duration.inMilliseconds});
  }
}

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mapboxnav_platform_interface.dart';

class MethodChannelMapboxNavPlugin extends MapboxNavPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('mapboxnav');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}

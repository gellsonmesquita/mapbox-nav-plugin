import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mapbox_nav_plugin_platform_interface.dart';

/// An implementation of [MapboxNavPluginPlatform] that uses method channels.
class MethodChannelMapboxNavPlugin extends MapboxNavPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('mapbox_nav_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}

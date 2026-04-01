import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'src/data/performance_policy.dart';

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

  @override
  Future<void> setPerformancePolicy(PerformancePolicy policy) async {
    await methodChannel.invokeMethod('setPerformancePolicy', policy.toMap());
  }
}

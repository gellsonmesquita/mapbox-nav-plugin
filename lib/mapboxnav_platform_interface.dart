import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'mapboxnav_method_channel.dart';
import 'src/data/performance_policy.dart';


abstract class MapboxNavPlatform extends PlatformInterface {

  MapboxNavPlatform() : super(token: _token);

  static final Object _token = Object();

  static MapboxNavPlatform _instance = MethodChannelMapboxNavPlugin();

  static MapboxNavPlatform get instance => _instance;

  static set instance(MapboxNavPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implementado.');
  }

  Future<void> setPerformancePolicy(PerformancePolicy policy) {
    throw UnimplementedError('setPerformancePolicy() has not been implementado.');
  }
}
import 'package:flutter_test/flutter_test.dart';
import 'package:mapbox_nav_plugin/mapbox_nav_plugin.dart';
import 'package:mapbox_nav_plugin/mapbox_nav_plugin_platform_interface.dart';
import 'package:mapbox_nav_plugin/mapbox_nav_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMapboxNavPluginPlatform
    with MockPlatformInterfaceMixin
    implements MapboxNavPluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MapboxNavPluginPlatform initialPlatform = MapboxNavPluginPlatform.instance;

  test('$MethodChannelMapboxNavPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMapboxNavPlugin>());
  });

}

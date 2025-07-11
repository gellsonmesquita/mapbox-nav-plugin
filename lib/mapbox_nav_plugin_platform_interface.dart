import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'mapbox_nav_plugin_method_channel.dart';

/// Uma interface abstrata para o plugin Mapbox Navigation.
///
/// Implementações específicas da plataforma devem estender esta classe
/// e fornecer sua própria lógica para interagir com as APIs nativas.
abstract class MapboxNavPluginPlatform extends PlatformInterface {
  /// Constrói um MapboxNavPluginPlatform.
  MapboxNavPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static MapboxNavPluginPlatform _instance = MethodChannelMapboxNavPlugin();

  /// A instância padrão de [MapboxNavPluginPlatform] a ser usada.
  ///
  /// O padrão é [MethodChannelMapboxNavPlugin].
  static MapboxNavPluginPlatform get instance => _instance;

  /// Implementações específicas da plataforma devem definir isso com sua própria
  /// classe específica da plataforma que estende [MapboxNavPluginPlatform] quando
  /// elas se registram.
  static set instance(MapboxNavPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Retorna a versão da plataforma nativa.
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implementado.');
  }

// **TODO:** Adicione aqui quaisquer métodos globais do plugin que não são específicos do controlador de navegação.
// Por exemplo, um método para inicializar o SDK Mapbox globalmente se necessário.
}
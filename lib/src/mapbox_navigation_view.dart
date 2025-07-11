import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:mapbox_nav_plugin/src/mapbox_navigation_controller.dart';

// Definir um novo typedef para o callback
typedef MapboxViewCreatedCallback = void Function(MapboxNavigationController controller);

/// Widget Flutter para exibir o mapa de navegação Mapbox.
///
/// Este widget é o invólucro para a `PlatformView` nativa. Ele usa um
/// [onMapboxViewCreated] callback para notificar o widget pai quando
/// o controlador de navegação estiver pronto para uso.
class MapboxNavigationView extends StatefulWidget {
  /// O callback que será chamado quando a view nativa for criada
  /// e o [MapboxNavigationController] estiver disponível.
  final MapboxViewCreatedCallback onMapboxViewCreated;

  /// Token de acesso do Mapbox.
  ///
  /// Opcional, pode ser definido uma vez globalmente via `controller.setAccessToken()`.
  final String? accessToken;

  const MapboxNavigationView({
    Key? key,
    required this.onMapboxViewCreated, // Agora é obrigatório e é um callback
    this.accessToken,
  }) : super(key: key);

  @override
  State<MapboxNavigationView> createState() => _MapboxNavigationViewState();
}

class _MapboxNavigationViewState extends State<MapboxNavigationView> {
  // O controlador agora é criado e gerenciado pelo widget pai,
  // ou será criado dentro deste método e retornado via callback.
  // Não precisamos mais de um _internalController aqui.

  @override
  void initState() {
    super.initState();
    // O accessToken pode ser definido aqui, mas o controlador ainda não foi criado.
    // É melhor passá-lo para o controlador no callback.
  }

  // Este método será chamado quando a view nativa for criada pelo Flutter.
  void _onPlatformViewCreated(int id) {
    // AQUI é onde o ID real da view é conhecido!
    // Criamos o controlador aqui e o passamos de volta via callback.
    final controller = MapboxNavigationController(id);

    // Definir o accessToken assim que o controlador for criado
    //if (widget.accessToken != null) {
    //  controller.setAccessToken(widget.accessToken!);
    //}

    // Notifique o widget pai que o controlador está pronto
    widget.onMapboxViewCreated(controller);
  }

  @override
  Widget build(BuildContext context) {
    const String viewType = 'com.app.mapbox_nav_plugin/mapbox_navigation_view'; //

    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: viewType,
        onPlatformViewCreated: _onPlatformViewCreated,
        layoutDirection: TextDirection.ltr,
        creationParams: const <String, dynamic>{},
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: viewType,
        onPlatformViewCreated: _onPlatformViewCreated,
        layoutDirection: TextDirection.ltr,
        creationParams: const <String, dynamic>{},
        creationParamsCodec: const StandardMessageCodec(),
      );
    }
    return Text('$defaultTargetPlatform não suportado pela View do Mapbox Navigation.',
      style: const TextStyle(color: Colors.red),
    );
  }
}
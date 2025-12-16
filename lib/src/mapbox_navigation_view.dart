import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mapboxnav/mapboxnav.dart';

typedef MapboxViewCreatedCallback = void Function(MapboxNavigationController controller);

class MapboxNavigationView extends StatefulWidget {

  final MapboxViewCreatedCallback onMapboxViewCreated;

  final String? accessToken;

  const MapboxNavigationView({
    Key? key,
    required this.onMapboxViewCreated,
    this.accessToken,
  }) : super(key: key);

  @override
  State<MapboxNavigationView> createState() => _MapboxNavigationViewState();
}

class _MapboxNavigationViewState extends State<MapboxNavigationView> {

  @override
  void initState() {
    super.initState();
  }

  void _onPlatformViewCreated(int id) {
    final controller = MapboxNavigationController(id);
    widget.onMapboxViewCreated(controller);
  }

  @override
  Widget build(BuildContext context) {
    const String viewType = 'mapView';

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
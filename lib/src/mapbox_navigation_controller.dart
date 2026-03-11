import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:mapboxnav/src/data/mapbox_config.dart';

typedef MapboxNavigationEvent = Map<String, dynamic>;

class MapboxNavigationController extends ChangeNotifier {
  final int _viewId;
  late final MethodChannel _methodChannel;
  late final EventChannel _eventChannel;
  late final MethodChannel _methodEventChannel;

  final StreamController<MapboxNavigationEvent> _eventStreamController =
  StreamController<MapboxNavigationEvent>.broadcast();

  Stream<MapboxNavigationEvent> get events => _eventStreamController.stream;

  MapboxNavigationController(this._viewId) {
    _methodChannel = const MethodChannel('nav_channel');
    _eventChannel = EventChannel('nav_event/$_viewId');
    _methodEventChannel = MethodChannel('nav_event/$_viewId/methods');
    _listenToEvents();
  }

  void _listenToEvents() {
    _eventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (kDebugMode) {
        print('Evento recebido do nativo para viewId $_viewId: $event');
      }
      try {
        if (event is String) {
          final Map<String, dynamic> data = Map<String, dynamic>.from(jsonDecode(event));
          _eventStreamController.add(data);
        } else if (event is Map) {
          _eventStreamController.add(Map<String, dynamic>.from(event));
        }
      } catch (e) {
        if (kDebugMode) {
          print('Erro ao decodificar evento do nativo: $e, Evento original: $event');
        }
        _eventStreamController.add({"type": "decodeError", "message": e.toString(), "originalEvent": event});
      }
    }).onError((error) {
      if (kDebugMode) {
        print('Erro no stream do evento para viewId $_viewId: $error');
      }
      _eventStreamController.add({"type": "streamError", "message": error.toString()});
    });
  }

  Future<void> setAccessToken(String token) async {
    await _methodChannel.invokeMethod('setAccessToken', token);
  }
  Future<void> createRoute({
    required List<double> origin,
    required List<double> destination,
    List<List<double>>? waypoints,
  }) async {
    await _methodChannel.invokeMethod('createRoute', {
      'viewId': _viewId,
      'origin': origin,
      'destination': destination,
      'waypoints': waypoints,
    });
  }

  Future<void> startNavigation({
    List<double>? origin,
    required List<double> destination,
    List<List<double>>? waypoints,
  }) async {
    await _methodChannel.invokeMethod('startNavigation', {
      'viewId': _viewId,
      'origin': origin,
      'destination': destination,
      'waypoints': waypoints,
    });
  }

  Future<void> changeDestination({
    List<double>? origin,
    required List<double> newDestination,
  }) async {
    await _methodChannel.invokeMethod('changeDestination', {
      'viewId': _viewId,
      'origin': origin,
      'newDestination': newDestination,
    });
  }

  Future<void> recenter() async {
    await _methodEventChannel.invokeMethod('recenter', {
      'viewId': _viewId,
    });
  }

  Future<void> toggleVoiceInstructions() async {
    await _methodEventChannel.invokeMethod('toggleVoiceInstructions', {
      'viewId': _viewId,
    });
  }

  Future<void> showRouteOverview() async {
    await _methodEventChannel.invokeMethod('showRouteOverview', {
      'viewId': _viewId,
    });
  }

  Future<void> requestNavigationCameraToOverview() async {
    await _methodEventChannel.invokeMethod('requestNavigationCameraToOverview', {
      'viewId': _viewId,
    });
  }

  Future<void> cancelNavigation() async {
    await _methodChannel.invokeMethod('cancelNavigation', {
      'viewId': _viewId,
    });
  }

  Future<void> finishNavigation() async {
    await _methodChannel.invokeMethod('finishNavigation', {
      'viewId': _viewId,
    });
  }

  Future<void> updateRouteOptions({
    required bool isTruck,
    double? maxHeight,
    double? maxWeight,
    double? maxWidth,
    RouteProfile profile = RouteProfile.driving,
    List<ExcludeType> excludes = const [],
  }) async {
    await _methodEventChannel.invokeMethod('updateRouteOptions', {
      'isTruck': isTruck,
      'maxHeight': maxHeight,
      'maxWeight': maxWeight,
      'maxWidth': maxWidth,
      'profile': profile.value,
      'excludeList': excludes.map((e) => e.value).toList(),
    });
  }

  Future<void> setForbiddenZones(List<ForbiddenZone> zones) async {
    await _methodEventChannel.invokeMethod('setForbiddenZones', {
      'zones': zones.map((z) => z.toList()).toList(),
    });
  }

  Future<void> downloadRegion({
    required String region,
    required double north,
    required double east,
    required double south,
    required double west,
  }) async {
    try {
      await _methodChannel.invokeMethod('downloadOfflineArea', {
        'region': region,
        'north': north,
        'east': east,
        'south': south,
        'west': west,
        'viewId': _viewId
      });
    } on PlatformException catch (e) {
      print("Falha ao iniciar download: ${e.message}");
    }
  }

  Future<void> stopNavigation() async {
    await _methodChannel.invokeMethod('stopNavigation', {
      'viewId': _viewId,
    });
  }

  @override
  void dispose() {
    _eventStreamController.close();
    super.dispose();
  }
}
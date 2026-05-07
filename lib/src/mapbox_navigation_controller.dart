import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:mapboxnav/src/data/mapbox_config.dart';
import 'package:mapboxnav/src/data/navigation_behavior_policy.dart';
import 'package:mapboxnav/src/data/performance_policy.dart';

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

  Future<String?> getPlatformVersion() async {
    return _methodChannel.invokeMethod<String>('getPlatformVersion');
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

  @Deprecated(
    'setAccessToken nao e suportado pelo handler Android atual. '
    'Configure o token no Android nativo ou via inicializacao da view.',
  )
  Future<void> setAccessToken(String token) async {
    throw UnsupportedError(
      'setAccessToken nao e suportado pelo MethodChannelHandler atual.',
    );
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
    await _methodEventChannel.invokeMethod('showRouteOverview');
  }

  @Deprecated('Use showRouteOverview().')
  Future<void> requestNavigationCameraToOverview() async {
    await showRouteOverview();
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
    String language = 'pt',
    RouteUnits units = RouteUnits.metric,
    RouteGeometryPrecision geometryPrecision = RouteGeometryPrecision.polyline,
    bool alternatives = false,
    bool enableRefresh = false,
    List<ExcludeType> excludes = const [],
  }) async {
    await _methodEventChannel.invokeMethod('updateRouteOptions', {
      'isTruck': isTruck,
      'maxHeight': maxHeight,
      'maxWeight': maxWeight,
      'maxWidth': maxWidth,
      'profile': profile.value,
      'language': language,
      'units': units.value,
      'geometryPrecision': geometryPrecision.value,
      'alternatives': alternatives,
      'enableRefresh': enableRefresh,
      'excludeList': excludes.map((e) => e.value).toList(),
    });
  }

  Future<void> setDataUsageConfig({
    required DataSaverMode mode,
    bool? allowAlternatives,
    bool? enableRouteRefresh,
    RouteGeometryPrecision? geometryPrecision,
    int? locationUpdateIntervalMs,
    PerformancePolicy? performancePolicy,
  }) async {
    final payload = <String, dynamic>{
      'viewId': _viewId,
      'mode': mode.nativeValue,
    };

    if (allowAlternatives != null) {
      payload['allowAlternatives'] = allowAlternatives;
    }
    if (enableRouteRefresh != null) {
      payload['enableRouteRefresh'] = enableRouteRefresh;
    }
    if (geometryPrecision != null) {
      payload['geometryPrecision'] = geometryPrecision.value;
    }
    if (locationUpdateIntervalMs != null) {
      payload['locationUpdateIntervalMs'] = locationUpdateIntervalMs;
    }
    if (performancePolicy != null) {
      payload['performancePolicy'] = performancePolicy.toMap();
    }

    await _methodChannel.invokeMethod('setDataUsageConfig', payload);
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
    int maxZoom = 17,
  }) async {
    try {
      await _methodChannel.invokeMethod('downloadOfflineArea', {
        'region': region,
        'north': north,
        'east': east,
        'south': south,
        'west': west,
        'maxZoom': maxZoom,
        'viewId': _viewId,
      });
    } on PlatformException catch (e) {
      print("Falha ao iniciar download: ${e.message}");
    }
  }

  Future<List<OfflineRegionInfo>> listOfflineRegions() async {
    try {
      final raw = await _methodChannel.invokeMethod<List<dynamic>>(
        'listOfflineRegions',
        {'viewId': _viewId},
      );
      return (raw ?? [])
          .map((e) => OfflineRegionInfo.fromMap(Map<String, dynamic>.from(e as Map)))
          .toList();
    } on PlatformException catch (e) {
      print("Falha ao listar regiões: ${e.message}");
      return [];
    }
  }

  Future<void> deleteRegion({required String region}) async {
    try {
      await _methodChannel.invokeMethod('deleteOfflineRegion', {
        'region': region,
        'viewId': _viewId,
      });
    } on PlatformException catch (e) {
      print("Falha ao apagar região: ${e.message}");
    }
  }

  Future<void> stopNavigation() async {
    await _methodChannel.invokeMethod('stopNavigation', {
      'viewId': _viewId,
    });
  }

  Future<void> setPerformancePolicy(PerformancePolicy policy) async {
    await _methodChannel.invokeMethod('setPerformancePolicy', {
      'viewId': _viewId,
      ...policy.toMap(),
    });
  }

  Future<void> setDataSaverMode(DataSaverMode mode) async {
    await _methodChannel.invokeMethod('setDataSaverMode', {
      'viewId': _viewId,
      'mode': mode.nativeValue,
    });
  }

  Future<void> setNavigationBehavior(NavigationBehaviorPolicy policy) async {
    await _methodChannel.invokeMethod('setNavigationBehavior', {
      'viewId': _viewId,
      ...policy.toMap(),
    });
  }

  Future<void> setTripSessionActive(bool active) async {
    await _methodChannel.invokeMethod('setTripSessionActive', {
      'viewId': _viewId,
      'active': active,
    });
  }

  Future<void> setVoiceInstructionsMuted(bool muted) async {
    await _methodChannel.invokeMethod('setVoiceInstructionsMuted', {
      'viewId': _viewId,
      'muted': muted,
    });
  }

  Future<void> toggleTraffic(bool show) async {
    await _methodChannel.invokeMethod('toggleTraffic', {
      'viewId': _viewId,
      'show': show,
    });
  }

  @override
  void dispose() {
    _eventStreamController.close();
    super.dispose();
  }
}

class OfflineRegionInfo {
  final String id;
  final int completedBytes;
  final int completedCount;
  final int requiredCount;

  const OfflineRegionInfo({
    required this.id,
    required this.completedBytes,
    required this.completedCount,
    required this.requiredCount,
  });

  factory OfflineRegionInfo.fromMap(Map<String, dynamic> map) {
    return OfflineRegionInfo(
      id: map['id'] as String,
      completedBytes: (map['completedBytes'] as num).toInt(),
      completedCount: (map['completedCount'] as num).toInt(),
      requiredCount: (map['requiredCount'] as num).toInt(),
    );
  }

  /// Tamanho formatado (ex: "245 MB")
  String get formattedSize {
    if (completedBytes < 1024 * 1024) {
      return '${(completedBytes / 1024).toStringAsFixed(1)} KB';
    }
    return '${(completedBytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  /// Percentagem de completude (0.0 a 1.0)
  double get completionRatio =>
      requiredCount > 0 ? completedCount / requiredCount : 1.0;

  bool get isComplete => completedCount >= requiredCount;
}
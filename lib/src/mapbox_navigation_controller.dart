import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

// Define um tipo para os eventos de navegação
typedef MapboxNavigationEvent = Map<String, dynamic>;

/// Um controlador para interagir com o Mapbox Navigation View.
///
/// Este controlador gerencia a comunicação bidirecional com o lado nativo
/// para operações de navegação e recebimento de eventos.
class MapboxNavigationController extends ChangeNotifier {
  final int _viewId; // O ID da PlatformView associada a este controlador
  late final MethodChannel _methodChannel;
  late final EventChannel _eventChannel;

  final StreamController<MapboxNavigationEvent> _eventStreamController =
  StreamController<MapboxNavigationEvent>.broadcast();

  /// Stream de eventos de navegação recebidos do lado nativo.
  Stream<MapboxNavigationEvent> get events => _eventStreamController.stream;

  /// Construtor para MapboxNavigationController.
  ///
  /// O `viewId` é crucial para associar o controlador a uma instância específica
  /// de `MapboxNavigationView` e seu `EventChannel` correspondente no lado nativo.
  MapboxNavigationController(this._viewId) {
    _methodChannel = const MethodChannel('com.app.mapbox_nav_plugin/method_channel');
    _eventChannel = EventChannel('com.app.mapbox_nav_plugin/event_channel/$_viewId');
    _listenToEvents();
  }

  /// Inicia a escuta por eventos do lado nativo através do EventChannel.
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

  /// Configura o token de acesso do Mapbox.
  ///
  /// É essencial chamar este método antes de iniciar qualquer operação de navegação.
  Future<void> setAccessToken(String token) async {
    // Este método é invocado no canal de métodos principal, não depende de viewId
    // pois o token geralmente é global para a sessão do Mapbox SDK.
    await _methodChannel.invokeMethod('setAccessToken', token);
  }

  /// Cria e exibe uma rota no mapa sem iniciar a navegação ativa.
  ///
  /// [origin]: Coordenadas de origem no formato [latitude, longitude].
  /// [destination]: Coordenadas de destino no formato [latitude, longitude].
  /// [waypoints]: Lista opcional de coordenadas de pontos de passagem.
  Future<void> createRoute({
    required List<double> origin,
    required List<double> destination,
    List<List<double>>? waypoints,
  }) async {
    await _methodChannel.invokeMethod('createRoute', {
      'viewId': _viewId, // Passa o viewId para o lado nativo saber qual view controlar
      'origin': origin,
      'destination': destination,
      'waypoints': waypoints,
    });
  }

  /// Inicia a navegação para um destino.
  ///
  /// Se `origin` for fornecido, uma nova rota será calculada.
  /// Se `origin` for `null`, a navegação iniciará com a rota atualmente definida no mapa.
  /// [origin]: Coordenadas de origem (opcional).
  /// [destination]: Coordenadas de destino.
  /// [waypoints]: Lista opcional de coordenadas de pontos de passagem.
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

  /// Altera o destino durante uma sessão de navegação ativa.
  ///
  /// A rota será recalculada da localização atual do usuário para o novo destino.
  /// [newDestination]: Novas coordenadas de destino no formato [latitude, longitude].
  Future<void> changeDestination({
    required List<double> newDestination,
  }) async {
    await _methodChannel.invokeMethod('changeDestination', {
      'viewId': _viewId,
      'newDestination': newDestination,
    });
  }

  /// Cancela a sessão de navegação.
  ///
  /// A rota pode permanecer visível no mapa, mas o acompanhamento e as instruções são parados.
  Future<void> cancelNavigation() async {
    await _methodChannel.invokeMethod('cancelNavigation', {
      'viewId': _viewId,
    });
  }

  /// Finaliza a sessão de navegação e limpa a rota do mapa.
  ///
  /// Ideal para quando a corrida é concluída ou deve ser totalmente resetada.
  Future<void> finishNavigation() async {
    await _methodChannel.invokeMethod('finishNavigation', {
      'viewId': _viewId,
    });
  }

  /// Para a navegação (sinônimo de [cancelNavigation]).
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
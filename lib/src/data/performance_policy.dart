
/// Enum para granularidade de localização
enum LocationGranularity {
  high, // Alta precisão (GPS)
  balanced, // Balanceado (GPS + rede)
  low, // Baixa precisão (apenas rede)
}

/// Enum para modo de economia de dados
enum DataSaverMode {
  off,
  balanced,
  aggressive,
}

class PerformancePolicy {
  /// Cooldown mínimo entre requisições de rota (ms)
  final int routeRequestCooldownMs;
  /// Intervalo mínimo entre eventos de localização (ms)
  final int locationEventMinIntervalMs;
  /// Intervalo mínimo entre eventos de progresso de rota (ms)
  final int tripProgressEventMinIntervalMs;
  /// Intervalo mínimo entre eventos de progresso offline (ms)
  final int offlineProgressEventMinIntervalMs;
  /// Ignorar requisições duplicadas de rota
  final bool skipDuplicateRouteRequests;
  /// Granularidade de localização (precisão)
  final LocationGranularity locationGranularity;
  /// Reduzir envio de eventos para o Flutter
  final bool reduceFlutterEvents;
  /// Habilitar/desabilitar cache de rotas
  final bool enableRouteCache;
  /// Habilitar/desabilitar telemetria detalhada
  final bool enableTelemetry;
  /// Modo de economia de dados
  final DataSaverMode dataSaverMode;
  /// Permitir download offline usando dados móveis
  final bool allowOfflineDownloadOnCellular;
  /// Forçar re-download de regiões offline mesmo se já baixadas
  final bool forceOfflineRedownload;

  const PerformancePolicy({
    required this.routeRequestCooldownMs,
    required this.locationEventMinIntervalMs,
    required this.tripProgressEventMinIntervalMs,
    required this.offlineProgressEventMinIntervalMs,
    required this.skipDuplicateRouteRequests,
    required this.locationGranularity,
    required this.reduceFlutterEvents,
    required this.enableRouteCache,
    required this.enableTelemetry,
    required this.dataSaverMode,
    required this.allowOfflineDownloadOnCellular,
    required this.forceOfflineRedownload,
  });

  /// Converte para Map para enviar via MethodChannel
  Map<String, dynamic> toMap() => {
        'routeRequestCooldownMs': routeRequestCooldownMs,
        'locationEventMinIntervalMs': locationEventMinIntervalMs,
        'tripProgressEventMinIntervalMs': tripProgressEventMinIntervalMs,
        'offlineProgressEventMinIntervalMs': offlineProgressEventMinIntervalMs,
        'skipDuplicateRouteRequests': skipDuplicateRouteRequests,
        'locationGranularity': locationGranularity.name,
        'reduceFlutterEvents': reduceFlutterEvents,
        'enableRouteCache': enableRouteCache,
        'enableTelemetry': enableTelemetry,
        'dataSaverMode': dataSaverMode.name,
        'allowOfflineDownloadOnCellular': allowOfflineDownloadOnCellular,
        'forceOfflineRedownload': forceOfflineRedownload,
      };

  /// Preset: modo OFF (sem economia, máxima performance)
  factory PerformancePolicy.off() => const PerformancePolicy(
        routeRequestCooldownMs: 2000,
        locationEventMinIntervalMs: 500,
        tripProgressEventMinIntervalMs: 500,
        offlineProgressEventMinIntervalMs: 500,
        skipDuplicateRouteRequests: false,
        locationGranularity: LocationGranularity.high,
        reduceFlutterEvents: false,
        enableRouteCache: true,
        enableTelemetry: true,
        dataSaverMode: DataSaverMode.off,
        allowOfflineDownloadOnCellular: true,
        forceOfflineRedownload: false,
      );

  /// Preset: modo BALANCED (economia moderada)
  factory PerformancePolicy.balanced() => const PerformancePolicy(
        routeRequestCooldownMs: 5000,
        locationEventMinIntervalMs: 1000,
        tripProgressEventMinIntervalMs: 1000,
        offlineProgressEventMinIntervalMs: 1000,
        skipDuplicateRouteRequests: true,
        locationGranularity: LocationGranularity.balanced,
        reduceFlutterEvents: true,
        enableRouteCache: true,
        enableTelemetry: false,
        dataSaverMode: DataSaverMode.balanced,
        allowOfflineDownloadOnCellular: true,
        forceOfflineRedownload: false,
      );

  /// Preset: modo AGGRESSIVE (máxima economia)
  factory PerformancePolicy.aggressive() => const PerformancePolicy(
        routeRequestCooldownMs: 10000,
        locationEventMinIntervalMs: 3000,
        tripProgressEventMinIntervalMs: 3000,
        offlineProgressEventMinIntervalMs: 3000,
        skipDuplicateRouteRequests: true,
        locationGranularity: LocationGranularity.low,
        reduceFlutterEvents: true,
        enableRouteCache: false,
        enableTelemetry: false,
        dataSaverMode: DataSaverMode.aggressive,
        allowOfflineDownloadOnCellular: false,
        forceOfflineRedownload: false,
      );

  /// Preset: modo customizado (compatível com defaults antigos)
  factory PerformancePolicy.defaults() => PerformancePolicy.balanced();
}


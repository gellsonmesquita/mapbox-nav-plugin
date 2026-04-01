/// Enum para granularidade de localização
enum LocationGranularity {
  high, // Alta precisão (GPS)
  balanced, // Balanceado (GPS + rede)
  low, // Baixa precisão (apenas rede)
}

/// Enum para modo de economia de dados.
enum DataSaverMode {
  off,
  balanced,
  aggressive;

  String get nativeValue => name.toUpperCase();
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

  const PerformancePolicy({
    required this.routeRequestCooldownMs,
    required this.locationEventMinIntervalMs,
    required this.tripProgressEventMinIntervalMs,
    required this.offlineProgressEventMinIntervalMs,
    required this.skipDuplicateRouteRequests,
  });

  Map<String, dynamic> toMap() => {
        'routeRequestCooldownMs': routeRequestCooldownMs,
        'locationEventMinIntervalMs': locationEventMinIntervalMs,
        'tripProgressEventMinIntervalMs': tripProgressEventMinIntervalMs,
        'offlineProgressEventMinIntervalMs': offlineProgressEventMinIntervalMs,
        'skipDuplicateRouteRequests': skipDuplicateRouteRequests,
      };

  /// Preset alinhado ao default Android (sem throttling extra).
  factory PerformancePolicy.off() => const PerformancePolicy(
        routeRequestCooldownMs: 0,
        locationEventMinIntervalMs: 0,
        tripProgressEventMinIntervalMs: 0,
        offlineProgressEventMinIntervalMs: 0,
        skipDuplicateRouteRequests: false,
      );

  /// Preset alinhado ao modo BALANCED Android.
  factory PerformancePolicy.balanced() => const PerformancePolicy(
        routeRequestCooldownMs: 10000,
        locationEventMinIntervalMs: 1500,
        tripProgressEventMinIntervalMs: 2000,
        offlineProgressEventMinIntervalMs: 1500,
        skipDuplicateRouteRequests: true,
      );

  /// Preset alinhado ao modo AGGRESSIVE Android.
  factory PerformancePolicy.aggressive() => const PerformancePolicy(
        routeRequestCooldownMs: 30000,
        locationEventMinIntervalMs: 3000,
        tripProgressEventMinIntervalMs: 5000,
        offlineProgressEventMinIntervalMs: 3000,
        skipDuplicateRouteRequests: true,
      );

  factory PerformancePolicy.defaults() => PerformancePolicy.balanced();
}

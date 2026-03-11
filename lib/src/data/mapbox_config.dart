class VehicleConfig {
  final bool isTruck;
  final double? maxHeight;
  final double? maxWeight;
  final double? maxWidth;
  final RouteProfile profile;
  final List<ExcludeType> excludes;

  VehicleConfig({
    required this.isTruck,
    this.maxHeight,
    this.maxWeight,
    this.maxWidth,
    this.profile = RouteProfile.driving,
    this.excludes = const [],
  });

  Map<String, dynamic> toMap() {
    return {
      "isTruck": isTruck,
      "maxHeight": maxHeight,
      "maxWeight": maxWeight,
      "maxWidth": maxWidth,
      "profile": profile.value,
      "excludeList": excludes.map((e) => e.value).toList(),
    };
  }
}

enum RouteProfile {
  driving('driving'),
  drivingTraffic('driving-traffic');
  final String value;
  const RouteProfile(this.value);
}

enum ExcludeType {
  toll('toll'),
  motorway('motorway'),
  ferry('ferry'),
  tunnel('tunnel'),
  unpaved('unpaved'),
  restricted('restricted');
  final String value;
  const ExcludeType(this.value);
}

class ForbiddenZone {
  final List<Map<String, double>> coordinates; // Lista de {"lat": x, "lng": y}
  ForbiddenZone(this.coordinates);

  List<Map<String, double>> toList() => coordinates;
}
class NavigationBehaviorPolicy {
  final bool autoOverviewOnRouteReady;
  final bool autoFollowOnFirstLocation;
  final bool autoFollowOnDestinationChange;
  final bool autoStartTripSession;

  const NavigationBehaviorPolicy({
    this.autoOverviewOnRouteReady = true,
    this.autoFollowOnFirstLocation = true,
    this.autoFollowOnDestinationChange = true,
    this.autoStartTripSession = true,
  });

  Map<String, dynamic> toMap() => {
        'autoOverviewOnRouteReady': autoOverviewOnRouteReady,
        'autoFollowOnFirstLocation': autoFollowOnFirstLocation,
        'autoFollowOnDestinationChange': autoFollowOnDestinationChange,
        'autoStartTripSession': autoStartTripSession,
      };

  factory NavigationBehaviorPolicy.defaults() =>
      const NavigationBehaviorPolicy();
}


package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import lombok.Getter;

@Getter
public enum RoutingProfile {
    CAR_FASTEST("car_fastest"),
    CAR_SHORTEST("car_shortest"),
    MOTOR_VEHICLE_CUSTOM("motor_vehicle_custom");
    private final String label;

    RoutingProfile(String label) {
        this.label = label;
    }
}

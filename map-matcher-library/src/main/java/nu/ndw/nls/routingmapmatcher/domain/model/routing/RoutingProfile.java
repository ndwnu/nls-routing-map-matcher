package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;

@Getter
public enum RoutingProfile {
    CAR_FASTEST(GlobalConstants.CAR_FASTEST), CAR_SHORTEST(GlobalConstants.CAR_SHORTEST);
    private final String label;

    RoutingProfile(String label) {
        this.label = label;
    }
}

package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Waypoint {
    private final double latitude;
    private final double longitude;
}

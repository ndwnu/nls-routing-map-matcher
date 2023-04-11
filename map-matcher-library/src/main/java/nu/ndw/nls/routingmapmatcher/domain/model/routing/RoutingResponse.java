package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingResponse {

    private final Route route;
    private final List<Waypoint> waypoints;

}

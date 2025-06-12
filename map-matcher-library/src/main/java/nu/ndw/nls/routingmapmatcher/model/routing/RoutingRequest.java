package nu.ndw.nls.routingmapmatcher.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingRequest {

    private final List<Point> wayPoints;
    @Builder.Default
    private final boolean simplifyResponseGeometry = true;
    @Builder.Default
    private final boolean snapToNodes = false;
}

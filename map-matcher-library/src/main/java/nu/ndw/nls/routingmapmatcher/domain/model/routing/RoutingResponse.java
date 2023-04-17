package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingResponse {

    private final double startLinkFraction;
    @Builder.Default
    private final double endLinkFraction = 1;
    private final List<Point> snappedWaypoints;
    private List<Integer> matchedLinkIds;
    private final LineString geometry;
    private final double weight;
    private final long duration;
    private final double distance;
}

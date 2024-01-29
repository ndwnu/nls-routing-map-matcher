package nu.ndw.nls.routingmapmatcher.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
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
    @Builder.Default
    private final List<MatchedLink> matchedLinks = List.of();
    private final LineString geometry;
    private final double weight;
    private final double duration;
    private final double distance;

    /**
     * @deprecated (MatchedLinkIds have been replaced with MatchedLink Objects.)
     */
    @Deprecated(since = "7.0.0", forRemoval = true)
    public List<Integer> getMatchedLinkIds() {
        return matchedLinks.stream().map(MatchedLink::getLinkId).toList();
    }
}

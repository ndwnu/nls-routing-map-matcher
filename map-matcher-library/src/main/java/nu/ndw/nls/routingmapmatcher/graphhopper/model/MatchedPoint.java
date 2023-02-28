package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedPoint {

    private final int matchedLinkId;
    private final boolean reversed;
    private final Point snappedPoint;
    private final double fraction;
    private final double distance;
    private final double bearing;
}

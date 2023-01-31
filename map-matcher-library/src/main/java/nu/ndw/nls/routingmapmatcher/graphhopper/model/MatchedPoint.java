package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import lombok.Builder;
import lombok.Value;
import org.locationtech.jts.geom.Point;

@Value
@Builder
public class MatchedPoint {
    int matchedLinkId;
    Point snappedPoint;
    boolean reversed;
    double distanceToSnappedPoint;
    double fractionOfSnappedPoint;
}

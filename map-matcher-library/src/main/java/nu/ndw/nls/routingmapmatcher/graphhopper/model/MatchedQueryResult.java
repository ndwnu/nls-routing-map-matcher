package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Value
@Builder(toBuilder = true)
@Slf4j
public class MatchedQueryResult {

    Point inputPoint;
    int matchedLinkId;
    BearingRange bearingRange;
    LineString originalGeometry;
    EdgeIteratorTravelDirection travelDirection;
    Geometry cutoffGeometry;
    boolean reversed;

}

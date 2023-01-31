package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.graphhopper.storage.index.QueryResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Value
@Builder(toBuilder = true)
@Slf4j
public class MatchedQueryResult {

    Point inputPoint;
    Double inputMinBearing;
    Double inputMaxBearing;
    QueryResult queryResult;
    TravelDirection travelDirection;
    Geometry cutoffGeometry;

}

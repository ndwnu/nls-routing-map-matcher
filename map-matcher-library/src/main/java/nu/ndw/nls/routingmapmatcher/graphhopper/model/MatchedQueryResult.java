package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.graphhopper.storage.index.QueryResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Value
@Builder(toBuilder = true)
@Slf4j
public class MatchedQueryResult {

    Point inputPoint;
    BearingRange bearingRange;
    QueryResult queryResult;
    TravelDirection travelDirection;
    Geometry cutoffGeometry;

}

package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
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
    @Getter(AccessLevel.NONE)
    Geometry cutoffGeometry;
    boolean reversed;

    public List<LineString> getCutoffGeometriesAsLineStrings() {
        if (cutoffGeometry instanceof MultiLineString) {
            var multiLineString = (MultiLineString) cutoffGeometry;
            List<LineString> lineStrings = new ArrayList<>();
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                lineStrings.add((LineString) multiLineString.getGeometryN(i));
            }
            return lineStrings;
        } else if (cutoffGeometry instanceof LineString) {
            var lineString = (LineString) cutoffGeometry;
            return List.of(lineString);
        } else {
            throw new IllegalStateException("Only LineString or MultiLineString "
                    + "geometry types are supported " + cutoffGeometry.getClass().getName());
        }
    }

}

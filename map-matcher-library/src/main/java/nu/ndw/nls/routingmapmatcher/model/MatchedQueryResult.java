package nu.ndw.nls.routingmapmatcher.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
@Slf4j
public class MatchedQueryResult {

    private final Point inputPoint;
    private final int matchedLinkId;
    private final int matchedReversedLinkId;
    private final BearingFilter bearingFilter;
    private final LineString originalGeometry;
    private final EdgeIteratorTravelDirection travelDirection;
    private final double cutoffDistance;
    @Getter(AccessLevel.NONE)
    private final Geometry cutoffGeometry;
    private final boolean reversed;

    public List<LineString> getCutoffGeometryAsLineStrings() {
        if (cutoffGeometry instanceof MultiLineString multiLineString) {
            List<LineString> lineStrings = new ArrayList<>();
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                lineStrings.add((LineString) multiLineString.getGeometryN(i));
            }
            return lineStrings;
        } else if (cutoffGeometry instanceof LineString lineString) {
            return List.of(lineString);
        } else {
            throw new IllegalStateException("Only LineString or MultiLineString "
                    + "geometry types are supported " + cutoffGeometry.getClass().getName());
        }
    }
}

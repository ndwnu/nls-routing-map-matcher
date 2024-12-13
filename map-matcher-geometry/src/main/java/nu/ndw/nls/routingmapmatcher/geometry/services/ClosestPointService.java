package nu.ndw.nls.routingmapmatcher.geometry.services;


import static com.graphhopper.util.DistancePlaneProjection.DIST_PLANE;

import com.graphhopper.util.shapes.GHPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.bearing.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.geometry.model.ClosestPointResult;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClosestPointService {

    private final BearingCalculator bearingCalculator;

    public ClosestPointResult closestPoint(List<Coordinate> lineString, Coordinate point) {
        ClosestPointResult closestProjectionResult = null;
        for (int i = 1; i < lineString.size(); i++) {
            Coordinate previous = lineString.get(i - 1);
            Coordinate current = lineString.get(i);

            var projectionResult = project(previous, current, point);
            if (closestProjectionResult == null || projectionResult.distance() < closestProjectionResult.distance()) {
                closestProjectionResult = projectionResult;
            }
        }
        if (closestProjectionResult == null) {
            throw new IllegalStateException("failed to project " + point + " on " + lineString);
        }
        return closestProjectionResult;
    }

    private ClosestPointResult project(Coordinate a, Coordinate b, Coordinate r) {
        double distanceToA = DIST_PLANE.calcDist(r.y, r.x, a.y, a.x);
        double distanceToB = DIST_PLANE.calcDist(r.y, r.x, b.y, b.x);

        GHPoint projection;
        if (DIST_PLANE.validEdgeDistance(r.y, r.x, a.y, a.x, b.y, b.x)) {
            projection = DIST_PLANE.calcCrossingPointToEdge(r.y, r.x, a.y, a.x, b.y, b.x);
        } else if (distanceToA < distanceToB) {
            projection = new GHPoint(a.y, a.x);
        } else {
            projection = new GHPoint(b.y, b.x);
        }

        return new ClosestPointResult(
                DIST_PLANE.calcDist(r.y, r.x, projection.lat, projection.lon),
                bearingCalculator.calculateBearing(a, b),
                new Coordinate(projection.lon, projection.lat)
        );
    }
}

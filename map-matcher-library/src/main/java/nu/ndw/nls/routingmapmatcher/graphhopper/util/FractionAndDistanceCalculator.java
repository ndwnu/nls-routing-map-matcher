package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@RequiredArgsConstructor
@Slf4j
public class FractionAndDistanceCalculator {

    public static final double NEAR_ZERO = 0.00000000000001;
    private final GeodeticCalculator geodeticCalculator;

    public double calculateFraction(final LineString line, final Coordinate snappedPointCoordinate) {
        final LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        final LinearLocation snappedPointLocation = locationIndexedLine.indexOf(snappedPointCoordinate);
        final Coordinate[] coordinates = line.getCoordinates();
        double sumOfPathLengths = NEAR_ZERO;
        Double pathDistanceToSnappedPoint = null;
        for (int i = 0; i < coordinates.length; i++) {
            final Coordinate current = coordinates[i];
            if (i == snappedPointLocation.getSegmentIndex()) {
                pathDistanceToSnappedPoint = sumOfPathLengths + calculateDistance(current, snappedPointCoordinate);
            }
            if (i + 1 < coordinates.length) {
                final Coordinate next = coordinates[i + 1];
                sumOfPathLengths += calculateDistance(current, next);
            }
        }
        if (pathDistanceToSnappedPoint == null) {
            throw new IllegalStateException("Failed to find path distance to snapped point");
        }
        double fraction = pathDistanceToSnappedPoint / sumOfPathLengths;

        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);
        return fraction;
    }

    public double calculateDistance(final Coordinate from, final Coordinate to) {
        geodeticCalculator.setStartingGeographicPoint(to.getX(), to.getY());
        geodeticCalculator.setDestinationGeographicPoint(from.getX(), from.getY());
        return geodeticCalculator.getOrthodromicDistance();
    }
}

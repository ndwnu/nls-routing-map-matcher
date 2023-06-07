package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.FractionAndDistance;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@RequiredArgsConstructor
@Slf4j
public class FractionAndDistanceCalculator {

    private static final double NEAR_ZERO = 0.00000000000001;
    private final GeodeticCalculator geodeticCalculator;

    public FractionAndDistance calculateFractionAndDistance(LineString line, Coordinate snappedPointCoordinate) {
        LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        LinearLocation snappedPointLocation = locationIndexedLine.indexOf(snappedPointCoordinate);
        Coordinate[] coordinates = line.getCoordinates();
        double sumOfPathLengths = NEAR_ZERO;
        Double pathDistanceToSnappedPoint = null;
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate current = coordinates[i];
            if (i == snappedPointLocation.getSegmentIndex()) {
                pathDistanceToSnappedPoint = sumOfPathLengths + calculateDistance(current, snappedPointCoordinate);
            }
            if (i + 1 < coordinates.length) {
                Coordinate next = coordinates[i + 1];
                sumOfPathLengths += calculateDistance(current, next);
            }
        }
        if (pathDistanceToSnappedPoint == null) {
            throw new IllegalStateException("Failed to find path distance to snapped point");
        }
        double fraction = pathDistanceToSnappedPoint / sumOfPathLengths;

        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);
        return FractionAndDistance
                .builder()
                .fraction(fraction)
                .fractionDistance(pathDistanceToSnappedPoint)
                .totalDistance(sumOfPathLengths)
                .build();
    }

    private double calculateDistance(Coordinate from, Coordinate to) {
        geodeticCalculator.setStartingGeographicPoint(to.getX(), to.getY());
        geodeticCalculator.setDestinationGeographicPoint(from.getX(), from.getY());
        return geodeticCalculator.getOrthodromicDistance();
    }
}

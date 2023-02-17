package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import java.util.Arrays;
import java.util.Iterator;
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

    private final GeodeticCalculator geodeticCalculator;

    public double calculateFraction(LineString line, Coordinate snappedPointCoordinate,
            boolean reversed) {
        final LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        final LinearLocation snappedPointLocation = locationIndexedLine.indexOf(snappedPointCoordinate);
        final Iterator<Coordinate> pointList = Arrays.asList(line.getCoordinates()).iterator();
        Coordinate previous = pointList.next();
        double sumOfPathLengths = 0D;
        Double pathDistanceToSnappedPoint = null;
        while (pointList.hasNext()) {
            Coordinate current = pointList.next();
            final LinearLocation previousIndex = locationIndexedLine.indexOf(previous);
            final LinearLocation currentIndex = locationIndexedLine.indexOf(current);
            if (snappedPointLocation.getSegmentIndex() == previousIndex.getSegmentIndex()) {
                final double previousToSnappedPointDistance = calculateDistance(snappedPointCoordinate, previous);
                pathDistanceToSnappedPoint = sumOfPathLengths + previousToSnappedPointDistance;
            } else if (!pointList.hasNext()
                    && snappedPointLocation.getSegmentIndex() == currentIndex.getSegmentIndex()) {
                final double currentToSnappedPointDistance = calculateDistance(snappedPointCoordinate, current);
                pathDistanceToSnappedPoint = sumOfPathLengths + currentToSnappedPointDistance;
            }
            sumOfPathLengths += calculateDistance(previous, current);
            // Prepare for next loop
            previous = current;
        }
        if (pathDistanceToSnappedPoint == null) {
            throw new IllegalStateException("Failed to find path distance to snapped point");
        }
        double fraction = pathDistanceToSnappedPoint / sumOfPathLengths;
        if (reversed) {
            log.trace("Reverse travel direction. Fraction will be inverted.");
            fraction = 1D - fraction;
        }
        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);
        return fraction;
    }

    public double calculateDistance(Coordinate from, Coordinate to) {
        geodeticCalculator.setStartingGeographicPoint(to
                        .getX(),
                to.getY());
        geodeticCalculator.setDestinationGeographicPoint(from.getX(), from.getY());
        return geodeticCalculator.getOrthodromicDistance();
    }
}

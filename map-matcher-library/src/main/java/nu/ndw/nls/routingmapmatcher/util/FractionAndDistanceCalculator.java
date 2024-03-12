package nu.ndw.nls.routingmapmatcher.util;

import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.WGS84_GEOMETRY_FACTORY;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.model.FractionAndDistance;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@Slf4j
public class FractionAndDistanceCalculator {

    private static final double DISTANCE_TOLERANCE_1_CM = 0.01;
    private static final GeodeticCalculator GEODETIC_CALCULATOR = new GeodeticCalculator();

    public static FractionAndDistance calculateFractionAndDistance(LineString line, Coordinate inputCoordinate) {
        LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        LinearLocation snappedPointLocation = locationIndexedLine.project(inputCoordinate);
        Coordinate snappedPointCoordinate = snappedPointLocation.getCoordinate(line);
        Coordinate[] coordinates = line.getCoordinates();
        double sumOfPathLengths = 0;
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
        double fraction = sumOfPathLengths > 0 ? (pathDistanceToSnappedPoint / sumOfPathLengths) : sumOfPathLengths;

        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);
        return FractionAndDistance
                .builder()
                .fraction(fraction)
                .fractionDistance(pathDistanceToSnappedPoint)
                .totalDistance(sumOfPathLengths)
                .build();
    }

    public static double calculateLengthInMeters(LineString lineString) {
        Coordinate[] coordinates = lineString.getCoordinates();
        return IntStream.range(1, coordinates.length)
                .mapToDouble(index -> calculateDistance(coordinates[index - 1], coordinates[index]))
                .sum();
    }

    /**
     * Extract a subsection from the provided lineString, starting at 0 and ending at the provided fraction.
     */
    public static LineString getSubLineString(LineString lineString, double fraction) {
        double sumOfPathLengths = 0;
        Coordinate[] coordinates = lineString.getCoordinates();
        double fractionLength = fraction * calculateLengthInMeters(lineString);
        List<Coordinate> result = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate current = coordinates[i];
            result.add(current);
            if (i + 1 < coordinates.length) {
                Coordinate next = coordinates[i + 1];
                double lengthBefore = sumOfPathLengths;
                sumOfPathLengths += calculateDistance(current, next);
                if (fractionLength >= lengthBefore && fractionLength < sumOfPathLengths) {
                    double distance = fractionLength - lengthBefore;
                    // Don't introduce an extra point if it's within 1 cm of the last point from the original geometry.
                    if (distance > DISTANCE_TOLERANCE_1_CM) {
                        GEODETIC_CALCULATOR.setStartingGeographicPoint(current.getX(), current.getY());
                        GEODETIC_CALCULATOR.setDestinationGeographicPoint(next.getX(), next.getY());
                        double azimuth = GEODETIC_CALCULATOR.getAzimuth();
                        GEODETIC_CALCULATOR.setDirection(azimuth, distance);
                        Point2D point = GEODETIC_CALCULATOR.getDestinationGeographicPoint();
                        result.add(new Coordinate(point.getX(), point.getY()));
                    } else if (i == 0) {
                        // Make sure the result always consists of at least two coordinates.
                        result.add(current);
                    }
                    break;
                }
            }
        }
        return WGS84_GEOMETRY_FACTORY.createLineString(result.toArray(new Coordinate[0]));
    }

    private static double calculateDistance(Coordinate from, Coordinate to) {
        GEODETIC_CALCULATOR.setStartingGeographicPoint(to.getX(), to.getY());
        GEODETIC_CALCULATOR.setDestinationGeographicPoint(from.getX(), from.getY());
        return GEODETIC_CALCULATOR.getOrthodromicDistance();
    }
}

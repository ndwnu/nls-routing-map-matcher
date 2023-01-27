package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.TravelDirection;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@RequiredArgsConstructor
@Slf4j
public class PointMatchingService {

    private static final int MAX_BEARING = 360;
    private static final int MIN_BEARING = 0;
    public static final int ALL_NODES = 3;
    private final GeometryFactory geometryFactory;

    private final LinkFlagEncoder flagEncoder;

    private final GeodeticCalculator geodeticCalculator;


    List<MatchedPoint> calculateMatches(MatchedQueryResult matchedQueryResult) {
        final List<MatchedPoint> matchedPoints = new ArrayList<>();
        final Coordinate[] coordinates = matchedQueryResult.getCutoffGeometry().getCoordinates();
        final Point inputPoint = matchedQueryResult.getInputPoint();
        final double minInputBearing = matchedQueryResult.getInputMinBearing();
        final double maxOutputBearing = matchedQueryResult.getInputMaxBearing();
        final QueryResult queryResult = matchedQueryResult.getQueryResult();
        final TravelDirection travelDirection = matchedQueryResult.getTravelDirection();
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        boolean lastBoundaryDetected;
        var coordinateIterator = Arrays.asList(coordinates).iterator();
        Coordinate currentCoordinate = coordinateIterator.next();
        while (coordinateIterator.hasNext()) {
            final Coordinate nextCoordinate = coordinateIterator.next();
            double convertedBearing = calculateBearing(currentCoordinate, nextCoordinate);
            if (bearingIsInRange(convertedBearing, minInputBearing, maxOutputBearing)) {
                partialGeometry.add(currentCoordinate);
                partialGeometry.add(nextCoordinate);
                lastBoundaryDetected = false;
            } else {
                lastBoundaryDetected = true;
                subGeometries.add(geometryFactory
                        .createLineString(partialGeometry.toArray(Coordinate[]::new)));
                partialGeometry = new ArrayList<>();
            }
            if (!coordinateIterator.hasNext() && !lastBoundaryDetected) {
                subGeometries.add(geometryFactory
                        .createLineString(partialGeometry.toArray(Coordinate[]::new)));
            }

            currentCoordinate = nextCoordinate;
            log.info("Segment [{} -> {}]. bearing is: {}", currentCoordinate, nextCoordinate, convertedBearing);
        }
        subGeometries.forEach(l -> {
                    final Point snappedPoint = calculateSnappedPoint(l, inputPoint);
                    final double fraction = calculateFraction(snappedPoint, queryResult, travelDirection);
                    final IntsRef flags = queryResult.getClosestEdge().getFlags();
                    final int matchedLinkId = flagEncoder.getId(flags);
                    final double distanceToSnappedPoint = calculateDistance(inputPoint.getCoordinate(),
                            snappedPoint.getCoordinate());

                    final MatchedPoint lineSegmentBearing = MatchedPoint
                            .builder()
                            .matchedLinkId(matchedLinkId)
                            .fractionOfSnappedPoint(fraction)
                            .distanceToSnappedPoint(distanceToSnappedPoint)
                            .snappedPoint(snappedPoint)
                            .build();

                    matchedPoints.add(lineSegmentBearing);
                }
        );
        return matchedPoints;
    }

    private double calculateFraction(Point snappedPoint, QueryResult queryResult, TravelDirection travelDirection) {
        final LineString originalGeometry = queryResult
                .getClosestEdge()
                .fetchWayGeometry(ALL_NODES)
                .toLineString(false);
        return getFraction(originalGeometry, snappedPoint.getCoordinate(), travelDirection);
    }

    private Point calculateSnappedPoint(LineString subGeometry, Point inputPoint) {
        final LocationIndexedLine lineIndex = new LocationIndexedLine(subGeometry);
        final LinearLocation snappedPointLinearLocation = lineIndex.project(inputPoint.getCoordinate());
        return geometryFactory
                .createPoint(lineIndex.extractPoint(snappedPointLinearLocation));
    }

    boolean bearingIsInRange(double convertedBearing, double inputMinBearing, double inputMaxBearing) {
        double minBearingStandardised = inputMinBearing % MAX_BEARING;
        double maxBearingStandardised = inputMaxBearing % MAX_BEARING;
        if (minBearingStandardised > maxBearingStandardised) {
            return (convertedBearing >= minBearingStandardised && convertedBearing <= MAX_BEARING) || (
                    convertedBearing >= MIN_BEARING && convertedBearing <= maxBearingStandardised);

        } else {
            return convertedBearing >= minBearingStandardised && convertedBearing <= maxBearingStandardised;
        }
    }

    private double calculateBearing(Coordinate currentCoordinate, Coordinate nextCoordinate) {
        geodeticCalculator.setStartingGeographicPoint(currentCoordinate.getX(),
                currentCoordinate.getY());
        geodeticCalculator.setDestinationGeographicPoint(nextCoordinate.getX(),
                nextCoordinate.getY());
        final double bearing = geodeticCalculator.getAzimuth();
        return bearing < 0.0 ? bearing + MAX_BEARING : bearing;
    }

    private double getFraction(LineString line, Coordinate coordinate, TravelDirection travelDirection) {
        final LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        final LinearLocation snappedPointLocation = locationIndexedLine.indexOf(coordinate);
        final Iterator<Coordinate> pointList = Arrays.asList(line.getCoordinates()).iterator();
        Coordinate previous = pointList.next();
        double sumOfPathLengths = 0D;
        Double pathDistanceToSnappedPoint = null;
        while (pointList.hasNext()) {
            Coordinate current = pointList.next();
            final LinearLocation previousIndex = locationIndexedLine.indexOf(previous);
            if (snappedPointLocation.getSegmentIndex() == previousIndex.getSegmentIndex()) {
                final double previousToSnappedPointDistance = calculateDistance(coordinate, previous);
                pathDistanceToSnappedPoint = sumOfPathLengths + previousToSnappedPointDistance;
            }
            sumOfPathLengths += calculateDistance(previous, current);
            // Prepare for next loop
            previous = current;
        }
        if (pathDistanceToSnappedPoint == null) {
            throw new IllegalStateException("Failed to find path distance to snapped point");
        }
        double fraction = pathDistanceToSnappedPoint / sumOfPathLengths;
        if (travelDirection == TravelDirection.REVERSED) {
            log.trace("Reverse travel direction. Fraction will be inverted.");
            fraction = 1D - fraction;
        }
        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);
        return fraction;
    }

    private double calculateDistance(Coordinate from, Coordinate to) {
        geodeticCalculator.setStartingGeographicPoint(to
                        .getX(),
                to.getY());
        geodeticCalculator.setDestinationGeographicPoint(from.getX(), from.getY());
        return geodeticCalculator.getOrthodromicDistance();
    }

}

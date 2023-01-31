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
    private static final int ALL_NODES = 3;
    private final GeometryFactory geometryFactory;

    private final LinkFlagEncoder flagEncoder;

    private final GeodeticCalculator geodeticCalculator;


    public List<MatchedPoint> calculateMatches(MatchedQueryResult matchedQueryResult) {
        final List<MatchedPoint> matchedPoints = new ArrayList<>();
        final Coordinate[] coordinates = matchedQueryResult.getCutoffGeometry().getCoordinates();
        final Coordinate[] coordinatesReversed = matchedQueryResult.getCutoffGeometry().reverse().getCoordinates();
        final Point inputPoint = matchedQueryResult.getInputPoint();
        final Double minInputBearing = matchedQueryResult.getInputMinBearing();
        final Double maxInputBearing = matchedQueryResult.getInputMaxBearing();
        final QueryResult queryResult = matchedQueryResult.getQueryResult();
        final TravelDirection travelDirection = matchedQueryResult.getTravelDirection();

        createAggregatedSubGeometries(coordinates, minInputBearing, maxInputBearing)
                .forEach(lineString -> {
                            final MatchedPoint lineSegmentBearing = createMatchedPoint(inputPoint,
                                    queryResult, travelDirection, lineString);
                            matchedPoints.add(lineSegmentBearing);
                        }
                );

        if (travelDirection == TravelDirection.BOTH_DIRECTIONS) {
            createAggregatedSubGeometries(coordinatesReversed, minInputBearing, maxInputBearing)
                    .forEach(lineString -> {
                                final MatchedPoint lineSegmentBearing = createMatchedPoint(inputPoint,
                                        queryResult,
                                        TravelDirection.REVERSED,
                                        lineString);
                                matchedPoints.add(lineSegmentBearing);
                            }

                    );
        }
        return matchedPoints;
    }

    private MatchedPoint createMatchedPoint(Point inputPoint,
            QueryResult queryResult,
            TravelDirection travelDirection,
            LineString aggregatedGeometry) {
        final Point snappedPoint = calculateSnappedPoint(aggregatedGeometry, inputPoint);
        final double fraction = calculateFraction(snappedPoint, queryResult, travelDirection);
        final IntsRef flags = queryResult.getClosestEdge().getFlags();
        final int matchedLinkId = flagEncoder.getId(flags);
        final double distanceToSnappedPoint = calculateDistance(inputPoint.getCoordinate(),
                snappedPoint.getCoordinate());
        return MatchedPoint
                .builder()
                .reversed(TravelDirection.REVERSED == travelDirection)
                .matchedLinkId(matchedLinkId)
                .fractionOfSnappedPoint(fraction)
                .distanceToSnappedPoint(distanceToSnappedPoint)
                .snappedPoint(snappedPoint)
                .build();
    }

    private List<LineString> createAggregatedSubGeometries(Coordinate[] coordinates, Double minInputBearing,
            Double maxInputBearing) {
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        var coordinateIterator = Arrays.asList(coordinates).iterator();
        Coordinate currentCoordinate = coordinateIterator.next();
        while (coordinateIterator.hasNext()) {
            final Coordinate nextCoordinate = coordinateIterator.next();
            double convertedBearing = calculateBearing(currentCoordinate, nextCoordinate);
            //While bearing is in range add coordinates to partialGeometry
            if (bearingIsInRange(convertedBearing, minInputBearing, maxInputBearing)) {
                partialGeometry.add(currentCoordinate);
                partialGeometry.add(nextCoordinate);
                // Stop condition last coordinate add partialGeometry if present
                if (!coordinateIterator.hasNext()){
                    subGeometries.add(geometryFactory
                            .createLineString(partialGeometry.toArray(Coordinate[]::new)));
                }

            } else {
                //Bearing is out of range add result to subGeometries and reinitialize
                if (!partialGeometry.isEmpty()) {
                    subGeometries.add(geometryFactory
                            .createLineString(partialGeometry.toArray(Coordinate[]::new)));
                    partialGeometry = new ArrayList<>();
                }
            }

            log.debug("Segment [{} -> {}]. bearing is: {}", currentCoordinate, nextCoordinate, convertedBearing);
            currentCoordinate = nextCoordinate;

        }
        return subGeometries;
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

    private boolean bearingIsInRange(double convertedBearing, Double inputMinBearing, Double inputMaxBearing) {
        // If no bearing is provided return true
        if (inputMinBearing == null || inputMaxBearing == null) {
            return true;
        }
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

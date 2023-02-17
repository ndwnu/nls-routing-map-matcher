package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@RequiredArgsConstructor
@Slf4j
public class PointMatchingService {

    private final GeometryFactory geometryFactory;

    private final BearingCalculator bearingCalculator;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    public List<MatchedPoint> calculateMatches(MatchedQueryResult matchedQueryResult) {
        final List<MatchedPoint> matchedPoints = new ArrayList<>();
        final Coordinate[] coordinates = matchedQueryResult.getCutoffGeometry().getCoordinates();
        final Point inputPoint = matchedQueryResult.getInputPoint();
        final BearingRange bearingRange = matchedQueryResult.getBearingRange();
        final LineString originalGeometry = matchedQueryResult.getOriginalGeometry();
        final EdgeIteratorTravelDirection travelDirection = matchedQueryResult.getTravelDirection();
        final int matchedLinkId = matchedQueryResult.getMatchedLinkId();

        createAggregatedSubGeometries(coordinates, bearingRange)
                .forEach(lineString -> {
                            final MatchedPoint matchedPoint = createMatchedPoint(inputPoint,
                                    matchedLinkId,
                                    originalGeometry, false, lineString);
                            matchedPoints.add(matchedPoint);
                        }
                );

        if (travelDirection == EdgeIteratorTravelDirection.BOTH_DIRECTIONS) {
            final Coordinate[] coordinatesReversed = matchedQueryResult.getCutoffGeometry().reverse().getCoordinates();
            createAggregatedSubGeometries(coordinatesReversed, bearingRange)
                    .forEach(lineString -> {
                                final MatchedPoint matchedPoint = createMatchedPoint(inputPoint,
                                        matchedLinkId,
                                        originalGeometry,
                                        true,
                                        lineString);
                                matchedPoints.add(matchedPoint);
                            }

                    );
        }
        return matchedPoints;
    }

    private MatchedPoint createMatchedPoint(Point inputPoint,
            int matchedLinkId,
            LineString originalGeometry,
            boolean reversed,
            LineString aggregatedGeometry

    ) {
        final Point snappedPoint = calculateSnappedPoint(aggregatedGeometry, inputPoint);
        final double fraction = calculateFraction(snappedPoint, originalGeometry, reversed);

        final double distanceToSnappedPoint = fractionAndDistanceCalculator.calculateDistance(
                inputPoint.getCoordinate(),
                snappedPoint.getCoordinate());
        return MatchedPoint
                .builder()
                .reversed(reversed)
                .matchedLinkId(matchedLinkId)
                .fractionOfSnappedPoint(fraction)
                .distanceToSnappedPoint(distanceToSnappedPoint)
                .snappedPoint(snappedPoint)
                .build();
    }

    private List<LineString> createAggregatedSubGeometries(Coordinate[] coordinates, BearingRange bearingRange) {
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        var coordinateIterator = Arrays.asList(coordinates).iterator();
        Coordinate currentCoordinate = coordinateIterator.next();
        while (coordinateIterator.hasNext()) {
            final Coordinate nextCoordinate = coordinateIterator.next();
            double convertedBearing = bearingCalculator.calculateBearing(currentCoordinate, nextCoordinate);
            //While bearing is in range add coordinates to partialGeometry
            if (bearingCalculator.bearingIsInRange(convertedBearing, bearingRange)) {
                partialGeometry.add(currentCoordinate);
                partialGeometry.add(nextCoordinate);
                // Stop condition last coordinate add partialGeometry if present
                if (!coordinateIterator.hasNext()) {
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

    private double calculateFraction(Point snappedPoint, LineString originalGeometry, boolean reversed) {

        return fractionAndDistanceCalculator.calculateFraction(originalGeometry,
                snappedPoint.getCoordinate(),
                reversed);
    }

    private Point calculateSnappedPoint(LineString subGeometry, Point inputPoint) {
        final LocationIndexedLine lineIndex = new LocationIndexedLine(subGeometry);
        final LinearLocation snappedPointLinearLocation = lineIndex.project(inputPoint.getCoordinate());
        return geometryFactory
                .createPoint(lineIndex.extractPoint(snappedPointLinearLocation));
    }

}

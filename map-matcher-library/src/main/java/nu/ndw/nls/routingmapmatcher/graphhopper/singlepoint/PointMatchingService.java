package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
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
        List<MatchedPoint> matchedPoints = new ArrayList<>();
        Coordinate inputCoordinate = matchedQueryResult.getInputPoint().getCoordinate();
        BearingFilter bearingFilter = matchedQueryResult.getBearingFilter();
        LineString originalGeometry = matchedQueryResult.getOriginalGeometry();
        EdgeIteratorTravelDirection travelDirection = matchedQueryResult.getTravelDirection();
        int matchedLinkId = matchedQueryResult.getMatchedLinkId();
        matchedQueryResult.getCutoffGeometryAsLineStrings()
                .forEach(cutOffGeometry -> {
                            Coordinate[] coordinates = cutOffGeometry.getCoordinates();
                            createAggregatedSubGeometries(coordinates, bearingFilter)
                                    .forEach(lineString -> {
                                                MatchedPoint matchedPoint = createMatchedPoint(inputCoordinate,
                                                        matchedLinkId, originalGeometry, false, lineString);
                                                matchedPoints.add(matchedPoint);
                                            }
                                    );
                            if (travelDirection == EdgeIteratorTravelDirection.BOTH_DIRECTIONS) {
                                Coordinate[] coordinatesReversed = cutOffGeometry.reverse().getCoordinates();
                                createAggregatedSubGeometries(coordinatesReversed, bearingFilter)
                                        .forEach(lineString -> {
                                                    MatchedPoint matchedPoint = createMatchedPoint(inputCoordinate,
                                                            matchedLinkId, originalGeometry, true, lineString);
                                                    matchedPoints.add(matchedPoint);
                                                }
                                        );
                            }
                        }
                );
        return matchedPoints;
    }

    private MatchedPoint createMatchedPoint(Coordinate inputCoordinate, int matchedLinkId, LineString originalGeometry,
            boolean reversed, LineString aggregatedGeometry) {
        LocationIndexedLine lineIndex = new LocationIndexedLine(aggregatedGeometry);
        LinearLocation snappedPointLinearLocation = lineIndex.project(inputCoordinate);
        LineSegment snappedPointSegment = snappedPointLinearLocation.getSegment(aggregatedGeometry);
        Coordinate snappedCoordinate = snappedPointSegment.closestPoint(inputCoordinate);

        Point snappedPoint = geometryFactory.createPoint(snappedCoordinate);
        double fraction = fractionAndDistanceCalculator
                .calculateFractionAndDistance(originalGeometry, snappedCoordinate).getFraction();
        double distance = fractionAndDistanceCalculator.calculateDistance(inputCoordinate, snappedCoordinate);
        double bearing = bearingCalculator.calculateBearing(snappedPointSegment.p0, snappedPointSegment.p1);

        return MatchedPoint
                .builder()
                .matchedLinkId(matchedLinkId)
                .reversed(reversed)
                .snappedPoint(snappedPoint)
                .fraction(fraction)
                .distance(distance)
                .bearing(bearing)
                .build();
    }

    private List<LineString> createAggregatedSubGeometries(Coordinate[] coordinates, BearingFilter bearingFilter) {
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        for (int i = 1; i < coordinates.length; i++) {
            Coordinate currentCoordinate = coordinates[i - 1];
            Coordinate nextCoordinate = coordinates[i];
            double convertedBearing = bearingCalculator.calculateBearing(currentCoordinate, nextCoordinate);
            // While bearing is in range, add coordinates to partialGeometry
            if (bearingCalculator.bearingIsInRange(convertedBearing, bearingFilter)) {
                if (partialGeometry.isEmpty()) {
                    partialGeometry.add(currentCoordinate);
                }
                partialGeometry.add(nextCoordinate);
                // Stop condition: last coordinate. Add partialGeometry if present
                if (i == coordinates.length - 1) {
                    subGeometries.add(geometryFactory.createLineString(partialGeometry.toArray(Coordinate[]::new)));
                }
            } else {
                // Bearing is out of range. Add result to subGeometries and reinitialize
                if (!partialGeometry.isEmpty()) {
                    subGeometries.add(geometryFactory.createLineString(partialGeometry.toArray(Coordinate[]::new)));
                    partialGeometry = new ArrayList<>();
                }
            }

            log.debug("Segment [{} -> {}]. bearing is: {}", currentCoordinate, nextCoordinate, convertedBearing);
        }
        return subGeometries;
    }
}

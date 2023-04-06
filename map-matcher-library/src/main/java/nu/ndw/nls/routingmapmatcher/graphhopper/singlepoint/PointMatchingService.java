package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Direction;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
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

    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private final GeometryFactory geometryFactory;

    private final BearingCalculator bearingCalculator;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    public List<MatchedPoint> calculateMatches(MatchedQueryResult matchedQueryResult) {
        List<MatchedPoint> matchedPoints = new ArrayList<>();
        Coordinate inputCoordinate = matchedQueryResult.getInputPoint().getCoordinate();
        BearingFilter bearingFilter = matchedQueryResult.getBearingFilter();
        double cutoffDistance = matchedQueryResult.getCutoffDistance();
        LineString originalGeometry = matchedQueryResult.getOriginalGeometry();
        EdgeIteratorTravelDirection travelDirection = matchedQueryResult.getTravelDirection();
        int matchedLinkId = matchedQueryResult.getMatchedLinkId();
        matchedQueryResult.getCutoffGeometryAsLineStrings()
                .forEach(cutOffGeometry -> {
                            Coordinate[] coordinates = cutOffGeometry.getCoordinates();
                            createAggregatedSubGeometries(coordinates, bearingFilter)
                                    .forEach(lineString -> {
                                                MatchedPoint matchedPoint = createMatchedPoint(
                                                        inputCoordinate,
                                                        matchedLinkId,
                                                        originalGeometry,
                                                        false,
                                                        lineString,
                                                        bearingFilter,
                                                        cutoffDistance
                                                );
                                                matchedPoints.add(matchedPoint);
                                            }
                                    );
                            if (travelDirection == EdgeIteratorTravelDirection.BOTH_DIRECTIONS) {
                                Coordinate[] coordinatesReversed = cutOffGeometry.reverse().getCoordinates();
                                createAggregatedSubGeometries(coordinatesReversed, bearingFilter)
                                        .forEach(lineString -> {
                                                    MatchedPoint matchedPoint = createMatchedPoint(
                                                            inputCoordinate,
                                                            matchedLinkId,
                                                            originalGeometry,
                                                            true,
                                                            lineString,
                                                            bearingFilter,
                                                            cutoffDistance
                                                    );
                                                    matchedPoints.add(matchedPoint);
                                                }
                                        );
                            }
                        }
                );
        return matchedPoints;
    }

    private MatchedPoint createMatchedPoint(Coordinate inputCoordinate, int matchedLinkId, LineString originalGeometry,
            boolean reversed, LineString aggregatedGeometry, BearingFilter bearingFilter, double cutoffDistance) {
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
                .direction(reversed ? Direction.BACKWARD : Direction.FORWARD)
                .snappedPoint(snappedPoint)
                .fraction(reversed ? 1 - fraction : fraction)
                .distance(distance)
                .reliability(calculateReliability(distance, bearing, bearingFilter, cutoffDistance))
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

    private double calculateReliability(double distance, double bearing, BearingFilter bearingFilter,
            double cutoffDistance) {
        double distancePenalty = distance / cutoffDistance;
        double bearingPenalty = Optional.ofNullable(bearingFilter)
                .map(bf -> bearingCalculator.bearingDelta(
                        bearing,
                        bf.target()) /
                        bf.cutoffMargin())
                .orElse(0.0);
        return Math.max(MIN_RELIABILITY_SCORE, (1 - distancePenalty - bearingPenalty) * MAX_RELIABILITY_SCORE);
    }
}

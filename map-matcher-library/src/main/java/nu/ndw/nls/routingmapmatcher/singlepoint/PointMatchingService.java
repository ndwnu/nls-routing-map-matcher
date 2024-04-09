package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter.toGeometryFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.bearing.BearingCalculator;
import nu.ndw.nls.geometry.constants.SRID;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.geo.model.ProjectionResult;
import nu.ndw.nls.routingmapmatcher.geo.services.ClosestPointService;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchedPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

@RequiredArgsConstructor
@Slf4j
public class PointMatchingService {

    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private final GeometryFactory geometryFactory;
    private final BearingCalculator bearingCalculator;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final ClosestPointService closestPointService;

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

    private MatchedPoint createMatchedPoint(Coordinate input, int matchedLinkId, LineString originalGeometry,
            boolean reversed, LineString aggregatedGeometry, BearingFilter bearingFilter, double cutoffDistance) {
        ProjectionResult projectionResult = closestPointService.closestPoint(
                Arrays.asList(aggregatedGeometry.getCoordinates()), input);
        double fraction = fractionAndDistanceCalculator
                .calculateFractionAndDistance(originalGeometry, projectionResult.point()).getFraction();

        return MatchedPoint
                .builder()
                .matchedLinkId(matchedLinkId)
                .reversed(reversed)
                .snappedPoint(geometryFactory.createPoint(projectionResult.point()))
                .fraction(reversed ? (1 - fraction) : fraction)
                .distance(projectionResult.distance())
                .reliability(
                        calculateReliability(projectionResult.distance(), projectionResult.bearing(), bearingFilter,
                                cutoffDistance))
                .bearing(projectionResult.bearing())
                .build();
    }

    private List<LineString> createAggregatedSubGeometries(Coordinate[] coordinates, BearingFilter bearingFilter) {
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        for (int i = 1; i < coordinates.length; i++) {
            Coordinate currentCoordinate = coordinates[i - 1];
            Coordinate nextCoordinate = coordinates[i];
            double convertedBearing = bearingCalculator.calculateBearing(currentCoordinate, nextCoordinate, SRID.WGS84);
            // While bearing is in range, add coordinates to partialGeometry
            if (bearingCalculator.bearingIsInRange(convertedBearing, toGeometryFilter(bearingFilter))) {
                if (partialGeometry.isEmpty()) {
                    partialGeometry.add(currentCoordinate);
                }
                partialGeometry.add(nextCoordinate);
                // Stop condition: last coordinate. Add partialGeometry if present
                if (i == coordinates.length - 1) {
                    subGeometries.add(geometryFactory.createLineString(partialGeometry.toArray(Coordinate[]::new)));
                }
            } else {
                // Bearing is out of range. Add the result to subGeometries and reinitialize
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

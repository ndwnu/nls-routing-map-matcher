package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.TravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
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

    private static final int ALL_NODES = 3;
    private final GeometryFactory geometryFactory;

    private final LinkFlagEncoder flagEncoder;

    private final BearingCalculator bearingCalculator;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

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
                            final MatchedPoint matchedPoint = createMatchedPoint(inputPoint,
                                    queryResult, travelDirection, lineString);
                            matchedPoints.add(matchedPoint);
                        }
                );

        if (travelDirection == TravelDirection.BOTH_DIRECTIONS) {
            createAggregatedSubGeometries(coordinatesReversed, minInputBearing, maxInputBearing)
                    .forEach(lineString -> {
                                final MatchedPoint matchedPoint = createMatchedPoint(inputPoint,
                                        queryResult,
                                        TravelDirection.REVERSED,
                                        lineString);
                                matchedPoints.add(matchedPoint);
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
        final double distanceToSnappedPoint = fractionAndDistanceCalculator.calculateDistance(inputPoint.getCoordinate(),
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
            double convertedBearing = bearingCalculator.calculateBearing(currentCoordinate, nextCoordinate);
            //While bearing is in range add coordinates to partialGeometry
            if (bearingCalculator.bearingIsInRange(convertedBearing, minInputBearing, maxInputBearing)) {
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

    private double calculateFraction(Point snappedPoint, QueryResult queryResult, TravelDirection travelDirection) {
        final LineString originalGeometry = queryResult
                .getClosestEdge()
                .fetchWayGeometry(ALL_NODES)
                .toLineString(false);
        return fractionAndDistanceCalculator.calculateFraction(originalGeometry,
                snappedPoint.getCoordinate(),
                travelDirection);
    }

    private Point calculateSnappedPoint(LineString subGeometry, Point inputPoint) {
        final LocationIndexedLine lineIndex = new LocationIndexedLine(subGeometry);
        final LinearLocation snappedPointLinearLocation = lineIndex.project(inputPoint.getCoordinate());
        return geometryFactory
                .createPoint(lineIndex.extractPoint(snappedPointLinearLocation));
    }

}

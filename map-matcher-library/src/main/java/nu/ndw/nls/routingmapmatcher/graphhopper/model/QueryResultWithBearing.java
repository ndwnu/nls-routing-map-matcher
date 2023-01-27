package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@Value
@Builder(toBuilder = true)
@Slf4j
public class QueryResultWithBearing {

    public static final int MAX_BEARING = 360;
    public static final int MIN_BEARING = 0;
    Point inputPoint;
    Double inputMinBearing;
    Double inputMaxBearing;
    QueryResult queryResult;
    TravelDirection travelDirection;
    Geometry cutoffGeometry;
    @Builder.Default
    List<MatchedLineSegment> matchedLineSegments = new ArrayList<>();
    @Builder.Default
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);

    LinkFlagEncoder flagEncoder;

    @Builder.Default
    GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

    @Value
    @Builder
    public static class MatchedLineSegment {

        int matchedLinkId;
        LineString subGeometry;
        Point snappedPoint;
        Double distanceToSnappedPoint;
        Double fractionOfSnappedPoint;


    }

    @SneakyThrows
    public QueryResultWithBearing calculateMatchedBearings() {
        final Coordinate[] coordinates = cutoffGeometry.getCoordinates();
        List<LineString> subGeometries = new ArrayList<>();
        List<Coordinate> partialGeometry = new ArrayList<>();
        boolean lastBoundaryDetected;
        var it = Arrays.asList(coordinates).iterator();
        Coordinate currentCoordinate = it.next();
        while (it.hasNext()) {
            final Coordinate nextCoordinate = it.next();
            double convertedBearing = calculateBearing(currentCoordinate, nextCoordinate);
            if (bearingIsInRange(convertedBearing)) {
                partialGeometry.add(currentCoordinate);
                partialGeometry.add(nextCoordinate);
                lastBoundaryDetected = false;
            } else {
                lastBoundaryDetected = true;
                subGeometries.add(geometryFactory
                        .createLineString(partialGeometry.toArray(Coordinate[]::new)));
                partialGeometry = new ArrayList<>();
            }
            if (!it.hasNext() && !lastBoundaryDetected) {
                subGeometries.add(geometryFactory
                        .createLineString(partialGeometry.toArray(Coordinate[]::new)));
            }

            currentCoordinate = nextCoordinate;
            log.info("Segment [{} -> {}]. bearing is: {}", currentCoordinate, nextCoordinate, convertedBearing);
        }

        subGeometries.forEach(l -> {
                    final Point snappedPoint = calculateSnappedPoint(l);
                    final double fraction = calculateFraction(snappedPoint);
                    final IntsRef flags = queryResult.getClosestEdge().getFlags();
                    final int matchedLinkId = flagEncoder.getId(flags);
                    final double distanceToSnappedPoint = calculateDistance(inputPoint.getCoordinate(),
                            snappedPoint.getCoordinate());

                    final MatchedLineSegment lineSegmentBearing = MatchedLineSegment
                            .builder()
                            .matchedLinkId(matchedLinkId)
                            .fractionOfSnappedPoint(fraction)
                            .distanceToSnappedPoint(distanceToSnappedPoint)
                            .snappedPoint(snappedPoint)
                            .build();

                    matchedLineSegments.add(lineSegmentBearing);
                }
        );

        return this;
    }

    private double calculateFraction(Point snappedPoint) {
        final LineString originalGeometry = queryResult
                .getClosestEdge()
                .fetchWayGeometry(3)
                .toLineString(false);
        return getFraction(originalGeometry, snappedPoint.getCoordinate());
    }

    private Point calculateSnappedPoint(LineString subGeometry) {
        final LocationIndexedLine lineIndex = new LocationIndexedLine(subGeometry);
        final LinearLocation snappedPointLinearLocation = lineIndex.project(inputPoint.getCoordinate());
        return geometryFactory
                .createPoint(lineIndex.extractPoint(snappedPointLinearLocation));
    }

    boolean bearingIsInRange(double convertedBearing) {
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

    private double getFraction(LineString line, Coordinate coordinate) {
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

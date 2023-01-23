package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import java.util.ArrayList;
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
import org.locationtech.jts.linearref.LengthLocationMap;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

@Value
@Builder(toBuilder = true)
@Slf4j
public class QueryResultWithBearing {

    Point inputPoint;
    List<Double> inputBearingRange;
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
    DistanceCalc distanceCalculator = new DistanceCalcEarth();
    @Builder.Default
    GeodeticCalculator calculator = new GeodeticCalculator();

    @Value
    @Builder
    public static class MatchedLineSegment {

        int matchedLinkId;
        Coordinate startCoordinate;
        Coordinate endCoordinate;
        //LineString subGeometry;
        Double bearing;
        Point snappedPoint;
        Double distanceToSnappedPoint;
        Double fractionOfSnappedPoint;


    }

    @SneakyThrows
    public QueryResultWithBearing calculateMatchedBearings() {
        Coordinate[] coordinates = cutoffGeometry.getCoordinates();
        for (int c = 0; c < coordinates.length - 1; c++) {
            Coordinate currentCoordinate = coordinates[c];
            Coordinate nextCoordinate = coordinates[c + 1];
            double convertedBearing = calculateBearing(currentCoordinate, nextCoordinate);
            if (bearingIsInRange(convertedBearing)) {
                final Point snappedPoint = calculateSnappedPoint(currentCoordinate, nextCoordinate);
                final double fraction = calculateFraction(snappedPoint);
                final IntsRef flags = queryResult.getClosestEdge().getFlags();
                final int matchedLinkId = flagEncoder.getId(flags);
                final MatchedLineSegment lineSegmentBearing = MatchedLineSegment
                        .builder()
                        .matchedLinkId(matchedLinkId)
                        .startCoordinate(currentCoordinate)
                        .endCoordinate(nextCoordinate)
                        .fractionOfSnappedPoint(fraction)
                        .distanceToSnappedPoint(distanceCalculator.calcDist(inputPoint.getX(),
                                inputPoint.getY(), snappedPoint.getX(), snappedPoint.getY()))

                        .snappedPoint(snappedPoint)
                        .bearing(convertedBearing)
                        .build();
                matchedLineSegments.add(lineSegmentBearing);
            }

            log.info("Segment [{} -> {}]. bearing is: {}", c, c + 1, convertedBearing);
        }

        return this;
    }

    private double calculateFraction(Point snappedPoint) {
        final LineString originalGeometry = queryResult
                .getClosestEdge()
                .fetchWayGeometry(3)
                .toLineString(false);
        final double pointDistance = getLengthAlongLineString(originalGeometry, snappedPoint.getCoordinate());
        final double totalLength = originalGeometry.getLength();
        final double fraction = (totalLength - pointDistance) / totalLength;
        return fraction;
    }

    private Point calculateSnappedPoint(Coordinate currentCoordinate, Coordinate nextCoordinate) {
        final Coordinate[] subGeometryCoordinates = {currentCoordinate, nextCoordinate};
        final LineString subGeometry = geometryFactory.createLineString(subGeometryCoordinates);
        final LocationIndexedLine lineIndex = new LocationIndexedLine(subGeometry);
        final LinearLocation snappedPointLinearLocation = lineIndex.project(inputPoint.getCoordinate());
        final Point snappedPoint = geometryFactory
                .createPoint(lineIndex.extractPoint(snappedPointLinearLocation));
        return snappedPoint;
    }

    private boolean bearingIsInRange(double convertedBearing) {
        return convertedBearing >= inputBearingRange.get(0) && convertedBearing <= inputBearingRange.get(1);
    }

    private double calculateBearing(Coordinate currentCoordinate, Coordinate nextCoordinate) {
        calculator.setStartingGeographicPoint(currentCoordinate.getX(),
                currentCoordinate.getY());
        calculator.setDestinationGeographicPoint(nextCoordinate.getX(),
                nextCoordinate.getY());
        double bearing = calculator.getAzimuth();
        double convertedBearing = bearing < 0.0 ? bearing + 360 : bearing;
        return convertedBearing;
    }

    /*
     * https://gis.stackexchange.com/questions/231750/geotools-calculate-length-along-line-from-start-vertex-up-to-some-point-on-the
     * */
    public static double getLengthAlongLineString(LineString line, Coordinate coordinate) {
        LocationIndexedLine locationIndexedLine = new LocationIndexedLine(line);
        LinearLocation location = locationIndexedLine.project(coordinate);
        return new LengthLocationMap(line).getLength(location);
    }

}

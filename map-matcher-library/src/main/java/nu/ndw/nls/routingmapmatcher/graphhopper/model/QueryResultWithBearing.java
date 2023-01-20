package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import com.graphhopper.storage.index.QueryResult;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

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
    List<LineSegmentBearing> matchedLineSegmentBearings = new ArrayList<>();

    @Value
    @Builder
    public static class LineSegmentBearing {
        Coordinate startCoordinate;
        Coordinate endCoordinate;
        LineString subGeometry;
        Double bearing;
        Point snappedPoint;

    }

    @SneakyThrows
    public QueryResultWithBearing calculateBearings() {
        queryResult.getClosestEdge().fetchWayGeometry(3).toLineString(false);
        GeodeticCalculator calculator = new GeodeticCalculator(CRS.decode("EPSG:4326"));
        Coordinate[] coordinates = queryResult.getClosestEdge().fetchWayGeometry(3).toLineString(false).getCoordinates();
        for (int c = 0; c < coordinates.length - 1; c++) {
            Coordinate currentCoordinate = coordinates[c];
            Coordinate nextCoordinate = coordinates[c + 1];
            calculator.setStartingGeographicPoint(currentCoordinate.getX(),
                    currentCoordinate.getY());
            calculator.setDestinationGeographicPoint(nextCoordinate.getX(),
                    nextCoordinate.getY());
            double convertedBearing = calculator.getAzimuth();
            //if(bearing>=inputBearingRange.get(0) && bearing<=inputBearingRange.get(1)) {
            final LineSegmentBearing lineSegmentBearing = LineSegmentBearing
                    .builder()
                    .startCoordinate(currentCoordinate)
                    .endCoordinate(nextCoordinate)
                    .bearing(convertedBearing)
                    .build();
            // }
            matchedLineSegmentBearings.add(lineSegmentBearing);
            log.info("Segment [{}{}]. bearing is: {}", c, c + 1, convertedBearing);
        }

        return this;
    }

}

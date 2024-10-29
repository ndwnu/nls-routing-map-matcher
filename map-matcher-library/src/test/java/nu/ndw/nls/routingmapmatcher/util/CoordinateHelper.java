package nu.ndw.nls.routingmapmatcher.util;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;


public class CoordinateHelper {

    private static final String COORDINATE_SEPARATOR = ";";
    private static final String DIMENSION_SEPARATOR = ",";
    private static final int EXPECTED_DIMENSIONS = 2;
    private static final int NUM_COORDINATES_LINE = 2;

    private CoordinateHelper() {
        // Utility class.
    }

    public static List<Coordinate> getCoordinatesFromString(String coordinates) {
        return getCoordinatesFromPoints(getPointsFromString(coordinates));
    }

    private static List<List<Double>> getPointsFromString(String coordinates) {
        List<List<Double>> points = new ArrayList<>();
        for (String coordinate : coordinates.split(COORDINATE_SEPARATOR)) {
            String[] dimensions = coordinate.split(DIMENSION_SEPARATOR);
            if (dimensions.length != EXPECTED_DIMENSIONS) {
                throw new IllegalArgumentException("Cannot parse point '" + coordinate + "'");
            }
            try {
                double lon = Double.parseDouble(dimensions[0]);
                double lat = Double.parseDouble(dimensions[1]);
                points.add(List.of(lon, lat));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot parse point '" + coordinate + "'");
            }
        }
        return points;
    }

    private static Coordinate getCoordinateFromPoint(List<Double> coordinates) {
        // Latitude is the Y axis, Longitude is the X axis
        return new Coordinate(coordinates.getFirst(), coordinates.get(1));
    }

    private static List<Coordinate> getCoordinatesFromPoints(List<List<Double>> points) {
        if (points.size() < NUM_COORDINATES_LINE) {
            throw new IllegalArgumentException("Expecting at least two coordinates");
        }

        return points.stream().map(CoordinateHelper::getCoordinateFromPoint).toList();
    }
}
